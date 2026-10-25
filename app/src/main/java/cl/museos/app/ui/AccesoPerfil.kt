package cl.museos.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.museos.app.domain.Reglas
import cl.museos.app.model.*
import cl.museos.app.native.SelectorImagen
import cl.museos.app.viewmodel.MuseoViewModel


@Composable
fun Acceso(s: EstadoApp, vm: MuseoViewModel) {

    var visible by remember(s.pantalla) { mutableStateOf(false) }
    LaunchedEffect(s.pantalla) { visible = true }

    Pagina {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(600)
            ) + slideInVertically(
                initialOffsetY = { it / 5 },
                animationSpec = tween(600)
            )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (s.pantalla == Pantalla.BIENVENIDA) {
                    IlustracionMuseo()
                    Text(
                        "CULTURA · MEMORIA · CONEXIÓN",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Titulo(
                        "Un mundo de museos",
                        "Explora colecciones, descubre cursos y guarda tus próximos destinos."
                    )
                    Accion("Iniciar sesión") {
                        vm.ir(Pantalla.LOGIN)
                    }
                    OutlinedButton(
                        onClick = { vm.ir(Pantalla.REGISTRO) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear cuenta de cliente")
                    }
                    Text(
                        "Proyecto académico · Datos y compras de demostración",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val registro = s.pantalla == Pantalla.REGISTRO
                    Titulo(
                        if (registro) "Crea tu cuenta"
                        else "Bienvenido de nuevo",
                        if (registro) "Completa tus datos para registrarte."
                        else "Ingresa tu correo y contraseña."
                    )
                    if (registro) { Campo(s, vm, "nombre", "Nombre", Icons.Default.Person)
                        SelectorImagen(s.formulario["avatar"] ?: "1", { vm.campo("avatar", it) }, vm::mensaje, avatar = true)
                    }
                    Campo(s, vm, "correo", "Correo", Icons.Default.Email)
                    Campo(s, vm, "clave", "Contraseña", Icons.Default.Lock, secreto = true)
                    if (registro) {
                        Campo( s, vm, "repetir", "Repite la contraseña", Icons.Default.Lock, secreto = true
                        )
                    }
                    Accion( if (registro) "Registrarme" else "Entrar", !s.ocupado, onClick = vm::enviarFormulario
                    )
                }
            }
        }
    }
}

@Composable fun Inicio(s: EstadoApp, vm: MuseoViewModel) {
    val u = s.usuario ?: return
    Pagina {
        Text(u.rol.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Titulo("Hola, ${u.nombre.substringBefore(' ')}", "Tu punto de encuentro con el patrimonio.")
        IlustracionMuseo()
        Tarjeta("Explorar museos", "Mapa, países y fichas de las colecciones", Icons.Default.Map) { vm.ir(Pantalla.MAPA) }
        if (u.rol != "Investigador") Tarjeta(if (u.rol == "Administrador") "Gestionar catálogo" else "Productos y cursos", "Publicaciones, materiales y formación", Icons.Default.Storefront) { vm.ir(Pantalla.CATALOGO) }
        if (u.rol != "Administrador") Tarjeta("Leer código QR", "Descubre una ficha y conserva tus lecturas", Icons.Default.QrCodeScanner) { vm.ir(Pantalla.QR) }
        if (u.rol == "Cliente") Tarjeta("Mis favoritos", "${s.favoritos.size} museos guardados", Icons.Default.Favorite) { vm.ir(Pantalla.FAVORITOS) }
        if (u.rol != "Investigador") Tarjeta("Historial de solicitudes", "${s.pedidos.size} solicitudes registradas", Icons.Default.ReceiptLong) { vm.ir(Pantalla.HISTORIAL) }
        if (u.rol == "Investigador") Tarjeta("Nueva ficha de museo", "El administrador revisará su publicación", Icons.Default.Add) { vm.editarMuseo(null) }
        if (u.avisos && s.avisos.any { !it.leido }) Tarjeta("Tienes novedades", "${s.avisos.count { !it.leido }} avisos sin leer", Icons.Default.Notifications) { vm.ir(Pantalla.AVISOS) }
    }
}

@Composable fun Perfil(s: EstadoApp, vm: MuseoViewModel) {
    val u = s.usuario ?: return
    val context = LocalContext.current
    Pagina {
        when (s.pantalla) {
            Pantalla.EDITAR_PERFIL -> {
                Titulo("Editar perfil")
                SelectorImagen(s.formulario["avatar"] ?: "1", { vm.campo("avatar", it) }, vm::mensaje, avatar = true)
                Campo(s, vm, "nombre", "Nombre", Icons.Default.Person)
                Campo(s, vm, "correo", "Correo", Icons.Default.Email)
                Accion("Guardar perfil", !s.ocupado, onClick = vm::enviarFormulario)
            }
            Pantalla.AVISOS -> {
                Titulo("Novedades", "Avisos guardados en este dispositivo. Las notificaciones push corresponden a la siguiente etapa.")
                if (u.rol == "Administrador") {
                    var texto by rememberSaveable { mutableStateOf("") }
                    OutlinedTextField(texto, { texto = it }, label = { Text("Nuevo aviso (5 a 200 caracteres)") }, modifier = Modifier.fillMaxWidth())
                    Accion("Publicar aviso local", !s.ocupado && texto.trim().length in 5..200) { vm.anunciar(texto); texto = "" }
                }
                if (s.avisos.isEmpty()) Vacio("Todavía no tienes avisos.")
                s.avisos.forEach { a -> Tarjeta((if (a.leido) "" else "● ") + a.titulo, "${a.texto}\n${a.fecha}", Icons.Default.Notifications) { vm.abrirAviso(a) } }
            }
            else -> {
                Titulo("Mi perfil", u.rol)
                cl.museos.app.native.ImagenPerfil(u.avatar)
                Text(u.nombre, style = MaterialTheme.typography.titleLarge)
                Text(u.correo)
                Accion("Editar mis datos", !s.ocupado, Icons.Default.Edit, vm::editarPerfil)
                Preferencia("Tema oscuro", u.oscuro) { vm.preferencias(it, u.textoGrande, u.avisos) }
                Preferencia("Texto más grande", u.textoGrande) { vm.preferencias(u.oscuro, it, u.avisos) }
                Preferencia("Mostrar novedades en Inicio", u.avisos) { vm.preferencias(u.oscuro, u.textoGrande, it) }
                Text("Idioma: español", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton({ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }, Modifier.fillMaxWidth()) { Text("Permisos en ajustes del dispositivo") }
                OutlinedButton(vm::salir, Modifier.fillMaxWidth(), enabled = !s.ocupado) { Text("Cerrar sesión") }
                Text("Sebastian Carrasco · Daniel Perez\nJose Venegas · Jairo Vasquez", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
@Composable private fun Preferencia(texto: String, valor: Boolean, cambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(texto, Modifier.weight(1f).padding(top = 12.dp)); Switch(valor, cambiar)
    }
}
