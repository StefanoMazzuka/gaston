# Gaston · sacos de gastos

Proyecto Android nativo para abrir directamente en Android Studio. Kotlin, Compose y Room; Android 8.0 (API 26) o posterior.

## Abrir en Android Studio

1. Selecciona **Open** y abre la carpeta que contiene `settings.gradle.kts` (esta carpeta).
2. Deja que termine la sincronización de Gradle. Se necesita internet la primera vez.
3. Usa JDK 17 para Gradle y descarga Android SDK Platform 35 y Build Tools 35.0.0 si Studio lo solicita.
4. Conecta tu móvil por USB, activa la depuración USB y acepta la autorización.
5. Selecciona el móvil y pulsa **Run** sobre la configuración `app`.

Si Android Studio está en Windows, es preferible copiar el proyecto a una carpeta Windows para compilar allí. No copies `.gradle`, `local.properties` ni directorios `build`. Studio configura su propio SDK; no reutilices rutas Linux en Windows.

## Estructura

```text
app/src/main/
  AndroidManifest.xml
  java/com/gaston/app/
    MainActivity.kt
    domain/Budget.kt         # Dinero, fechas y presupuestos
    data/                   # Room y repositorio
    ui/                     # Pantallas, estado y saco Canvas
  res/                      # Tema e icono
app/src/test/               # Pruebas de las reglas financieras
app/schemas/                # Esquemas Room generados al compilar
```

Versiones fijadas: AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20, Compose BOM 2025.04.01, Room 2.7.1. Se usan versiones compatibles explícitas, sin actualizaciones automáticas de dependencias.

## Incluido

- Varios sacos independientes por banco.
- Creación con ingreso, fecha, salidas con iconos y ahorro fijo o porcentual.
- Disponible inicial opcional para empezar a mitad del ciclo.
- Fecha y presupuesto diario, siete días y total restante.
- Interfaz sobria con tarjetas de saldo y presupuesto diario.
- Edición de nombre, ingresos, día de cobro, ahorro y salidas fijas.
- Eliminación de sacos con confirmación, incluidos sus gastos e historial.
- Registro de gastos y deshacer con confirmación.
- Calendario, movimientos diarios y saldos al cierre de ciclos anteriores.
- Persistencia local Room, actualización de ciclos al regresar y cambio de día.

## Reglas de esta primera versión

- Si no existe el día elegido, se usa el último día del mes; después se retrasa al lunes si cae en fin de semana. No se contemplan festivos.
- El ahorro porcentual se calcula sobre el ingreso.
- El primer ciclo empieza hoy. El disponible real opcional ya debe excluir pagos y ahorro reservados.
- El presupuesto de hoy se fija sobre el saldo al empezar el día; los gastos de hoy se restan de ese margen. Mañana se reparte el saldo restante de nuevo.
- Los céntimos se guardan como enteros. El reparto diario redondea hacia abajo; el resto permanece disponible.
- Los excesos de gasto se muestran como importes negativos.
- Al renovar el ciclo, el saldo anterior queda visible en el historial. No se transfiere automáticamente al siguiente saco ni a un cofre de ahorro.
- Las reservas se descuentan al calcular el presupuesto inicial. No deben registrarse otra vez como gasto libre.

## Pendiente

Marcado de reservas pagadas, exportación/restauración, cofre de ahorro, política definitiva de sobrantes y pruebas instrumentadas en móvil.

Al editar, el nombre cambia inmediatamente. La configuración financiera se aplica al siguiente ciclo: el saldo, ahorro y fecha de cierre del ciclo actual se conservan. Si cambia el día de cobro, el siguiente ciclo empieza al cierre del actual y termina en la siguiente fecha del nuevo calendario. El formulario permite añadir, editar y quitar salidas fijas. Eliminar un saco borra también sus salidas fijas, ciclos y gastos; los demás sacos se conservan.

## Compilación

```sh
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. En Windows usa `gradlew.bat`. La versión debug está firmada para pruebas. Para distribuir usa **Generate Signed App Bundle / APK** y conserva tu clave privada fuera del repositorio.

No se necesitan permisos de internet en la app ni conexión bancaria. La base de datos se guarda en el almacenamiento privado del móvil. Esta versión no incluye copia de seguridad: desinstalar elimina los datos.

## Verificación realizada

Compilación `testDebugUnitTest assembleDebug` completada correctamente con JDK 17 y SDK 35. Pasan 12 pruebas: 8 de presupuesto y 4 de persistencia Room con Robolectric que cubren edición, conservación de ciclos, rechazo de cambios inválidos y borrado en cascada sin afectar a otros sacos. Se ha generado el APK debug. Queda pendiente probar la interfaz y la persistencia en el móvil físico; no se ha conectado ningún dispositivo durante esta verificación.
