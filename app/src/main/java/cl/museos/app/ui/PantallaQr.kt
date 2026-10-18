package cl.museos.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cl.museos.app.model.*
import cl.museos.app.viewmodel.MuseoViewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable fun PantallaQr(s: EstadoApp, vm: MuseoViewModel) {
    if (s.usuario?.rol !in listOf("Investigador", "Cliente")) return
    val context = LocalContext.current
    var escaneando by remember { mutableStateOf(false) }
    Pagina {
        if (s.pantalla == Pantalla.RESULTADO_QR) {
            val qr = s.qr
            if (qr == null) { Vacio("No hay una lectura válida."); return@Pagina }
            Titulo("Código reconocido", "${qr.tipo.replaceFirstChar { it.uppercase() }} · ID ${qr.id}")
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(visible) {
                if (qr.tipo == "museo") IlustracionMuseo(qr.id.toInt())
                else IlustracionProducto(s.productos.find { it.id == qr.id }?.categoria.orEmpty())
            }
            Text(if (qr.tipo == "museo") s.museos.find { it.id == qr.id }?.nombre.orEmpty() else s.productos.find { it.id == qr.id }?.nombre.orEmpty(), style = MaterialTheme.typography.titleLarge)
            Text(if (qr.tipo == "museo") s.museos.find { it.id == qr.id }?.descripcion.orEmpty() else s.productos.find { it.id == qr.id }?.descripcion.orEmpty())
            Text("Lectura guardada en tu historial. Repetir el mismo código actualiza la fecha sin duplicarlo.")
            Accion("Abrir ficha", onClick = vm::abrirQr)
            OutlinedButton({ vm.ir(Pantalla.QR, true) }) { Text("Volver al lector") }
        } else {
            Titulo("Descubre con QR", "Escanea un código del proyecto para abrir un museo o producto.")
            Accion("Escanear QR", !escaneando && !s.ocupado, Icons.Default.QrCodeScanner) {
                escaneando = true
                runCatching {
                    GmsBarcodeScanning.getClient(context).startScan()
                        .addOnSuccessListener { codigo -> escaneando = false; vm.qr(codigo.rawValue.orEmpty()) }
                        .addOnCanceledListener { escaneando = false; vm.mensaje("Lectura cancelada") }
                        .addOnFailureListener { escaneando = false; vm.mensaje("No se pudo abrir el lector. Comprueba Google Play Services e Internet para su primera descarga.") }
                }.onFailure { escaneando = false; vm.mensaje("El lector no está disponible en este dispositivo") }
            }
            Text("El lector utiliza Google Play Services, como el ejemplo de clase.", style = MaterialTheme.typography.bodySmall)
            var manual by rememberSaveable { mutableStateOf(false) }
            TextButton({ manual = !manual }) { Text("Prueba alternativa con texto JSON") }
            if (manual) {
                var texto by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(texto, { texto = it }, Modifier.fillMaxWidth(), label = { Text("Contenido del código QR") }, placeholder = { Text("{\"tipo\":\"museo\",\"id\":1}") })
                Accion("Validar contenido", texto.isNotBlank() && !s.ocupado) { vm.qr(texto) }
                Text("Esta alternativa comprueba el contenido, pero no sustituye la prueba de cámara.", style = MaterialTheme.typography.bodySmall)
            }
            Text("Mis lecturas", style = MaterialTheme.typography.titleLarge)
            if (s.lecturas.isEmpty()) Vacio("Aún no has leído códigos.")
            s.lecturas.forEach { l -> Tarjeta(l.titulo, "${l.tipo} · ${l.fecha}", Icons.Default.QrCode) { vm.qr("{\"tipo\":\"${l.tipo}\",\"id\":${l.referencia}}") } }
        }
    }
}
