package cl.museos.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.museos.app.model.*
import cl.museos.app.viewmodel.MuseoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MuseoApp(vm: MuseoViewModel) {
    val s by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(s.mensaje) { s.mensaje?.let { snackbar.showSnackbar(it); vm.mensaje(null) } }
    val esquema = if (s.usuario?.oscuro == true) darkColorScheme(primary = Color(0xFFB8D9D6), secondary = Color(0xFFE8BC70))
    else lightColorScheme(primary = Color(0xFF194D52), secondary = Color(0xFF785515), background = Color(0xFFFAF8F1), surface = Color(0xFFFAF8F1))
    val densidad = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, densidad.fontScale * if (s.usuario?.textoGrande == true) 1.15f else 1f)) {
        MaterialTheme(colorScheme = esquema) {
            val principales = listOf(Pantalla.INICIO, Pantalla.MAPA, Pantalla.CATALOGO, Pantalla.AVISOS, Pantalla.PERFIL)
            BackHandler(enabled = s.pantalla != Pantalla.BIENVENIDA && s.pantalla != Pantalla.INICIO) { vm.volver() }
            Scaffold(modifier = Modifier.imePadding(), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
                TopAppBar(title = { Text("Museo de los Museos", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = { if (s.pantalla !in principales && s.pantalla != Pantalla.BIENVENIDA) IconButton(vm::volver) { Icon(Icons.Default.ArrowBack, "Volver") } },
                    actions = { if (s.usuario?.rol == "Cliente") IconButton({ vm.ir(Pantalla.CARRITO) }) {
                        BadgedBox(badge = { if (s.carrito.isNotEmpty()) Badge { Text(s.carrito.sumOf { it.cantidad }.toString()) } }) { Icon(Icons.Default.ShoppingBag, "Carrito") }
                    } })
            }, bottomBar = {
                if (s.usuario != null) NavigationBar {
                    val destinos = listOf(Triple(Pantalla.INICIO,"Inicio",Icons.Default.Home), Triple(Pantalla.MAPA,"Mapa",Icons.Default.Map)) +
                        (if (s.usuario?.rol != "Investigador") listOf(Triple(Pantalla.CATALOGO,"Catálogo",Icons.Default.Storefront)) else emptyList()) +
                        listOf(Triple(Pantalla.AVISOS,"Avisos",Icons.Default.Notifications), Triple(Pantalla.PERFIL,"Perfil",Icons.Default.Person))
                    destinos.forEach { (pantalla, titulo, icono) -> NavigationBarItem(selected = s.pantalla == pantalla,
                        onClick = { vm.ir(pantalla, true) }, icon = { Icon(icono, titulo) }, label = { Text(titulo) }) }
                }
            }) { margen ->
                Box(Modifier.fillMaxSize().padding(margen)) {
                    if (s.cargando) CircularProgressIndicator(Modifier.padding(32.dp))
                    else Crossfade(s.pantalla, label = "Cambio de pantalla") { pantalla ->
                        when (pantalla) {
                            Pantalla.BIENVENIDA, Pantalla.LOGIN, Pantalla.REGISTRO -> Acceso(s, vm)
                            Pantalla.INICIO -> Inicio(s, vm)
                            Pantalla.CATALOGO, Pantalla.PRODUCTO, Pantalla.EDITAR_PRODUCTO -> Catalogo(s, vm)
                            Pantalla.MAPA, Pantalla.MUSEO, Pantalla.FAVORITOS, Pantalla.EDITAR_MUSEO, Pantalla.VISITA -> Museos(s, vm)
                            Pantalla.CARRITO, Pantalla.PAGO, Pantalla.COMPROBANTE, Pantalla.HISTORIAL -> Compras(s, vm)
                            Pantalla.PERFIL, Pantalla.EDITAR_PERFIL, Pantalla.AVISOS -> Perfil(s, vm)
                            Pantalla.QR, Pantalla.RESULTADO_QR -> PantallaQr(s, vm)
                        }
                    }
                    if (s.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
