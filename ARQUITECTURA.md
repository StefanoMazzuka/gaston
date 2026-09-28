# Arquitectura propuesta para Android

Referencia inicial de arquitectura. La versión completa se mantiene en el apartado Arquitectura de DEFINICION_APP.md. Ya existe una primera implementación: consulta README.md para abrirla en Android Studio y revisar el alcance real.

## Enfoque

App nativa Android con Kotlin y Jetpack Compose. Primera versión propuesta: datos locales, registro manual y funcionamiento sin conexión. Cada saco corresponde a un banco configurado por el usuario; no existe conexión bancaria en este alcance.

## Herramientas

| Herramienta | Uso |
|---|---|
| Android Studio | Editor, depuración, previsualizaciones y emulador |
| Android SDK y Platform Tools (ADB) | Compilar, instalar y depurar en Android |
| JDK compatible con Android Gradle Plugin | Ejecutar la compilación; usar el incluido en Android Studio cuando sea compatible |
| Gradle Wrapper y Android Gradle Plugin | Compilación reproducible y generación del APK |
| Kotlin | Lenguaje de la app y de los cálculos |
| Jetpack Compose y Material 3 | Pantallas, formularios y controles |
| Compose Canvas y animaciones | Saco y gemas en 2D |
| Navigation Compose | Navegación entre pantallas |
| ViewModel, StateFlow y coroutines | Estado de pantalla y operaciones asíncronas |
| Room sobre SQLite y KSP | Persistencia de sacos, ciclos, reservas y movimientos |
| DataStore | Preferencias pequeñas, como tema o animaciones |
| java.time | Fechas y ciclos, con desugaring si el Android mínimo lo requiere |
| kotlinx.serialization y selector de archivos de Android | Exportación e importación de copias JSON versionadas |
| JUnit, pruebas de Room y Compose UI Test | Validar cálculos, persistencia y recorridos principales |
| Emulador Android y teléfono físico | Comprobar interfaz, rendimiento e instalación |
| Git | Historial del código |
| Keystore de firma | Firmar el APK de distribución y futuras actualizaciones |

Fijar versiones estables compatibles al crear el proyecto. La firma y sus contraseñas se guardan fuera del repositorio y se conserva una copia segura.

## Organización

Un módulo de aplicación inicial, con paquetes separados:

- `ui/`: selector de sacos, creación, detalle, registro de gastos, calendario y ajustes.
- `domain/`: reglas de presupuesto, ahorro, redondeo y fechas de cobro; Kotlin sin dependencias de interfaz.
- `data/`: Room, repositorios, preferencias y copias de seguridad.

Flujo: pantalla → ViewModel → reglas y repositorio → base de datos. Los cambios de datos actualizan la pantalla mediante Flow. Inyección manual de dependencias inicialmente.

## Datos principales

- Saco: identificador, nombre del banco, moneda, ingreso mensual, día habitual de cobro y configuración de ahorro.
- Gasto fijo: saco, nombre, icono e importe.
- Ciclo: saco, fechas efectivas, ingreso y reservas del periodo. Conserva una copia de las condiciones para no modificar el historial al editar la configuración.
- Reserva del ciclo: gasto fijo reservado e información de pago.
- Movimiento: saco, ciclo, importe, fecha y categoría o concepto.

Guardar dinero en céntimos con enteros de 64 bits. Usar aritmética decimal y una regla explícita de redondeo para porcentajes. El saldo se obtiene de los datos financieros; las gemas solo lo representan.

## Fechas y actualización

Cada saco tiene su ciclo. El sábado o domingo de cobro se desplaza al lunes. Al abrir o volver a la app se comprueba la fecha y se actualiza el ciclo; si permanece abierta al cambiar de día, también se actualiza. La creación de ciclos debe evitar duplicados y contemplar varios meses sin abrir la app.

No se necesita un proceso permanente en segundo plano para los cálculos. Si posteriormente se añaden recordatorios, se evaluarán las herramientas de notificaciones y planificación de Android.

Separar el presupuesto asignado al inicio del día, lo ya gastado hoy y la previsión para los días siguientes. Definir y probar esta regla antes de implementar para evitar repartir otra vez el saldo tras cada gasto y mostrar un margen diario engañoso.

## Interfaz

1. Entrada: sacos existentes y crear saco.
2. Configuración: ingreso, fecha, salidas e importe o porcentaje de ahorro.
3. Saco: fecha actual, disponible de hoy, total, gemas y botón «− Sacar».
4. Registrar gasto: importe y categoría o concepto.
5. Calendario e historial por saco.
6. Ajustes y copia de seguridad.

Dibujos vectoriales y Canvas con animaciones breves. Gemas decorativas sin equivalencia individual en euros. El número de elementos dibujados se limita para mantener fluidez. Mostrar importes legibles, etiquetas accesibles y opción de reducir animaciones.

## Verificación y entrega

- Pruebas de fechas: fines de semana, febrero, años bisiestos y cambio de año.
- Pruebas de dinero: redondeo, ahorro, sobrantes, exceso de gasto y reservas sin doble descuento.
- Pruebas de independencia entre sacos y actualización de ciclos sin duplicados.
- Comprobar persistencia tras cerrar la app y restauración de una copia.
- Probar creación de saco y registro de gasto en emulador y móvil.
- Generar APK de prueba y después APK de distribución firmado. Conservar la misma clave para actualizaciones.

El APK se puede instalar directamente. Publicar en Google Play es un paso independiente que requiere su cuenta y un paquete AAB para el flujo habitual de publicación.

## Pendientes antes de cerrar las reglas

Resolver los puntos funcionales pendientes en DEFINICION_APP.md, especialmente sobrantes entre ciclos, primer uso a mitad de mes y días de cobro inexistentes. Definir Android mínimo, comportamiento ante saldo negativo y formato de copia de seguridad.

## Fuentes oficiales

- Arquitectura: https://developer.android.com/topic/architecture/recommendations
- Compose: https://developer.android.com/develop/ui/compose/documentation
- Datos locales: https://developer.android.com/topic/architecture/data-layer/offline-first
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Compilación: https://developer.android.com/build/building-cmdline
- Firma: https://developer.android.com/studio/publish/app-signing
