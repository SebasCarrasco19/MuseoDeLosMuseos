package cl.museos.app.domain

import cl.museos.app.model.*
import com.google.gson.JsonParser
import kotlin.math.ceil
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Reglas {
    val roles = listOf("Administrador", "Investigador", "Cliente")
    val categorias = listOf("Publicaciones", "Materiales", "Merch", "Cursos")
    val destinos = listOf("Chile - Santiago", "Chile - Regiones", "Iberoamérica", "Otros países")
    val modalidades = listOf("Estándar", "Express")
    val estados = listOf("Pendiente", "Confirmado", "Enviado", "Entregado")
    fun envio(destino: String, modalidad: String, peso: Double): Int {
        require(destino in destinos && modalidad in modalidades && peso.isFinite() && peso >= 0 && peso <= 2000)
        if (peso == 0.0) return 0
        val base = listOf(2500, 4500, 14500, 24000)[destinos.indexOf(destino)]
        val kilo = listOf(700, 1000, 3500, 5000)[destinos.indexOf(destino)]
        val normal = base + ceil(peso).toInt() * kilo
        return if (modalidad == "Express") ceil(normal * 1.5).toInt() else normal
    }
    fun validar(pantalla: Pantalla, f: Map<String, String>): Map<String, String> = buildMap {
        fun texto(campo: String, min: Int = 3, max: Int = 200) {
            if (f[campo].orEmpty().trim().length !in min..max) put(campo, "Escribe entre $min y $max caracteres")
        }
        if (pantalla in listOf(Pantalla.LOGIN, Pantalla.REGISTRO, Pantalla.EDITAR_PERFIL)) {
            if (!Regex("^[A-Za-z0-9._+-]+@example\\.com$").matches(f["correo"].orEmpty().trim().lowercase()))
                put("correo", "Usa un correo ficticio terminado en @example.com")
            if (pantalla != Pantalla.LOGIN) texto("nombre", 3, 50)
            if (pantalla != Pantalla.EDITAR_PERFIL && f["clave"].orEmpty().length !in 6..64)
                put("clave", "La contraseña debe tener entre 6 y 64 caracteres")
            if (pantalla == Pantalla.REGISTRO && f["repetir"] != f["clave"]) put("repetir", "Las contraseñas no coinciden")
        }
        if (pantalla == Pantalla.EDITAR_PRODUCTO) {
            texto("nombre", 3, 60); texto("descripcion", 10, 500)
            if (f["categoria"] !in categorias) put("categoria", "Selecciona una categoría")
            if (f["precio"]?.toIntOrNull() !in 1..1_000_000) put("precio", "Precio entre 1 y 1.000.000 CLP")
            if (f["stock"]?.toIntOrNull() !in 0..10000) put("stock", "Disponibilidad entre 0 y 10.000")
            val peso = f["peso"]?.replace(',', '.')?.toDoubleOrNull()
            if (f["categoria"] != "Cursos" && (peso == null || !peso.isFinite() || peso <= 0 || peso > 10)) put("peso", "Peso mayor que 0 y hasta 10 kg")
        }
        if (pantalla == Pantalla.EDITAR_MUSEO) {
            texto("nombre", 3, 60); texto("pais", 3, 40); texto("ciudad", 3, 50)
            texto("descripcion", 10, 500); texto("categoria", 3, 40)
            val lat = f["latitud"]?.replace(',', '.')?.toDoubleOrNull()
            val lon = f["longitud"]?.replace(',', '.')?.toDoubleOrNull()
            if (lat == null || !lat.isFinite() || lat !in -90.0..90.0) put("latitud", "Latitud entre -90 y 90")
            if (lon == null || !lon.isFinite() || lon !in -180.0..180.0) put("longitud", "Longitud entre -180 y 180")
            val enlace = f["enlace"].orEmpty().trim()
            if (enlace.isNotEmpty() && !enlaceValido(enlace)) put("enlace", "Usa un enlace HTTPS válido o deja el campo vacío")
        }
    }
    fun enlaceValido(texto: String): Boolean = runCatching {
        val uri = java.net.URI(texto)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && texto.length <= 500
    }.getOrDefault(false)
    // El QR identifica; los precios y permisos siempre se consultan en la base local.
    fun leerQr(texto: String): CodigoQr? = try {
        if (texto.length > 4096) null else {
            val objeto = JsonParser.parseString(texto).asJsonObject
            val tipo = objeto.get("tipo").asString
            val idTexto = objeto.get("id").asString
            val id = idTexto.toLongOrNull() ?: 0
            if (tipo in listOf("producto", "museo") && id > 0) CodigoQr(tipo, id) else null
        }
    } catch (_: Exception) { null }
}


object Claves {
    private fun derivar(clave: String, sal: ByteArray): ByteArray = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(clave.toCharArray(), sal, 120000, 256)).encoded
    fun crear(clave: String): String {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(sal) + ":" + Base64.getEncoder().encodeToString(derivar(clave, sal))
    }
    fun coincide(clave: String, guardada: String): Boolean = try {
        val partes = guardada.split(':')
        java.security.MessageDigest.isEqual(derivar(clave, Base64.getDecoder().decode(partes[0])), Base64.getDecoder().decode(partes[1]))
    } catch (_: Exception) { false }
}
