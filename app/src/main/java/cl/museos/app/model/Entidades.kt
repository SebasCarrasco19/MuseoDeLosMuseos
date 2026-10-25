package cl.museos.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios", indices = [Index(value = ["correo"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String, val correo: String, val clave: String, val rol: String = "Cliente",
    val avatar: String = "1", val oscuro: Boolean = false, val textoGrande: Boolean = false,
    val avisos: Boolean = true
)

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String, val descripcion: String, val categoria: String,
    val precio: Int, val stock: Int, val peso: Double, val activo: Boolean = true
) { val esCurso: Boolean get() = categoria == "Cursos" }

@Entity(tableName = "museos")
data class Museo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String, val pais: String, val ciudad: String, val descripcion: String,
    val categoria: String, val latitud: Double, val longitud: Double,
    val autorId: Long, val publicado: Boolean = true, val foto: String = "",
    val fecha: String = "15/10/2026", val enlace: String = ""
)

@Entity(tableName = "favoritos", primaryKeys = ["usuarioId", "museoId"])
data class Favorito(val usuarioId: Long, val museoId: Long)

@Entity(tableName = "carrito", primaryKeys = ["usuarioId", "productoId"])
data class ItemCarrito(val usuarioId: Long, val productoId: Long, val cantidad: Int)

@Entity(tableName = "lecturas", primaryKeys = ["usuarioId", "tipo", "referencia"])
data class Lectura(val usuarioId: Long, val tipo: String, val referencia: Long, val titulo: String, val fecha: String)

@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long, val nombreCliente: String, val fecha: String,
    val subtotal: Int, val envio: Int, val destino: String, val modalidad: String,
    val estado: String = "Pendiente", val tipo: String = "Compra"
) { val total: Int get() = subtotal + envio }

@Entity(tableName = "lineas_pedido")
data class LineaPedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Long, val productoId: Long, val nombre: String, val precio: Int, val cantidad: Int
)

@Entity(tableName = "avisos")
data class Aviso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long, val titulo: String, val texto: String,
    val destino: String = "HISTORIAL", val fecha: String, val leido: Boolean = false
)

@Entity(tableName = "sesion")
data class Sesion(@PrimaryKey val id: Int = 1, val usuarioId: Long = 0)

data class FilaCarrito(val producto: Producto, val cantidad: Int) {
    val subtotal: Int get() = producto.precio * cantidad
}

data class CodigoQr(val tipo: String = "", val id: Long = 0)

enum class Pantalla {
    BIENVENIDA, LOGIN, REGISTRO, INICIO, MAPA, MUSEO, VISITA, CATALOGO,
    PRODUCTO, FAVORITOS, CARRITO, PAGO, COMPROBANTE, HISTORIAL, AVISOS,
    PERFIL, EDITAR_PERFIL, QR, RESULTADO_QR, EDITAR_PRODUCTO, EDITAR_MUSEO
}

data class EstadoApp(
    val cargando: Boolean = true, val ocupado: Boolean = false,
    val usuario: Usuario? = null, val pantalla: Pantalla = Pantalla.BIENVENIDA,
    val productos: List<Producto> = emptyList(), val museos: List<Museo> = emptyList(),
    val favoritos: List<Long> = emptyList(), val carrito: List<FilaCarrito> = emptyList(),
    val pedidos: List<Pedido> = emptyList(), val lineas: List<LineaPedido> = emptyList(),
    val lecturas: List<Lectura> = emptyList(), val avisos: List<Aviso> = emptyList(),
    val formulario: Map<String, String> = emptyMap(), val errores: Map<String, String> = emptyMap(),
    val rolLogin: String = "Cliente", val seleccion: Long = 0, val pedidoActual: Long = 0,
    val qr: CodigoQr? = null, val mensaje: String? = null,
    val destino: String = "Chile - Santiago", val modalidad: String = "Estándar"
)