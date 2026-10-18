package cl.museos.app.data

import androidx.room.*
import cl.museos.app.model.*


@Dao
interface MuseoDao {
    @Query("SELECT * FROM usuarios WHERE id = :id") suspend fun usuario(id: Long): Usuario?
    @Query("SELECT * FROM usuarios WHERE correo = :correo") suspend fun correo(correo: String): Usuario?
    @Query("SELECT * FROM usuarios") suspend fun usuarios(): List<Usuario>
    @Insert suspend fun insertarUsuario(usuario: Usuario): Long
    @Update suspend fun actualizarUsuario(usuario: Usuario)
    @Query("SELECT * FROM productos ORDER BY id") suspend fun productos(): List<Producto>
    @Query("SELECT * FROM productos WHERE id = :id") suspend fun producto(id: Long): Producto?
    @Insert suspend fun insertarProducto(producto: Producto): Long
    @Update suspend fun actualizarProducto(producto: Producto)
    @Query("SELECT * FROM museos ORDER BY id") suspend fun museos(): List<Museo>
    @Query("SELECT * FROM museos WHERE id = :id") suspend fun museo(id: Long): Museo?
    @Insert suspend fun insertarMuseo(museo: Museo): Long
    @Update suspend fun actualizarMuseo(museo: Museo)
    @Query("SELECT museoId FROM favoritos WHERE usuarioId = :usuario") suspend fun favoritos(usuario: Long): List<Long>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun favorito(favorito: Favorito)
    @Query("DELETE FROM favoritos WHERE usuarioId = :usuario AND museoId = :museo") suspend fun quitarFavorito(usuario: Long, museo: Long)
    @Query("SELECT * FROM carrito WHERE usuarioId = :usuario") suspend fun carrito(usuario: Long): List<ItemCarrito>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarItem(item: ItemCarrito)
    @Query("DELETE FROM carrito WHERE usuarioId = :usuario AND productoId = :producto") suspend fun quitarItem(usuario: Long, producto: Long)
    @Query("DELETE FROM carrito WHERE usuarioId = :usuario") suspend fun vaciarCarrito(usuario: Long)
    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuario ORDER BY id DESC") suspend fun pedidos(usuario: Long): List<Pedido>
    @Query("SELECT * FROM pedidos ORDER BY id DESC") suspend fun todosPedidos(): List<Pedido>
    @Query("SELECT * FROM pedidos WHERE id = :id") suspend fun pedido(id: Long): Pedido?
    @Insert suspend fun insertarPedido(pedido: Pedido): Long
    @Update suspend fun actualizarPedido(pedido: Pedido)
    @Insert suspend fun insertarLinea(linea: LineaPedido)
    @Query("SELECT * FROM lineas_pedido WHERE pedidoId IN (:pedidos)") suspend fun lineas(pedidos: List<Long>): List<LineaPedido>
    @Query("SELECT * FROM lecturas WHERE usuarioId = :usuario ORDER BY fecha DESC") suspend fun lecturas(usuario: Long): List<Lectura>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarLectura(lectura: Lectura)
    @Query("SELECT * FROM avisos WHERE usuarioId = :usuario ORDER BY id DESC") suspend fun avisos(usuario: Long): List<Aviso>
    @Insert suspend fun insertarAviso(aviso: Aviso)
    @Query("UPDATE avisos SET leido = 1 WHERE id = :id AND usuarioId = :usuario") suspend fun leerAviso(id: Long, usuario: Long)
    @Query("SELECT usuarioId FROM sesion WHERE id = 1") suspend fun sesion(): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarSesion(sesion: Sesion)
}
