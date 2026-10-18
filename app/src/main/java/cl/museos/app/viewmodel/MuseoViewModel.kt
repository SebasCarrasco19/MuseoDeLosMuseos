package cl.museos.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.museos.app.data.*
import cl.museos.app.domain.Reglas
import cl.museos.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MuseoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MuseoRepository(AppDatabase.crear(app))
    private val _estado = MutableStateFlow(EstadoApp())
    val estado = _estado.asStateFlow()
    private val anteriores = mutableListOf<Pantalla>()
    init { ejecutar {
        repo.iniciar(); refrescar()
        _estado.update { it.copy(pantalla = if (it.usuario == null) Pantalla.BIENVENIDA else Pantalla.INICIO) }
    } }
    private fun ejecutar(accion: suspend () -> Unit) {
        if (_estado.value.ocupado) return
        _estado.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { accion() } }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                _estado.update { it.copy(mensaje = e.message ?: "No se pudo completar la acción") }
            } finally { _estado.update { it.copy(ocupado = false, cargando = false) } }
        }
    }
    private suspend fun refrescar() {
        val datos = repo.cargar()
        _estado.update { it.copy(cargando = false, usuario = datos.usuario, productos = datos.productos,
            museos = datos.museos, favoritos = datos.favoritos, carrito = datos.carrito,
            pedidos = datos.pedidos, lineas = datos.lineas, avisos = datos.avisos, lecturas = datos.lecturas) }
    }
    private fun id() = _estado.value.usuario?.id ?: error("Inicia sesión")
    fun ir(p: Pantalla, principal: Boolean = false) {
        if (_estado.value.ocupado) return
        if (principal) anteriores.clear() else anteriores.add(_estado.value.pantalla)
        _estado.update { it.copy(pantalla = p, errores = emptyMap(), formulario = emptyMap(), mensaje = null) }
    }
    fun volver() {
        if (_estado.value.ocupado) return
        val p = if (anteriores.isEmpty()) { if (_estado.value.usuario == null) Pantalla.BIENVENIDA else Pantalla.INICIO } else anteriores.removeAt(anteriores.lastIndex)
        _estado.update { it.copy(pantalla = p, errores = emptyMap(), formulario = emptyMap()) }
    }
    fun abrir(p: Pantalla, referencia: Long) { ir(p); _estado.update { it.copy(seleccion = referencia) } }
    fun campo(nombre: String, valor: String) {
        _estado.update {
            val f = it.formulario + (nombre to valor)
            val e = Reglas.validar(it.pantalla, f)
            it.copy(formulario = f, errores = e.filterKeys { clave -> clave == nombre || clave in it.errores })
        }
    }
    fun rol(rol: String) { _estado.update { it.copy(rolLogin = rol, errores = emptyMap()) } }
    fun demo() {
        val correo = when (_estado.value.rolLogin) { "Administrador" -> "admin"; "Investigador" -> "investigador"; else -> "cliente" }
        _estado.update { it.copy(formulario = mapOf("correo" to "$correo@example.com", "clave" to "Museo123"), errores = emptyMap()) }
    }
    fun enviarFormulario() {
        val s = _estado.value
        val errores = Reglas.validar(s.pantalla, s.formulario)
        _estado.update { it.copy(errores = errores) }
        if (errores.isNotEmpty()) return
        ejecutar {
            val f = s.formulario
            fun v(k: String) = f[k].orEmpty().trim()
            when (s.pantalla) {
                Pantalla.LOGIN -> repo.login(v("correo"), f["clave"].orEmpty(), s.rolLogin)
                Pantalla.REGISTRO -> repo.registro(v("nombre"), v("correo"), f["clave"].orEmpty(), f["avatar"] ?: "1")
                Pantalla.EDITAR_PERFIL -> repo.perfil(id(), v("nombre"), v("correo"), f["avatar"] ?: "1")
                Pantalla.EDITAR_PRODUCTO -> repo.guardarProducto(id(), Producto(id = s.seleccion,
                    nombre = v("nombre"), descripcion = v("descripcion"), categoria = v("categoria"),
                    precio = v("precio").toInt(), stock = v("stock").toInt(),
                    peso = if (v("categoria") == "Cursos") 0.0 else v("peso").replace(',', '.').toDouble(),
                    activo = s.productos.find { it.id == s.seleccion }?.activo ?: true))
                Pantalla.EDITAR_MUSEO -> repo.guardarMuseo(id(), Museo(id = s.seleccion, nombre = v("nombre"),
                    descripcion = v("descripcion"), pais = v("pais"), ciudad = v("ciudad"), categoria = v("categoria"),
                    latitud = v("latitud").replace(',', '.').toDouble(), longitud = v("longitud").replace(',', '.').toDouble(),
                    autorId = id(), foto = v("foto"), enlace = v("enlace"), fecha = java.time.LocalDate.now().toString()))
                else -> return@ejecutar
            }
            refrescar()
            anteriores.clear()
            val destino = when (s.pantalla) { Pantalla.EDITAR_PRODUCTO -> Pantalla.CATALOGO; Pantalla.EDITAR_MUSEO -> Pantalla.MAPA; Pantalla.EDITAR_PERFIL -> Pantalla.PERFIL; else -> Pantalla.INICIO }
            _estado.update { it.copy(pantalla = destino, formulario = emptyMap(), mensaje = "Datos guardados correctamente") }
        }
    }
    fun editarPerfil() {
        val u = _estado.value.usuario ?: return
        ir(Pantalla.EDITAR_PERFIL)
        _estado.update { it.copy(formulario = mapOf("nombre" to u.nombre, "correo" to u.correo, "avatar" to u.avatar)) }
    }
    fun editarProducto(p: Producto?) {
        ir(Pantalla.EDITAR_PRODUCTO)
        _estado.update { it.copy(seleccion = p?.id ?: 0, formulario = mapOf("nombre" to (p?.nombre ?: ""),
            "descripcion" to (p?.descripcion ?: ""), "categoria" to (p?.categoria ?: "Publicaciones"),
            "precio" to (p?.precio?.toString() ?: ""), "stock" to (p?.stock?.toString() ?: ""), "peso" to (p?.peso?.toString() ?: ""))) }
    }
    fun editarMuseo(m: Museo?) {
        ir(Pantalla.EDITAR_MUSEO)
        _estado.update { it.copy(seleccion = m?.id ?: 0, formulario = mapOf("nombre" to (m?.nombre ?: ""),
            "descripcion" to (m?.descripcion ?: ""), "pais" to (m?.pais ?: ""), "ciudad" to (m?.ciudad ?: ""),
            "categoria" to (m?.categoria ?: "Historia"), "latitud" to (m?.latitud?.toString() ?: ""),
            "longitud" to (m?.longitud?.toString() ?: ""), "foto" to (m?.foto ?: ""), "enlace" to (m?.enlace ?: ""))) }
    }
    fun favorito(m: Long) = ejecutar { repo.favorito(id(), m); refrescar() }
    fun cantidad(producto: Long, cantidad: Int) = ejecutar { repo.carrito(id(), producto, cantidad); refrescar(); mensaje("Carrito actualizado") }
    fun agregar(p: Producto) = cantidad(p.id, (_estado.value.carrito.find { it.producto.id == p.id }?.cantidad ?: 0) + 1)
    fun destino(valor: String) { _estado.update { it.copy(destino = valor) } }
    fun modalidad(valor: String) { _estado.update { it.copy(modalidad = valor) } }
    fun pagar(aceptado: Boolean) {
        if (!aceptado) { _estado.update { it.copy(pantalla = Pantalla.CARRITO, mensaje = "Pago rechazado. No se creó una compra ni se descontó stock. Puedes reintentar.") }; anteriores.clear(); return }
        val s = _estado.value
        ejecutar {
            val pedido = repo.comprar(id(), s.destino, s.modalidad, s.carrito)
            refrescar(); anteriores.clear()
            _estado.update { it.copy(pedidoActual = pedido, pantalla = Pantalla.COMPROBANTE) }
        }
    }
    fun curso(p: Long) = ejecutar {
        val pedido = repo.solicitarCurso(id(), p); refrescar(); anteriores.clear()
        _estado.update { it.copy(pedidoActual = pedido, pantalla = Pantalla.COMPROBANTE) }
    }
    fun comprobante(id: Long) { ir(Pantalla.COMPROBANTE); _estado.update { it.copy(pedidoActual = id) } }
    fun avanzar(p: Pedido) = ejecutar {
        val pos = Reglas.estados.indexOf(p.estado)
        if (pos in 0..2) repo.estado(id(), p.id, Reglas.estados[pos + 1]); refrescar()
    }
    fun activar(p: Producto) = ejecutar { repo.activarProducto(id(), p.id); refrescar() }
    fun publicar(m: Museo) = ejecutar { repo.publicar(id(), m.id); refrescar() }
    fun qr(texto: String) = ejecutar {
        _estado.update { it.copy(qr = null) }
        val resultado = repo.leerQr(id(), texto); refrescar()
        _estado.update { it.copy(qr = resultado, pantalla = Pantalla.RESULTADO_QR) }
    }
    fun abrirQr() { val qr = _estado.value.qr ?: return; abrir(if (qr.tipo == "museo") Pantalla.MUSEO else Pantalla.PRODUCTO, qr.id) }
    fun abrirAviso(a: Aviso) = ejecutar { repo.leerAviso(id(), a.id); refrescar(); _estado.update { it.copy(pantalla = Pantalla.valueOf(a.destino)) } }
    fun anunciar(texto: String) = ejecutar { repo.anunciar(id(), texto); refrescar(); mensaje("Aviso local publicado") }
    fun preferencias(oscuro: Boolean, grande: Boolean, avisos: Boolean) = ejecutar { repo.preferencias(id(), oscuro, grande, avisos); refrescar() }
    fun salir() = ejecutar { repo.salir(); refrescar(); anteriores.clear(); _estado.value = EstadoApp(cargando = false) }
    fun mensaje(texto: String?) { _estado.update { it.copy(mensaje = texto) } }
}
