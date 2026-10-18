package cl.museos.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.museos.app.model.EstadoApp
import cl.museos.app.viewmodel.MuseoViewModel
import java.text.NumberFormat
import java.util.Locale

fun dinero(valor: Int): String = "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL")).format(valor)

@Composable fun Pagina(contenido: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = contenido)
}
@Composable fun Titulo(texto: String, detalle: String = "") {
    Text(texto, style = MaterialTheme.typography.headlineMedium)
    if (detalle.isNotBlank()) Text(detalle, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable fun Accion(texto: String, enabled: Boolean = true, icono: ImageVector = Icons.Default.ArrowForward, onClick: () -> Unit) {
    val focus = LocalFocusManager.current
    Button({ focus.clearFocus(); onClick() }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) {
        Icon(icono, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(texto)
    }
}
@Composable fun Tarjeta(titulo: String, detalle: String, icono: ImageVector = Icons.Default.Museum, onClick: () -> Unit) {
    ElevatedCard(onClick, Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(5.dp)); Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
@Composable fun Campo(s: EstadoApp, vm: MuseoViewModel, clave: String, etiqueta: String,
                      icono: ImageVector = Icons.Default.Edit, secreto: Boolean = false, multilinea: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    OutlinedTextField(value = s.formulario[clave].orEmpty(), onValueChange = { vm.campo(clave, it) },
        modifier = Modifier.fillMaxWidth(), label = { Text(etiqueta) }, singleLine = !multilinea,
        minLines = if (multilinea) 3 else 1, enabled = !s.ocupado,
        keyboardOptions = KeyboardOptions(imeAction = if (multilinea) ImeAction.Default else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        leadingIcon = { Icon(icono, null) }, isError = clave in s.errores,
        supportingText = { s.errores[clave]?.let { Text(it) } },
        visualTransformation = if (secreto && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (secreto) { { IconButton({ visible = !visible }) {
            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar u ocultar contraseña")
        } } } else null)
}
@Composable fun Seleccion(etiqueta: String, valor: String, opciones: List<String>, cambiar: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton({ abierto = true }, Modifier.fillMaxWidth()) {
                Text(valor, Modifier.weight(1f)); Icon(Icons.Default.ExpandMore, null)
            }
            DropdownMenu(abierto, { abierto = false }) {
                opciones.forEach { opcion -> DropdownMenuItem(text = { Text(opcion) }, onClick = { cambiar(opcion); abierto = false }) }
            }
        }
    }
}
@Composable fun Vacio(texto: String) {
    Card(Modifier.fillMaxWidth()) { Text(texto, Modifier.padding(24.dp)) }
}

@Composable fun IlustracionProducto(categoria: String) {
    Surface(Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(when (categoria) { "Cursos" -> Icons.Default.School; "Materiales" -> Icons.Default.Inventory2; "Merch" -> Icons.Default.AutoStories; else -> Icons.Default.MenuBook },
                "Ilustración de $categoria", Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp)); Text(categoria, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Ilustración propia: no representa una fotografía de un museo real. */
@Composable fun IlustracionMuseo(variante: Int = 0, modifier: Modifier = Modifier) {
    val fondo = listOf(Color(0xFF143D48), Color(0xFF6A4453), Color(0xFF305F63))[Math.floorMod(variante, 3)]
    Surface(modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(22.dp), color = fondo) {
        Canvas(Modifier.fillMaxSize().padding(22.dp)) {
            val w = size.width; val h = size.height
            drawCircle(Color(0xFFE8BC70), h * .25f, Offset(w * .82f, h * .25f))
            val tejado = Path().apply { moveTo(w*.12f,h*.35f); lineTo(w*.5f,0f); lineTo(w*.88f,h*.35f); close() }
            drawPath(tejado, Color(0xFFF4E6CA))
            repeat(5) { i -> drawRect(Color(0xFFF4E6CA), Offset(w*(.2f+i*.14f), h*.43f), Size(w*.055f,h*.4f)) }
            drawRect(Color(0xFFDCA75B), Offset(w*.12f,h*.89f), Size(w*.76f,h*.08f))
        }
    }
}
