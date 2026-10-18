package cl.museos.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import cl.museos.app.domain.Reglas
import cl.museos.app.model.*
import cl.museos.app.viewmodel.MuseoViewModel

@Composable fun Catalogo(s: EstadoApp, vm: MuseoViewModel) {
    val rol = s.usuario?.rol ?: return
    Pagina {
        when (s.pantalla) {
            Pantalla.EDITAR_PRODUCTO -> {
                if (rol != "Administrador") return@Pagina
                Titulo(if (s.seleccion == 0L) "Nuevo producto o curso" else "Editar catálogo")
                Campo(s, vm, "nombre", "Nombre")
                Campo(s, vm, "descripcion", "Descripción", multilinea = true)
                Seleccion("Categoría", s.formulario["categoria"].orEmpty(), Reglas.categorias) { vm.campo("categoria", it) }
                Campo(s, vm, "precio", "Precio en CLP", Icons.Default.Payments)
                Campo(s, vm, "stock", "Unidades o cupos disponibles", Icons.Default.Inventory)
                if (s.formulario["categoria"] != "Cursos") Campo(s, vm, "peso", "Peso unitario en kg", Icons.Default.Scale)
                Accion("Guardar producto", !s.ocupado, onClick = vm::enviarFormulario)
            }
            Pantalla.PRODUCTO -> {
                val p = s.productos.find { it.id == s.seleccion }
                if (p == null) { Vacio("Este producto ya no está disponible."); return@Pagina }
                IlustracionProducto(p.categoria)
                Text(p.categoria.uppercase(), style = MaterialTheme.typography.labelLarge)
                Titulo(p.nombre, p.descripcion)
                Text(dinero(p.precio) + " CLP", style = MaterialTheme.typography.headlineMedium)
                Text(if (p.stock == 0) "Sin disponibilidad" else "${p.stock} ${if (p.esCurso) "cupos" else "unidades"} disponibles")
                if (!p.esCurso) Text("Peso unitario: ${p.peso} kg. Envío calculado en el carrito.")
                if (rol == "Cliente") {
                    if (p.esCurso) {
                        Text("Solicita información para conocer la inscripción. Esta consulta no reserva cupos ni realiza un cobro.")
                        Accion("Solicitar información", !s.ocupado && p.stock > 0 && p.activo) { vm.curso(p.id) }
                    } else {
                        val cantidad = s.carrito.find { it.producto.id == p.id }?.cantidad ?: 0
                        Accion("Agregar al carrito", !s.ocupado && p.activo && cantidad < minOf(p.stock, 10), Icons.Default.AddShoppingCart) { vm.agregar(p) }
                        if (cantidad > 0) Text("En tu carrito: $cantidad")
                        OutlinedButton({ vm.ir(Pantalla.CARRITO) }, Modifier.fillMaxWidth()) { Text("Ver carrito") }
                    }
                }
                if (rol == "Administrador") {
                    Accion("Editar producto", !s.ocupado, Icons.Default.Edit) { vm.editarProducto(p) }
                    OutlinedButton({ vm.activar(p) }, Modifier.fillMaxWidth(), enabled = !s.ocupado) { Text(if (p.activo) "Desactivar producto" else "Activar producto") }
                }
            }
            else -> {
                if (rol == "Investigador") { Vacio("Explora las fichas desde el mapa o un código QR."); return@Pagina }
                var buscar by rememberSaveable { mutableStateOf("") }
                var categoria by rememberSaveable { mutableStateOf("Todas") }
                Titulo("Catálogo", "Objetos que cuentan historias y cursos para seguir aprendiendo.")
                OutlinedTextField(buscar, { buscar = it }, Modifier.fillMaxWidth(), label = { Text("Buscar producto o curso") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
                Seleccion("Categoría", categoria, listOf("Todas") + Reglas.categorias) { categoria = it }
                if (rol == "Administrador") Accion("Nuevo producto o curso", !s.ocupado, Icons.Default.Add) { vm.editarProducto(null) }
                val lista = s.productos.filter { (categoria == "Todas" || categoria == it.categoria) && (it.nombre.contains(buscar, true) || it.descripcion.contains(buscar, true)) }
                if (lista.isEmpty()) Vacio("No hay resultados para esta búsqueda.")
                lista.forEach { p -> Tarjeta(p.nombre, "${p.categoria} · ${dinero(p.precio)} CLP\n" +
                    if (!p.activo) "Inactivo" else if (p.stock == 0) "Sin disponibilidad" else "Disponibles: ${p.stock}",
                    if (p.esCurso) Icons.Default.School else Icons.Default.MenuBook) { vm.abrir(Pantalla.PRODUCTO, p.id) } }
            }
        }
    }
}
