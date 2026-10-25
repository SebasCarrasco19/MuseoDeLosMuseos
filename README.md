# Museo de los Museos

Aplicación Android educativa para explorar museos, consultar fichas culturales, localizar espacios en un mapa y acceder a un catálogo de productos y cursos. Incluye distintos perfiles de usuario, lectura de códigos QR y flujos de compra **simulados**.

> **Estado:** proyecto demostrativo · **Versión declarada:** 2.0 · **Identificador:** `cl.museos.app`

## Funcionalidades

- **Acceso y perfiles:** inicio de sesión, registro de clientes, edición de perfil, avatar y preferencias de visualización (modo oscuro y texto ampliado).
- **Museos:** consulta de fichas, mapa interactivo, favoritos y gestión de información cultural según permisos.
- **Catálogo:** productos de distintas categorías y cursos, con control de disponibilidad.
- **Compras de demostración:** carrito, cálculo de envío, comprobantes e historial de solicitudes. No se procesan pagos reales.
- **Cursos:** solicitud de información sin cobro ni reserva efectiva de cupo.
- **Códigos QR:** escaneo de referencias a museos o productos, validación del contenido JSON e historial de lecturas.
- **Avisos:** notificaciones internas vinculadas a acciones y cambios de estado.
- **Administración:** gestión de productos, publicación de museos, actualización de solicitudes y envío de avisos.

## Roles

| Rol | Uso principal |
| --- | --- |
| **Cliente** | Explorar museos, guardar favoritos, consultar productos, utilizar el carrito y revisar solicitudes. |
| **Investigador** | Consultar museos y crear o editar sus propias fichas; las nuevas fichas requieren publicación por un administrador. |
| **Administrador** | Gestionar catálogo, museos, publicaciones, estados de solicitudes y avisos. |

Las pantallas y operaciones disponibles dependen del rol activo.

## Tecnologías

- **Kotlin** y **Jetpack Compose / Material 3** para la interfaz.
- **AndroidX ViewModel**, **StateFlow** y corrutinas para el estado y las operaciones asíncronas.
- **Room / SQLite** para la persistencia local.
- **osmdroid** para los mapas.
- **Google Code Scanner (Google Play Services)** para la lectura de QR.
- **Coil** para la carga de imágenes y **Gson** para el tratamiento de JSON.
- **Gradle** con Kotlin DSL y **JUnit** para pruebas unitarias.

## Requisitos

- Android Studio compatible con el proyecto y SDK de Android **36** instalado.
- **JDK 17**.
- Android **8.0 (API 26)** o superior para ejecutar la aplicación.
- Acceso a Internet para recursos de red, como mapas o imágenes, y Google Play Services para el escaneo QR en dispositivos compatibles.

## Instalación y ejecución

1. Clona el repositorio o descarga y descomprime el proyecto.
2. Abre la carpeta `MuseoDeLosMuseos` en **Android Studio**.
3. Espera a que Gradle sincronice las dependencias.
4. Selecciona un emulador o dispositivo Android compatible.
5. Ejecuta la configuración de la aplicación (`app`).

También puedes compilar desde la raíz del proyecto:

```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

El APK de depuración se genera normalmente en `app/build/outputs/apk/debug/`.

## Cuentas de demostración

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@example.com` | `Museo123` |
| Investigador | `investigador@example.com` | `Museo123` |
| Cliente | `cliente@example.com` | `Museo123` |

Estas cuentas se crean como datos iniciales de prueba. **No deben reutilizarse en producción.** La aplicación incluye una opción para completar automáticamente las credenciales de demostración.

## Estructura del código

```text
MuseoDeLosMuseos/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/cl/museos/app/
│       │   │   ├── data/        # Room, DAO, repositorio y datos iniciales
│       │   │   ├── domain/      # Reglas y validaciones
│       │   │   ├── model/       # Entidades y estado de la aplicación
│       │   │   ├── native/      # Mapas y recursos de imágenes
│       │   │   ├── ui/          # Pantallas y componentes Compose
│       │   │   ├── viewmodel/   # Estado y coordinación de acciones
│       │   │   └── MainActivity.kt
│       │   └── AndroidManifest.xml
│       ├── test/                # Pruebas unitarias
│       └── androidTest/         # Pruebas instrumentadas
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Datos y funcionamiento

La aplicación utiliza una base de datos **local Room**, con entidades para usuarios, museos, productos, favoritos, carrito, pedidos, líneas de pedido, avisos, lecturas QR y sesión. Al iniciarse por primera vez, el repositorio carga datos sintéticos de demostración.

La lógica sigue, en términos generales, el flujo **interacción de pantalla → ViewModel → repositorio / reglas → base de datos → actualización del estado (`StateFlow`) → interfaz Compose**.

Las operaciones de compra no se conectan con una pasarela de pagos ni generan transacciones monetarias reales. Los museos y productos precargados son datos de laboratorio.

## Formato QR de prueba

La aplicación admite referencias JSON como:

```json
{"tipo":"museo","id":1}
```

```json
{"tipo":"producto","id":1}
```

El identificador debe corresponder a un registro disponible y autorizado. La pantalla QR también ofrece validación manual de texto para pruebas sin cámara.

## Pruebas

Desde la raíz del proyecto:

```bash
# Windows: pruebas unitarias
.\gradlew.bat testDebugUnitTest

# Windows: pruebas instrumentadas (requieren emulador o dispositivo)
.\gradlew.bat connectedDebugAndroidTest
```

En Linux o macOS sustituye `gradlew.bat` por `./gradlew`.

El repositorio incluye `ReglasTest.kt`, `PantallasTest.kt` y `RepositorioTest.kt`. La presencia de estas pruebas no implica que se haya verificado su ejecución en todos los dispositivos.

## Limitaciones y alcance

- Es una aplicación **demostrativa**, no un servicio comercial listo para producción.
- La información se almacena localmente; no hay un backend de sincronización de cuentas y datos entre dispositivos documentado en el código.
- Las credenciales de demostración y los contenidos precargados son ficticios.
- El lector QR depende de la disponibilidad de Google Play Services y puede requerir una descarga inicial.
- Las compras y las consultas de cursos son simulaciones.

## Próximas mejoras

- Revisar la experiencia de navegación, los mensajes de error y los estados vacíos.
- Ampliar la cobertura de pruebas y comprobar los flujos completos en un dispositivo.
- Revisar seguridad y persistencia de datos antes de considerar un despliegue real.
- Incorporar capturas de pantalla verificadas de las principales funcionalidades.
- Preparar una versión de distribución sin datos ni accesos de demostración, si se proyecta publicar la aplicación.

## Licencia y autoría

Pendiente de definir por los responsables del proyecto. No se presupone una licencia de código abierto.
