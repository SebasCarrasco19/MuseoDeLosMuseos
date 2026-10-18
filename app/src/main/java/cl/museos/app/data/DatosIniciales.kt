package cl.museos.app.data

import cl.museos.app.model.*

object DatosIniciales {
    val productos = listOf(
        Producto(nombre = "Arte colonial ilustrado", descripcion = "Publicación de demostración sobre arquitectura, objetos y memoria cultural. Edición ilustrada de 120 páginas.", categoria = "Publicaciones", precio = 12000, stock = 12, peso = 0.7),
        Producto(nombre = "Kit de conservación", descripcion = "Material educativo para practicar conservación preventiva: guantes de algodón, pincel suave y caja de archivo.", categoria = "Materiales", precio = 18900, stock = 8, peso = 1.2),
        Producto(nombre = "Cuaderno de exploración", descripcion = "Cuaderno de campo para dibujar piezas, registrar una visita y descubrir historias. Papel reciclado y 80 páginas.", categoria = "Merch", precio = 6500, stock = 20, peso = 0.3),
        Producto(nombre = "Museos y comunidad", descripcion = "Curso en línea sobre participación y mediación cultural. Duración de referencia: 8 horas. Información y cupos ficticios.", categoria = "Cursos", precio = 35000, stock = 20, peso = 0.0),
        Producto(nombre = "Colecciones digitales", descripcion = "Curso introductorio de documentación de colecciones. Sin cupos para demostrar el control de disponibilidad.", categoria = "Cursos", precio = 28000, stock = 0, peso = 0.0)
    )
    fun museos(autor: Long) = listOf(
        Museo(nombre = "Museo de la Memoria del Valle", pais = "Chile", ciudad = "Santiago", descripcion = "Museo ficticio que reúne objetos, relatos y oficios de un valle imaginario. Explora su sala digital de memoria cotidiana.", categoria = "Historia", latitud = -33.45, longitud = -70.66, autorId = autor),
        Museo(nombre = "Galería de las Formas", pais = "Perú", ciudad = "Lima", descripcion = "Colección sintética que conecta formas, materiales y diseño. Una experiencia de aprendizaje sobre la creación visual.", categoria = "Arte", latitud = -12.05, longitud = -77.04, autorId = autor),
        Museo(nombre = "Archivo del Río", pais = "Argentina", ciudad = "Buenos Aires", descripcion = "Archivo de demostración dedicado a los cambios de la ciudad y su relación con el agua. Todas las piezas son ficticias.", categoria = "Historia", latitud = -34.60, longitud = -58.38, autorId = autor),
        Museo(nombre = "Casa de los Encuentros", pais = "Colombia", ciudad = "Bogotá", descripcion = "Espacio ficticio de patrimonio comunitario y relatos compartidos. Recorre una exposición virtual de prueba.", categoria = "Comunidad", latitud = 4.71, longitud = -74.07, autorId = autor),
        Museo(nombre = "Museo Digital del Sol", pais = "México", ciudad = "Ciudad de México", descripcion = "Museo de laboratorio sobre las relaciones entre arte y naturaleza. Las imágenes y relatos son demostrativos.", categoria = "Arte", latitud = 19.43, longitud = -99.13, autorId = autor),
        Museo(nombre = "Museo de las Conexiones", pais = "España", ciudad = "Madrid", descripcion = "Colección ficticia sobre intercambios culturales de Iberoamérica. Incluye un recorrido local para probar sin internet.", categoria = "Comunidad", latitud = 40.42, longitud = -3.70, autorId = autor)
    )
}
