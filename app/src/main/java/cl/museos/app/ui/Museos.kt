package cl.museos.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.museos.app.model.*
import cl.museos.app.native.MapaMuseos
import cl.museos.app.native.SelectorImagen
import cl.museos.app.viewmodel.MuseoViewModel
import coil.compose.AsyncImage

@Composable fun Museos(s: EstadoApp, vm: MuseoViewModel) {
    val u = s.usuario ?: return
    val context = LocalContext.current
    Pagina {
        when (s.pantalla) {
            Pantalla.EDITAR_MUSEO -> {
                if (u.rol == "Cliente") return@Pagina
                Titulo(if (s.seleccion == 0L) "Nueva ficha de museo" else "Editar ficha", if (u.rol == "Investigador") "Tu ficha se guardará como borrador para revisión del administrador." else "Los cambios se guardarán publicados.")
                Campo(s, vm, "nombre", "Nombre del museo")
                Campo(s, vm, "pais", "País", Icons.Default.Public)
                Campo(s, vm, "ciudad", "Ciudad", Icons.Default.LocationCity)
                Campo(s, vm, "categoria", "Clasificación")
                Campo(s, vm, "descripcion", "Descripción", multilinea = true)
                Campo(s, vm, "latitud", "Latitud", Icons.Default.Place)
                Campo(s, vm, "longitud", "Longitud", Icons.Default.Place)
                Campo(s, vm, "enlace", "Enlace HTTPS al recorrido (opcional)", Icons.Default.Link)
                Text("Foto opcional de la ficha")
                SelectorImagen(s.formulario["foto"].orEmpty(), { vm.campo("foto", it) }, vm::mensaje)
                Accion("Guardar museo", !s.ocupado, onClick = vm::enviarFormulario)
            }
            Pantalla.MUSEO, Pantalla.VISITA -> {
                val m = s.museos.find { it.id == s.seleccion }
                if (m == null) { Vacio("Museo no disponible."); return@Pagina }
                if (s.pantalla == Pantalla.VISITA) {
                    var sala by rememberSaveable(m.id) { mutableIntStateOf(0) }
                    val salas = listOf("El edificio", "La colección", "Conservación y memoria")
                    val textos = listOf("Observa las columnas y el espacio de encuentro. La arquitectura también forma parte de la historia del museo.",
                        "Las colecciones permiten interpretar la vida de una comunidad. Cada objeto necesita una ficha que explique su origen y contexto.",
                        "Conservar significa documentar, cuidar y compartir el patrimonio con las próximas generaciones.")
                    Titulo("Recorrido: ${m.nombre}", "Visita ilustrada de demostración · ${sala + 1} de 3")
                    IlustracionMuseo(sala)
                    Text(salas[sala], style = MaterialTheme.typography.titleLarge); Text(textos[sala])
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ sala-- }, enabled = sala > 0, modifier = Modifier.weight(1f)) { Text("Anterior") }
                        Button({ sala++ }, enabled = sala < 2, modifier = Modifier.weight(1f)) { Text("Siguiente") }
                    }
                    Text("Recorrido educativo local. Las ilustraciones y fichas son ficticias; no es un tour fotográfico de un museo real.", style = MaterialTheme.typography.bodySmall)
                    return@Pagina
                }
                if (m.foto.isBlank()) IlustracionMuseo(m.id.toInt()) else AsyncImage(m.foto, "Foto de ${m.nombre}", Modifier.fillMaxWidth().height(200.dp))
                Text("${m.pais.uppercase()} · ${m.ciudad}", style = MaterialTheme.typography.labelLarge)
                Titulo(m.nombre, m.descripcion)
                Text("${m.categoria} · ${if (m.publicado) "Publicado" else "Borrador"}\nCoordenadas: ${m.latitud}, ${m.longitud}\nRegistro: ${m.fecha}")
                Accion("Iniciar visita ilustrada", icono = Icons.Default.ViewInAr) { vm.ir(Pantalla.VISITA) }
                if (m.enlace.isNotBlank()) OutlinedButton({
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.enlace))) }
                        .onFailure { vm.mensaje("No hay un navegador disponible para abrir el recorrido") }
                }, Modifier.fillMaxWidth()) { Text("Abrir recorrido web del museo") }
                if (u.rol == "Cliente") OutlinedButton({ vm.favorito(m.id) }, Modifier.fillMaxWidth(), enabled = !s.ocupado) {
                    Icon(if (m.id in s.favoritos) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.width(8.dp)); Text(if (m.id in s.favoritos) "Quitar de favoritos" else "Guardar favorito")
                }
                if (u.rol == "Administrador" || u.rol == "Investigador" && u.id == m.autorId) Accion("Editar ficha", !s.ocupado, Icons.Default.Edit) { vm.editarMuseo(m) }
                if (u.rol == "Administrador") OutlinedButton({ vm.publicar(m) }, enabled = !s.ocupado) { Text(if (m.publicado) "Retirar publicación" else "Publicar ficha") }
            }
            else -> {
                var pais by rememberSaveable { mutableStateOf("Todos") }
                var buscar by rememberSaveable { mutableStateOf("") }
                val favoritos = s.pantalla == Pantalla.FAVORITOS
                Titulo(if (favoritos) "Mis museos favoritos" else "Museos en el mapa", "Fichas académicas de demostración. Selecciona un museo para conocerlo.")
                if (favoritos && u.rol != "Cliente") return@Pagina
                OutlinedTextField(buscar, { buscar = it }, Modifier.fillMaxWidth(), label = { Text("Buscar museo o clasificación") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) })
                Seleccion("País", pais, listOf("Todos") + s.museos.map { it.pais }.distinct().sorted()) { pais = it }
                val lista = s.museos.filter { (!favoritos || it.id in s.favoritos) && (pais == "Todos" || it.pais == pais) &&
                    (it.nombre.contains(buscar, true) || it.categoria.contains(buscar, true)) }
                if (!favoritos) {
                    MapaMuseos(lista) { vm.abrir(Pantalla.MUSEO, it) }
                    Text("© OpenStreetMap contributors · El mapa necesita Internet. Las fichas se conservan localmente.", style = MaterialTheme.typography.bodySmall)
                    if (u.rol != "Cliente") Accion("Nueva ficha de museo", !s.ocupado, Icons.Default.Add) { vm.editarMuseo(null) }
                }
                if (lista.isEmpty()) Vacio(if (favoritos) "No hay favoritos que coincidan. Guarda museos desde su ficha." else "No hay museos que coincidan con la búsqueda.")
                lista.forEach { m -> Tarjeta(m.nombre, "${m.ciudad}, ${m.pais} · ${m.categoria}" + if (!m.publicado) "\nBorrador" else "") { vm.abrir(Pantalla.MUSEO, m.id) } }
            }
        }
    }
}
