package cl.museos.app.data

import androidx.room.withTransaction
import cl.museos.app.domain.Claves
import cl.museos.app.domain.Reglas
import cl.museos.app.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MuseoRepository(private val db: AppDatabase) {
    private val dao = db.museoDao()
    private fun fecha() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    private suspend fun exigir(id: Long, vararg roles: String): Usuario {
        val usuario = dao.usuario(id) ?: error("Inicia sesión nuevamente")
        check(usuario.rol in roles) { "Tu perfil no tiene permiso para esta acción" }
        return usuario
    }
    suspend fun iniciar() {
        db.withTransaction {
            if (dao.usuarios().isEmpty()) {
                dao.insertarUsuario(Usuario(nombre = "Administrador demo", correo = "admin@example.com", clave = Claves.crear("Museo123"), rol = "Administrador", avatar = "2"))
                val investigador = dao.insertarUsuario(Usuario(nombre = "Investigador demo", correo = "investigador@example.com", clave = Claves.crear("Museo123"), rol = "Investigador", avatar = "3"))
                dao.insertarUsuario(Usuario(nombre = "Visitante demo", correo = "cliente@example.com", clave = Claves.crear("Museo123")))
                DatosIniciales.productos.forEach { dao.insertarProducto(it) }
                DatosIniciales.museos(investigador).forEach { dao.insertarMuseo(it) }
            }
        }
    }
    suspend fun cargar(): EstadoApp {
        val usuario = dao.usuario(dao.sesion() ?: 0)
        val id = usuario?.id ?: 0
        val productos = dao.productos()
        val museos = dao.museos().filter { it.publicado || usuario?.rol == "Administrador" || it.autorId == id }
        val pedidos = if (usuario?.rol == "Administrador") dao.todosPedidos() else dao.pedidos(id)
        return EstadoApp(cargando = false, usuario = usuario,
            productos = productos.filter { it.activo || usuario?.rol == "Administrador" }, museos = museos,
            favoritos = dao.favoritos(id), carrito = dao.carrito(id).mapNotNull { item ->
                productos.find { it.id == item.productoId }?.let { FilaCarrito(it, item.cantidad) }
            }, pedidos = pedidos, lineas = if (pedidos.isEmpty()) emptyList() else dao.lineas(pedidos.map { it.id }),
            avisos = dao.avisos(id), lecturas = dao.lecturas(id))
    }
    suspend fun login(correo: String, clave: String, rol: String) {
        val u = dao.correo(correo.trim().lowercase())
        check(u != null && u.rol == rol && Claves.coincide(clave, u.clave)) { "Correo, contraseña o rol incorrectos" }
        dao.guardarSesion(Sesion(usuarioId = u.id))
    }
    suspend fun registro(nombre: String, correo: String, clave: String, avatar: String) = db.withTransaction {
        check(dao.correo(correo.trim().lowercase()) == null) { "Ya existe una cuenta con ese correo" }
        val id = dao.insertarUsuario(Usuario(nombre = nombre.trim(), correo = correo.trim().lowercase(), clave = Claves.crear(clave), avatar = avatar))
        dao.guardarSesion(Sesion(usuarioId = id))
    }
    suspend fun salir() = dao.guardarSesion(Sesion())
    suspend fun perfil(id: Long, nombre: String, correo: String, avatar: String) {
        val u = exigir(id, *Reglas.roles.toTypedArray())
        val existente = dao.correo(correo.trim().lowercase())
        check(existente == null || existente.id == id) { "Ese correo pertenece a otra cuenta" }
        dao.actualizarUsuario(u.copy(nombre = nombre.trim(), correo = correo.trim().lowercase(), avatar = avatar))
    }
    suspend fun preferencias(id: Long, oscuro: Boolean, grande: Boolean, avisos: Boolean) {
        val u = exigir(id, *Reglas.roles.toTypedArray())
        dao.actualizarUsuario(u.copy(oscuro = oscuro, textoGrande = grande, avisos = avisos))
    }
    suspend fun favorito(id: Long, museo: Long) {
        exigir(id, "Cliente")
        check(dao.museo(museo)?.publicado == true) { "Museo no disponible" }
        if (museo in dao.favoritos(id)) dao.quitarFavorito(id, museo) else dao.favorito(Favorito(id, museo))
    }
    suspend fun carrito(id: Long, producto: Long, cantidad: Int) = db.withTransaction {
        exigir(id, "Cliente")
        if (cantidad == 0) { dao.quitarItem(id, producto); return@withTransaction }
        val p = dao.producto(producto) ?: error("Producto no encontrado")
        check(p.activo && !p.esCurso) { "Este producto no se puede agregar al carrito" }
        check(cantidad in 1..minOf(10, p.stock)) { "Cantidad no disponible (máximo 10 por producto)" }
        check(dao.carrito(id).size < 20 || dao.carrito(id).any { it.productoId == producto }) { "Máximo 20 productos diferentes" }
        dao.guardarItem(ItemCarrito(id, producto, cantidad))
    }
    suspend fun comprar(id: Long, destino: String, modalidad: String, esperado: List<FilaCarrito>): Long = db.withTransaction {
        val u = exigir(id, "Cliente")
        val items = dao.carrito(id)
        check(items.isNotEmpty()) { "El carrito está vacío" }
        check(items.size == esperado.size) { "Cambió el carrito. Revisa el total antes de pagar" }
        val filas = items.map { item ->
            val p = dao.producto(item.productoId) ?: error("Producto no disponible")
            check(p.activo && !p.esCurso && item.cantidad in 1..minOf(p.stock, 10)) { "Revisa la disponibilidad de ${p.nombre}" }
            check(esperado.any { it.producto.id == p.id && it.producto.precio == p.precio && it.producto.peso == p.peso && it.cantidad == item.cantidad }) { "El precio, peso o cantidad cambió. Revisa el carrito" }
            FilaCarrito(p, item.cantidad)
        }
        val envio = Reglas.envio(destino, modalidad, filas.sumOf { it.producto.peso * it.cantidad })
        val pedido = dao.insertarPedido(Pedido(usuarioId = id, nombreCliente = u.nombre, fecha = fecha(),
            subtotal = filas.sumOf { it.subtotal }, envio = envio, destino = destino, modalidad = modalidad))
        filas.forEach {
            dao.insertarLinea(LineaPedido(pedidoId = pedido, productoId = it.producto.id, nombre = it.producto.nombre, precio = it.producto.precio, cantidad = it.cantidad))
            dao.actualizarProducto(it.producto.copy(stock = it.producto.stock - it.cantidad))
        }
        dao.vaciarCarrito(id)
        dao.insertarAviso(Aviso(usuarioId = id, titulo = "Solicitud #$pedido recibida", texto = "Pago simulado aprobado. Puedes consultar tu comprobante.", fecha = fecha()))
        pedido
    }
    suspend fun solicitarCurso(id: Long, producto: Long): Long = db.withTransaction {
        val u = exigir(id, "Cliente")
        val p = dao.producto(producto) ?: error("Curso no encontrado")
        check(p.activo && p.esCurso && p.stock > 0) { "Curso sin disponibilidad" }
        check(dao.pedidos(id).none { pedido -> pedido.tipo == "Información de curso" && pedido.estado != "Entregado" && dao.lineas(listOf(pedido.id)).any { it.productoId == producto } }) { "Ya tienes una consulta pendiente de este curso" }
        val pedido = dao.insertarPedido(Pedido(usuarioId = id, nombreCliente = u.nombre, fecha = fecha(), subtotal = 0, envio = 0,
            destino = "En línea", modalidad = "Sin envío", tipo = "Información de curso"))
        dao.insertarLinea(LineaPedido(pedidoId = pedido, productoId = p.id, nombre = p.nombre, precio = 0, cantidad = 1))
        dao.insertarAviso(Aviso(usuarioId = id, titulo = "Consulta de curso recibida", texto = "${p.nombre}. Sin reserva de cupo ni cobro.", fecha = fecha()))
        pedido
    }
    suspend fun estado(id: Long, pedido: Long, nuevo: String) {
        exigir(id, "Administrador")
        val p = dao.pedido(pedido) ?: error("Solicitud inexistente")
        check(Reglas.estados.indexOf(nuevo) == Reglas.estados.indexOf(p.estado) + 1) { "Avanza al siguiente estado" }
        db.withTransaction {
            dao.actualizarPedido(p.copy(estado = nuevo))
            dao.insertarAviso(Aviso(usuarioId = p.usuarioId, titulo = "Solicitud #${p.id}: $nuevo", texto = "El administrador actualizó tu solicitud.", fecha = fecha()))
        }
    }
    suspend fun guardarProducto(id: Long, producto: Producto) {
        exigir(id, "Administrador")
        if (producto.id == 0L) dao.insertarProducto(producto) else {
            check(dao.producto(producto.id) != null); dao.actualizarProducto(producto)
        }
    }
    suspend fun activarProducto(id: Long, producto: Long) {
        exigir(id, "Administrador")
        val p = dao.producto(producto) ?: error("Producto inexistente")
        dao.actualizarProducto(p.copy(activo = !p.activo))
    }
    suspend fun guardarMuseo(id: Long, museo: Museo) {
        val u = exigir(id, "Administrador", "Investigador")
        val original = if (museo.id == 0L) null else dao.museo(museo.id) ?: error("Museo inexistente")
        check(original == null || u.rol == "Administrador" || original.autorId == id) { "Solo puedes editar tus propias fichas" }
        val limpio = museo.copy(autorId = original?.autorId ?: id, publicado = u.rol == "Administrador")
        if (limpio.id == 0L) dao.insertarMuseo(limpio) else dao.actualizarMuseo(limpio)
    }
    suspend fun publicar(id: Long, museo: Long) {
        exigir(id, "Administrador")
        val m = dao.museo(museo) ?: error("Museo inexistente")
        dao.actualizarMuseo(m.copy(publicado = !m.publicado))
    }
    suspend fun leerQr(id: Long, texto: String): CodigoQr {
        val u = exigir(id, "Investigador", "Cliente")
        val qr = Reglas.leerQr(texto) ?: error("QR inválido. Debe contener tipo e id en formato JSON")
        val titulo = if (qr.tipo == "producto") {
            val p = dao.producto(qr.id)
            check(p != null && p.activo) { "El QR corresponde a un producto desconocido o inactivo" }; p.nombre
        } else {
            val m = dao.museo(qr.id)
            check(m != null && (m.publicado || u.rol == "Investigador" && m.autorId == id)) { "Museo desconocido o no publicado" }; m.nombre
        }
        dao.guardarLectura(Lectura(id, qr.tipo, qr.id, titulo, fecha()))
        return qr
    }
    suspend fun leerAviso(id: Long, aviso: Long) = dao.leerAviso(aviso, id)
    suspend fun anunciar(id: Long, texto: String) {
        exigir(id, "Administrador")
        check(texto.trim().length in 5..200) { "Escribe un aviso de 5 a 200 caracteres" }
        dao.usuarios().forEach { u -> dao.insertarAviso(Aviso(usuarioId = u.id, titulo = "Novedad del museo", texto = texto.trim(), destino = "INICIO", fecha = fecha())) }
    }
}
