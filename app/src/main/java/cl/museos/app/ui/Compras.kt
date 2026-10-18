package cl.museos.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.museos.app.domain.Reglas
import cl.museos.app.model.*
import cl.museos.app.viewmodel.MuseoViewModel

@Composable fun Compras(s: EstadoApp, vm: MuseoViewModel) {
    val u = s.usuario ?: return
    var confirmar by remember { mutableStateOf<Pedido?>(null) }
    confirmar?.let { pedido -> AlertDialog(onDismissRequest = { confirmar = null }, title = { Text("Actualizar solicitud #${pedido.id}") },
        text = { Text("Se avanzará al siguiente estado y el cliente recibirá un aviso local.") },
        confirmButton = { TextButton({ vm.avanzar(pedido); confirmar = null }) { Text("Confirmar cambio") } },
        dismissButton = { TextButton({ confirmar = null }) { Text("Cancelar") } }) }
    Pagina {
        when (s.pantalla) {
            Pantalla.CARRITO, Pantalla.PAGO -> {
                if (u.rol != "Cliente") return@Pagina
                Titulo(if (s.pantalla == Pantalla.PAGO) "Pago simulado" else "Tu carrito")
                if (s.carrito.isEmpty()) {
                    Vacio("Todavía no agregas productos.")
                    Accion("Explorar catálogo") { vm.ir(Pantalla.CATALOGO, true) }; return@Pagina
                }
                s.carrito.forEach { fila ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(fila.producto.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("${dinero(fila.producto.precio)} × ${fila.cantidad} = ${dinero(fila.subtotal)}")
                            if (!fila.producto.activo || fila.cantidad > fila.producto.stock) Text("Revisa la disponibilidad antes de pagar", color = MaterialTheme.colorScheme.error)
                            if (s.pantalla == Pantalla.CARRITO) Row {
                                IconButton({ vm.cantidad(fila.producto.id, fila.cantidad - 1) }, enabled = !s.ocupado) { Icon(Icons.Default.Remove, "Quitar una unidad") }
                                Text("${fila.cantidad}", Modifier.padding(top = 12.dp))
                                IconButton({ vm.cantidad(fila.producto.id, fila.cantidad + 1) }, enabled = !s.ocupado && fila.producto.activo && fila.cantidad < minOf(10, fila.producto.stock)) { Icon(Icons.Default.Add, "Agregar una unidad") }
                                Spacer(Modifier.weight(1f))
                                IconButton({ vm.cantidad(fila.producto.id, 0) }, enabled = !s.ocupado) { Icon(Icons.Default.DeleteOutline, "Eliminar producto del carrito") }
                            }
                        }
                    }
                }
                if (s.pantalla == Pantalla.CARRITO) {
                    Seleccion("Destino", s.destino, Reglas.destinos, vm::destino)
                    Seleccion("Modalidad de envío", s.modalidad, Reglas.modalidades, vm::modalidad)
                } else Text("${s.destino} · ${s.modalidad}")
                val peso = s.carrito.sumOf { it.cantidad * it.producto.peso }
                val subtotal = s.carrito.sumOf { it.subtotal }
                val envio = Reglas.envio(s.destino, s.modalidad, peso)
                Text("Peso total: ${"%.2f".format(peso)} kg\nSubtotal: ${dinero(subtotal)}\nDespacho: ${dinero(envio)}")
                Text("Total: ${dinero(subtotal + envio)} CLP", style = MaterialTheme.typography.titleLarge)
                Text("Tarifas académicas de demostración; no corresponden a un courier real.", style = MaterialTheme.typography.bodySmall)
                val disponible = s.carrito.all { it.producto.activo && it.cantidad <= it.producto.stock }
                if (s.pantalla == Pantalla.CARRITO) Accion("Continuar al pago", disponible && !s.ocupado) { vm.ir(Pantalla.PAGO) }
                else {
                    Text("Elige el resultado de la simulación. No se solicitarán datos bancarios ni se hará un cobro real.")
                    Accion("Simular pago aprobado", disponible && !s.ocupado, Icons.Default.CheckCircle) { vm.pagar(true) }
                    OutlinedButton({ vm.pagar(false) }, Modifier.fillMaxWidth(), enabled = !s.ocupado) { Text("Simular pago rechazado") }
                }
            }
            Pantalla.COMPROBANTE -> {
                val p = s.pedidos.find { it.id == s.pedidoActual }
                if (p == null) { Vacio("No se encontró el comprobante."); return@Pagina }
                Icon(Icons.Default.TaskAlt, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                Titulo("Comprobante #${p.id}", "${p.tipo} · ${p.estado}")
                Text("${p.nombreCliente}\n${p.fecha}")
                s.lineas.filter { it.pedidoId == p.id }.forEach { Text("${it.cantidad} × ${it.nombre} — ${dinero(it.precio * it.cantidad)}") }
                HorizontalDivider()
                Text("${p.destino} · ${p.modalidad}\nSubtotal: ${dinero(p.subtotal)}\nEnvío: ${dinero(p.envio)}")
                Text("Total: ${dinero(p.total)} CLP", style = MaterialTheme.typography.headlineSmall)
                Text(if (p.tipo == "Compra") "Pago de prueba aprobado. Este comprobante es una simulación académica." else "Consulta recibida. No se ha reservado un cupo ni realizado un cobro.")
                Accion("Ver historial") { vm.ir(Pantalla.HISTORIAL, true) }
            }
            else -> {
                if (u.rol == "Investigador") return@Pagina
                var filtro by rememberSaveable { mutableStateOf("Todos") }
                Titulo(if (u.rol == "Administrador") "Gestionar solicitudes" else "Mis solicitudes")
                Seleccion("Estado", filtro, listOf("Todos") + Reglas.estados) { filtro = it }
                val lista = s.pedidos.filter { filtro == "Todos" || it.estado == filtro }
                if (lista.isEmpty()) Vacio("No hay solicitudes en este estado.")
                lista.forEach { p ->
                    Tarjeta("#${p.id} · ${p.tipo}", "${p.estado} · ${dinero(p.total)} CLP\n${p.nombreCliente} · ${p.fecha}", Icons.Default.ReceiptLong) { vm.comprobante(p.id) }
                    if (u.rol == "Administrador" && p.estado != "Entregado") OutlinedButton({ confirmar = p }, enabled = !s.ocupado) { Text("Avanzar a ${Reglas.estados[Reglas.estados.indexOf(p.estado) + 1]}") }
                }
            }
        }
    }
}
