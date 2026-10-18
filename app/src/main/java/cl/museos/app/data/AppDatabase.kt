package cl.museos.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.museos.app.model.*

@Database(entities = [Usuario::class, Producto::class, Museo::class, Favorito::class,
    ItemCarrito::class, Pedido::class, LineaPedido::class, Lectura::class, Aviso::class, Sesion::class],
    version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun museoDao(): MuseoDao
    companion object {
        @Volatile private var instancia: AppDatabase? = null
        fun crear(context: Context): AppDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "museos.db")
                .build().also { instancia = it }
        }
    }
}
