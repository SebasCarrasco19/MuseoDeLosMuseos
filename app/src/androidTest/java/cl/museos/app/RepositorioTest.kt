package cl.museos.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.museos.app.data.*
import cl.museos.app.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Base aislada en memoria: estas pruebas nunca eliminan los datos de la app. */
@RunWith(AndroidJUnit4::class)
class RepositorioTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: MuseoRepository
    @Before fun preparar() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = MuseoRepository(db); repo.iniciar()
    }
    @After fun cerrar() { db.close() }
    private suspend fun cliente(): Long { repo.login("cliente@example.com", "Museo123", "Cliente"); return repo.cargar().usuario!!.id }
    private suspend fun admin(): Long { repo.login("admin@example.com", "Museo123", "Administrador"); return repo.cargar().usuario!!.id }

    @Test fun iniciarDosVecesNoDuplicaNiBorraDatos() = runBlocking {
        val id = cliente(); repo.favorito(id, 1); repo.iniciar()
        assertEquals(3, db.museoDao().usuarios().size)
        assertEquals(listOf(1L), repo.cargar().favoritos)
    }
    @Test fun datosPersistenAlCerrarYReabrirBaseEnDisco() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        val nombre = "prueba-persistencia-${System.currentTimeMillis()}.db"
        var archivo = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
        try {
            var r = MuseoRepository(archivo); r.iniciar(); r.login("cliente@example.com", "Museo123", "Cliente")
            val id = r.cargar().usuario!!.id; r.favorito(id, 1); r.carrito(id, 1, 2)
            archivo.close()
            archivo = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
            r = MuseoRepository(archivo); r.iniciar()
            assertEquals(id, r.cargar().usuario!!.id)
            assertEquals(listOf(1L), r.cargar().favoritos)
            assertEquals(2, r.cargar().carrito.single().cantidad)
        } finally { archivo.close(); contexto.deleteDatabase(nombre) }
    }
    @Test fun clientesNoCompartenFavoritosCarritoNiLecturas() = runBlocking {
        val id = cliente(); repo.favorito(id, 1); repo.carrito(id, 1, 2); repo.leerQr(id, "{\"tipo\":\"museo\",\"id\":1}")
        repo.registro("Otro cliente", "otro@example.com", "Museo123", "1")
        val otro = repo.cargar()
        assertTrue(otro.favoritos.isEmpty()); assertTrue(otro.carrito.isEmpty()); assertTrue(otro.lecturas.isEmpty()); assertTrue(otro.pedidos.isEmpty())
        cliente(); assertEquals(2, repo.cargar().carrito.single().cantidad)
    }
    @Test fun compraGuardaLineasDescuentaStockYVaciaCarrito() = runBlocking {
        val id = cliente(); val stock = db.museoDao().producto(1)!!.stock
        repo.carrito(id, 1, 2)
        val pedido = repo.comprar(id, "Chile - Santiago", "Estándar", repo.cargar().carrito)
        val s = repo.cargar()
        assertTrue(s.carrito.isEmpty()); assertEquals(stock - 2, db.museoDao().producto(1)!!.stock)
        assertEquals(27900, s.pedidos.single().total); assertEquals(pedido, s.lineas.single().pedidoId)
        assertEquals(1, s.avisos.size)
    }
    @Test fun cambioDePrecioImpideCompraSinAlterarStockOPedidos() = runBlocking {
        val id = cliente(); repo.carrito(id, 1, 1); val esperado = repo.cargar().carrito
        val producto = db.museoDao().producto(1)!!
        db.museoDao().actualizarProducto(producto.copy(precio = producto.precio + 100))
        assertTrue(runCatching { repo.comprar(id, "Chile - Santiago", "Estándar", esperado) }.isFailure)
        assertTrue(repo.cargar().pedidos.isEmpty()); assertEquals(1, repo.cargar().carrito.size)
        assertEquals(producto.stock, db.museoDao().producto(1)!!.stock)
    }
    @Test fun unaCompraFallidaNoDescuentaElPrimerProducto() = runBlocking {
        val id = cliente(); repo.carrito(id, 1, 1); repo.carrito(id, 2, 1)
        val esperado = repo.cargar().carrito
        val p = db.museoDao().producto(2)!!; db.museoDao().actualizarProducto(p.copy(stock = 0))
        assertTrue(runCatching { repo.comprar(id, "Chile - Santiago", "Estándar", esperado) }.isFailure)
        assertEquals(esperado[0].producto.stock, db.museoDao().producto(1)!!.stock)
        assertTrue(repo.cargar().pedidos.isEmpty())
    }
    @Test fun rolesBloqueanOperacionesAjenas() = runBlocking {
        val id = cliente(); val producto = db.museoDao().producto(1)!!
        assertTrue(runCatching { repo.guardarProducto(id, producto) }.isFailure)
        assertTrue(runCatching { repo.publicar(id, 1) }.isFailure)
        val administrador = admin()
        assertTrue(runCatching { repo.carrito(administrador, 1, 1) }.isFailure)
        assertTrue(runCatching { repo.login("cliente@example.com", "Museo123", "Administrador") }.isFailure)
    }
    @Test fun qrRepetidoNoDuplicaYDesconocidoNoSeGuarda() = runBlocking {
        val id = cliente()
        repeat(2) { repo.leerQr(id, "{\"tipo\":\"producto\",\"id\":1}") }
        assertEquals(1, repo.cargar().lecturas.size)
        assertTrue(runCatching { repo.leerQr(id, "{\"tipo\":\"museo\",\"id\":9999}") }.isFailure)
        assertEquals(1, repo.cargar().lecturas.size)
    }
    @Test fun solicitudCursoNoCobraYNoSeDuplicaPendiente() = runBlocking {
        val id = cliente(); repo.solicitarCurso(id, 4)
        assertEquals(0, repo.cargar().pedidos.single().total)
        assertTrue(runCatching { repo.solicitarCurso(id, 4) }.isFailure)
        assertTrue(runCatching { repo.solicitarCurso(id, 5) }.isFailure)
    }
    @Test fun investigadorCreaBorradorYAdminLoPublica() = runBlocking {
        repo.login("investigador@example.com", "Museo123", "Investigador")
        val id = repo.cargar().usuario!!.id
        repo.guardarMuseo(id, Museo(nombre = "Prueba", pais = "Chile", ciudad = "Santiago", descripcion = "Museo de prueba", categoria = "Historia", latitud = -33.0, longitud = -70.0, autorId = id))
        val nuevo = repo.cargar().museos.last(); assertFalse(nuevo.publicado)
        cliente(); assertFalse(repo.cargar().museos.any { it.id == nuevo.id })
        val administrador = admin(); repo.publicar(administrador, nuevo.id)
        cliente(); assertTrue(repo.cargar().museos.any { it.id == nuevo.id })
    }
    @Test fun adminAvanzaEstadoYAvisoPerteneceAlComprador() = runBlocking {
        val id = cliente(); repo.carrito(id, 1, 1)
        val pedido = repo.comprar(id, "Chile - Santiago", "Estándar", repo.cargar().carrito)
        val administrador = admin()
        assertTrue(runCatching { repo.estado(administrador, pedido, "Entregado") }.isFailure)
        repo.estado(administrador, pedido, "Confirmado")
        cliente(); assertEquals("Confirmado", repo.cargar().pedidos.single().estado)
        assertEquals(2, repo.cargar().avisos.size)
    }
}
