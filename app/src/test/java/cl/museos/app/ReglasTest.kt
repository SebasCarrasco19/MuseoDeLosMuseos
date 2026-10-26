package cl.museos.app

import cl.museos.app.domain.*
import cl.museos.app.model.*
import org.junit.Assert.*
import org.junit.Test

class ReglasTest {
    @Test fun envioRedondeaKilosYAplicaExpress() {
        assertEquals(3900, Reglas.envio("Chile - Santiago", "Estándar", 1.01))
        assertEquals(5850, Reglas.envio("Chile - Santiago", "Express", 1.01))
        assertEquals(21500, Reglas.envio("Iberoamérica", "Estándar", 1.01))
        assertEquals(0, Reglas.envio("Otros países", "Express", 0.0))
    }
    @Test fun cotizadorRechazaDatosInvalidos() {
        for (peso in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, 2001.0)) {
            assertTrue(runCatching { Reglas.envio("Chile - Santiago", "Estándar", peso) }.isFailure)
        }
        assertTrue(runCatching { Reglas.envio("Marte", "Estándar", 1.0) }.isFailure)
    }
    @Test fun carritoMaximoAdmiteVeinteProductosDeDiezUnidades() {
        assertEquals(10024000, Reglas.envio("Otros países", "Estándar", 2000.0))
    }
    @Test fun qrValidaEstructuraTipoEIdentificador() {
        assertEquals(CodigoQr("museo", 1), Reglas.leerQr("{\"tipo\":\"museo\",\"id\":1}"))
        assertEquals(CodigoQr("compra", 15), Reglas.leerQr("{\"tipo\":\"compra\",\"id\":15}"))
        for (qr in listOf("https://example.com", "{}", "[]", "{\"tipo\":\"usuario\",\"id\":1}", "{\"tipo\":\"museo\",\"id\":-1}", "{\"tipo\":\"museo\",\"id\":1.5}")) assertNull(Reglas.leerQr(qr))
    }
    @Test fun registroValidaCamposYConfirmacion() {
        val valido = mapOf("nombre" to "Cliente prueba", "correo" to "prueba@example.com", "clave" to "Museo123", "repetir" to "Museo123")
        assertTrue(Reglas.validar(Pantalla.REGISTRO, valido).isEmpty())
        assertTrue("repetir" in Reglas.validar(Pantalla.REGISTRO, valido + ("repetir" to "distinta")))
        assertEquals(setOf("nombre", "correo", "clave"), Reglas.validar(Pantalla.REGISTRO, emptyMap()).keys)
    }
    @Test fun museoNoAceptaCoordenadasFueraDeRangoNiEnlaceInseguro() {
        val campos = mapOf("nombre" to "Museo prueba", "pais" to "Chile", "ciudad" to "Santiago", "descripcion" to "Descripción de prueba", "categoria" to "Historia", "latitud" to "-33.4", "longitud" to "-70.6")
        assertTrue(Reglas.validar(Pantalla.EDITAR_MUSEO, campos).isEmpty())
        assertEquals(setOf("latitud", "longitud", "enlace"), Reglas.validar(Pantalla.EDITAR_MUSEO, campos + mapOf("latitud" to "NaN", "longitud" to "200", "enlace" to "javascript:alert(1)")).keys)
    }
    @Test fun productoValidaStockPesoYPrecio() {
        val campos = mapOf("nombre" to "Curso demo", "descripcion" to "Descripción de prueba", "categoria" to "Cursos", "precio" to "20000", "stock" to "5")
        assertTrue(Reglas.validar(Pantalla.EDITAR_PRODUCTO, campos).isEmpty())
        assertEquals(setOf("precio", "stock", "peso"), Reglas.validar(Pantalla.EDITAR_PRODUCTO, campos + mapOf("categoria" to "Merch", "precio" to "-1", "stock" to "-1", "peso" to "0")).keys)
    }
    @Test fun contrasenasSeGuardanConSalYNoEnTextoPlano() {
        val primera = Claves.crear("Museo123")
        assertNotEquals(primera, Claves.crear("Museo123"))
        assertFalse(primera.contains("Museo123"))
        assertTrue(Claves.coincide("Museo123", primera))
        assertFalse(Claves.coincide("incorrecta", primera))
    }
}
