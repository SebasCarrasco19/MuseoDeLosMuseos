package cl.museos.app.ui

import androidx.compose.foundation.layout.*
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
            when (qr.tipo) {
                "museo" -> {
                    val museo = s.museos.find { it.id == qr.id }
                    if (museo == null) Vacio("No se encontraron los detalles del museo") else {
                        IlustracionMuseo(qr.id.toInt())
                        Text(museo.nombre, style = MaterialTheme.typography.titleLarge)
                        Text("Ubicación: ${museo.ciudad}, ${museo.pais}")
                        Text("Categoría: ${museo.categoria}")
                        Text(museo.descripcion)
                        Text("Coordenadas: ${museo.latitud}, ${museo.longitud}")
                        Text("Estado: ${if (museo.publicado) "Publicado" else "Borrador"}")
                        Accion("Ver ficha del museo", onClick = vm::abrirQr)
                    }
                }
                "producto" -> {
                    val producto = s.productos.find { it.id == qr.id }
                    if (producto == null) Vacio("Producto no disponible") else {
                        IlustracionProducto(producto.categoria)
                        Text(producto.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(producto.descripcion)
                        Text("Categoría: ${producto.categoria}")
                        Text("Precio: ${dinero(producto.precio)} CLP")
                        Accion("Ver ficha del producto", onClick = vm::abrirQr)
                    }
                }
                "compra" -> {
                    val pedido = s.pedidos.find { it.id == qr.id && it.usuarioId == s.usuario?.id }
                    if (pedido == null) Vacio("La compra no está disponible para este usuario") else {
                        Text("Comprobante #${pedido.id}", style = MaterialTheme.typography.titleLarge)
                        Text("Cliente: ${pedido.nombreCliente}")
                        Text("Fecha: ${pedido.fecha}")
                        Text("Estado: ${pedido.estado}")
                        Text("Tipo: ${pedido.tipo}")
                        HorizontalDivider()
                        s.lineas.filter { it.pedidoId == pedido.id }.forEach { linea ->
                            Text("${linea.cantidad} × ${linea.nombre} — ${dinero(linea.precio * linea.cantidad)}")
                        }
                        HorizontalDivider()
                        Text("Destino: ${pedido.destino} · ${pedido.modalidad}")
                        Text("Subtotal: ${dinero(pedido.subtotal)}")
                        Text("Envío: ${dinero(pedido.envio)}")
                        Text("Total: ${dinero(pedido.total)} CLP", style = MaterialTheme.typography.titleLarge)
                        Accion("Ver comprobante", onClick = vm::abrirQr)
                    }
                }
            }
            Text("Información consultada desde SQLite. El escaneo no redirige automáticamente.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton({ vm.ir(Pantalla.QR, true) }) { Text("Volver al lector") }
        } else {
            Titulo("Descubre con QR", "Escanea un QR de museo, producto o compra para consultar sus detalles.")
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
