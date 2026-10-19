package cl.museos.app

import android.app.Application
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import cl.museos.app.data.*
import cl.museos.app.ui.MuseoApp
import cl.museos.app.viewmodel.MuseoViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import java.io.File

class PantallasTest {
    @get:Rule val ui = createComposeRule()
    private lateinit var vm: MuseoViewModel
    @After fun evidenciaFinal() {
        android.util.Log.i("MuseoPrueba", "pantalla=${vm.estado.value.pantalla}, errores=${vm.estado.value.errores}, mensaje=${vm.estado.value.mensaje}")
        ui.onRoot().printToLog("MuseoArbol")
        foto("final-${System.currentTimeMillis()}")
    }
    @Before fun preparar() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        runBlocking { MuseoRepository(AppDatabase.crear(app)).salir() }
        vm = MuseoViewModel(app)
        ui.setContent { MuseoApp(vm) }
        esperar("Iniciar sesión")
    }
    private fun esperar(texto: String) {
        ui.waitUntil(15000) { ui.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
        ui.waitForIdle()
    }
    private fun pulsar(texto: String) {
        // Espera a que termine el aviso temporal antes de pulsar controles que podría cubrir.
        ui.waitUntil(10000) { vm.estado.value.mensaje == null }
        ui.onNodeWithText(texto).performScrollTo().performClick()
    }
    private fun foto(nombre: String) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dir = File(app.getExternalFilesDir(null), "evidencias").apply { mkdirs() }
        File(dir, "$nombre.png").outputStream().use { ui.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun login(rol: String = "Cliente") {
        pulsar("Iniciar sesión")
        if (rol != "Cliente") { pulsar("Cliente"); ui.onNodeWithText(rol).performClick() }
        pulsar("Usar cuenta de demostración"); pulsar("Entrar"); esperar("Hola, ${if (rol == "Cliente") "Visitante" else if (rol == "Administrador") "Administrador" else "Investigador"}")
    }
    @Test fun registroValidaYConservaCuentaAlCerrarSesion() {
        pulsar("Crear cuenta de cliente"); pulsar("Registrarme")
        ui.onNodeWithText("Usa un correo ficticio terminado en @example.com").assertExists()
        val correo = "prueba${System.currentTimeMillis()}@example.com"
        listOf("Nombre" to "Cliente de prueba", "Correo" to correo, "Contraseña" to "Museo123", "Repite la contraseña" to "Museo123").forEach { (campo, valor) ->
            ui.onNodeWithText(campo).performScrollTo().performTextReplacement(valor)
        }
        ui.onNodeWithText("Repite la contraseña").performImeAction()
        pulsar("Registrarme"); esperar("Hola, Cliente")
        ui.onNodeWithText("Perfil").performClick(); pulsar("Cerrar sesión"); esperar("Iniciar sesión")
        pulsar("Iniciar sesión")
        ui.onNodeWithText("Correo").performTextInput(correo)
        ui.onNodeWithText("Contraseña").performTextInput("Museo123")
        ui.onNodeWithText("Contraseña").performImeAction()
        pulsar("Entrar"); esperar("Hola, Cliente")
        ui.runOnIdle { check(vm.estado.value.favoritos.isEmpty() && vm.estado.value.pedidos.isEmpty()) }
        foto("10-registro")
    }
    @Test fun clienteCompletaCompraRechazoFavoritosQrYPerfil() {
        foto("01-bienvenida"); login(); foto("02-inicio")
        ui.onNodeWithText("Catálogo").performClick(); esperar("Arte colonial ilustrado")
        pulsar("Arte colonial ilustrado"); pulsar("Agregar al carrito")
        ui.waitUntil(10000) { !vm.estado.value.ocupado && vm.estado.value.carrito.isNotEmpty() }
        pulsar("Ver carrito"); esperar("Tu carrito"); foto("03-carrito")
        pulsar("Continuar al pago"); pulsar("Simular pago rechazado"); esperar("Tu carrito")
        ui.runOnIdle { check(vm.estado.value.carrito.isNotEmpty()) }
        pulsar("Continuar al pago"); pulsar("Simular pago aprobado")
        ui.waitUntil(10000) { vm.estado.value.pantalla.name == "COMPROBANTE" && !vm.estado.value.ocupado }
        foto("04-comprobante")
        ui.runOnIdle { check(vm.estado.value.carrito.isEmpty()); check(vm.estado.value.pedidos.isNotEmpty()) }
        ui.onNodeWithText("Mapa").performClick()
        ui.waitUntil(10000) { vm.estado.value.pantalla.name == "MAPA" }; foto("05-mapa")
        val museo = vm.estado.value.museos.first()
        pulsar(museo.nombre)
        val boton = if (museo.id in vm.estado.value.favoritos) "Quitar de favoritos" else "Guardar favorito"
        pulsar(boton); ui.waitUntil(10000) { !vm.estado.value.ocupado }
        pulsar("Iniciar visita ilustrada"); esperar("El edificio"); pulsar("Siguiente"); esperar("La colección")
        ui.onNodeWithText("Inicio").performClick(); pulsar("Leer código QR")
        pulsar("Prueba alternativa con texto JSON")
        ui.onNodeWithText("Contenido del código QR").performScrollTo().performTextInput("{\"tipo\":\"museo\",\"id\":1}")
        pulsar("Validar contenido"); esperar("Código reconocido"); foto("06-qr")
        ui.onNodeWithText("Perfil").performClick(); pulsar("Editar mis datos")
        ui.onNodeWithText("Nombre").performTextReplacement("Visitante demo")
        pulsar("Guardar perfil"); esperar("Mi perfil"); foto("07-perfil")
    }
    @Test fun investigadorGuardaFichaYAdminPublica() {
        login("Investigador")
        pulsar("Nueva ficha de museo")
        val nombre = "Museo prueba ${System.currentTimeMillis()}"
        listOf("Nombre del museo" to nombre, "País" to "Chile", "Ciudad" to "Santiago", "Clasificación" to "Historia", "Descripción" to "Ficha de demostración para la prueba de publicación.", "Latitud" to "-33.4", "Longitud" to "-70.6").forEach { (campo, valor) ->
            ui.onNodeWithText(campo).performScrollTo().performTextReplacement(valor)
        }
        ui.onNodeWithText("Longitud").performImeAction()
        pulsar("Guardar museo")
        ui.waitUntil(10000) { !vm.estado.value.ocupado && vm.estado.value.pantalla.name == "MAPA" }
        pulsar(nombre); ui.onNodeWithText("Editar ficha").assertExists(); foto("08-investigador")
        ui.onNodeWithText("Perfil").performClick(); pulsar("Cerrar sesión"); esperar("Iniciar sesión")
        login("Administrador"); ui.onNodeWithText("Mapa").performClick(); pulsar(nombre)
        pulsar("Publicar ficha"); ui.waitUntil(10000) { !vm.estado.value.ocupado && vm.estado.value.museos.any { it.nombre == nombre && it.publicado } }
        foto("09-administrador")
    }
}
