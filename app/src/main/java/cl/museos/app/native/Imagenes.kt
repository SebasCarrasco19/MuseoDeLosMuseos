package cl.museos.app.native

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

@Composable fun ImagenPerfil(valor: String) {
    if (valor.startsWith("content:")) AsyncImage(valor, "Imagen elegida", Modifier.fillMaxWidth().height(150.dp))
    else Icon(when (valor) { "2" -> Icons.Default.AccountBalance; "3" -> Icons.Default.School; "4" -> Icons.Default.Palette; else -> Icons.Default.Person },
        "Avatar", Modifier.size(76.dp), tint = MaterialTheme.colorScheme.primary)
}

/** Igual que el ejemplo: cámara del sistema + FileProvider; selector de documentos para galería. */
@Composable fun SelectorImagen(valor: String, cambiar: (String) -> Unit, mensaje: (String?) -> Unit, avatar: Boolean = false) {
    val context = LocalContext.current
    var pendiente by rememberSaveable { mutableStateOf("") }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendiente.isNotBlank()) cambiar(pendiente)
        else { if (pendiente.isNotBlank()) runCatching { context.contentResolver.delete(Uri.parse(pendiente), null, null) }; mensaje("Captura cancelada") }
        pendiente = ""
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onSuccess { cambiar(uri.toString()) }.onFailure { mensaje("No se pudo conservar el acceso a esa imagen") }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (valor.isNotBlank()) ImagenPerfil(valor)
        if (avatar) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { n -> FilterChip(valor == n.toString(), { cambiar(n.toString()) }, label = { Text("$n") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({
                runCatching {
                    val dir = File(context.filesDir, "fotos").apply { mkdirs() }
                    val archivo = File.createTempFile("museo_", ".jpg", dir)
                    pendiente = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo).toString()
                    camara.launch(Uri.parse(pendiente))
                }.onFailure { mensaje("No hay una cámara disponible para capturar la foto") }
            }, Modifier.weight(1f)) { Text("Cámara") }
            OutlinedButton({ runCatching { galeria.launch(arrayOf("image/*")) }.onFailure { mensaje("No hay un selector de imágenes disponible") } }, Modifier.weight(1f)) { Text("Galería") }
        }
    }
}
