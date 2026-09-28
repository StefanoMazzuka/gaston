# App móvil de gastos — definición inicial

Documento vivo con lo hablado hasta ahora. Las propuestas aún no confirmadas se recogen al final.

## Objetivo

Ayudar a saber cuánto se puede gastar cada día y cada semana hasta el próximo ingreso, después de reservar los gastos fijos y el ahorro. El usuario registra sus gastos y la app actualiza el dinero disponible.

La interfaz permite gestionar varios sacos de gemas, cada uno asociado a un banco. Dentro de cada saco se muestra el dinero disponible para gastar.

## Entrada a la app y sacos por banco

- Al entrar se pueden abrir los sacos existentes o crear otro.
- Cada saco representa un banco y permite identificarlo por su nombre.
- Al abrir un saco se accede a sus gemas, información y registro de gastos.
- La pantalla del saco muestra la fecha de hoy y cuánto queda por gastar ese día, con el importe en euros destacado.
- La acción principal es «− Sacar»: sacar gemas significa registrar un gasto en ese saco.
- Al pulsarla se introduce el importe y la categoría o concepto; al guardar, salen gemas y se actualizan los importes.
- Los gastos quedan asociados al saco desde el que se registran.

Cada saco se configura de forma independiente: tiene su propio ingreso mensual, fecha habitual de cobro, gastos reservados y ahorro. Al crear otro saco se repite el proceso de configuración.

El saldo, el presupuesto diario y semanal, el ciclo y los movimientos se calculan por saco. Registrar un gasto en un saco solo afecta a ese saco.

## Configuración de cada saco

Este proceso se realiza al crear cada saco:

1. Introducir su único ingreso mensual y el día habitual de cobro.
2. Introducir los gastos fijos o comprometidos, cada uno con nombre, importe e icono elegible.
3. Elegir el ahorro reservado: una cantidad fija o un porcentaje.
4. Calcular el dinero libre y repartirlo entre los días hasta el próximo cobro.

Ejemplos de gastos e iconos:

| Gasto | Icono orientativo |
|---|---|
| Alquiler o hipoteca | Casa |
| Electricidad | Bombilla |
| Netflix u otras suscripciones | Televisión |
| Dinero para familiares | Corazón |
| Otros gastos | Selección entre varios iconos |

## Ciclos y fecha de cobro

- Cada saco tiene un único ingreso mensual recurrente y su propio ciclo.
- El usuario configura un día habitual del mes.
- Si ese día cae en sábado o domingo, el cobro se retrasa siempre al lunes siguiente.
- La fecha ajustada marca el inicio del nuevo ciclo y el reseteo del presupuesto.
- El ajuste no cambia el día habitual configurado para los meses siguientes.
- El saco debe durar hasta el cobro ajustado, incluyendo los días adicionales del fin de semana.
- Los fines de semana cuentan como días de gasto aunque no sean días de cobro.
- El cálculo utiliza los días reales de cada ciclo, no un mes fijo de 30 días.

Ejemplo: si el día habitual es el 25 y cae en sábado, el nuevo ciclo comienza el lunes 27. El mes siguiente se vuelve a partir del día 25 y se aplica la misma regla.

El reseteo renueva el presupuesto; no debe borrar el historial. Al abrir el nuevo ciclo se vuelven a contemplar los gastos fijos y el ahorro.

## Cálculo del presupuesto

Estas fórmulas se aplican por separado a cada saco, usando únicamente sus ingresos, reservas y gastos.

```text
Disponible inicial = ingreso mensual − gastos reservados − ahorro reservado
Disponible restante = disponible inicial − gastos registrados del saco
Presupuesto diario = disponible restante ÷ días restantes hasta el próximo cobro
```

Al abrir el día, el reparto incluye ese día y llega hasta el día anterior al próximo cobro. El día de cobro pertenece al nuevo ciclo.

El dinero que no se gasta permanece disponible. Se acumula y se reparte de nuevo entre los días restantes; no desaparece al terminar el día.

Ejemplo planteado:

| Concepto | Importe |
|---|---:|
| Ingreso | 1.800 € |
| Gastos reservados | −950 € |
| Ahorro reservado | −250 € |
| Disponible para gastar | 600 € |

Si quedan 30 días, el presupuesto inicial es de 20 € al día. Si el primer día se gastan 12 €, al día siguiente quedan 588 € para 29 días: aproximadamente 20,28 € al día.

Si un día se gasta por encima de lo previsto, disminuye el margen de los días siguientes. La app debe explicar el efecto con claridad, por ejemplo: «Con este gasto, tendrás 18 € al día hasta cobrar».

Un gasto ya reservado, como el alquiler, no se debe descontar de nuevo del saco al marcarlo como pagado.

## Registro y consulta de gastos

- El usuario introduce los gastos que va realizando.
- El botón visible «− Sacar» permite registrar un gasto con importe y categoría en pocos pasos.
- El saldo se actualiza al registrar un gasto y el reparto se recalcula diariamente.
- Deben distinguirse el margen restante de hoy y la previsión para los días siguientes.
- Se muestra el total disponible, cuánto se puede gastar hoy y el margen semanal.

## Calendario

La app necesita un calendario interno para calcular fechas, fines de semana y duración de los ciclos.

Se ha propuesto también una vista de calendario con:

- Próximo cobro, con su fecha ajustada al lunes cuando corresponda.
- Gastos registrados por día y detalle al tocar una fecha.
- El día actual destacado.
- Resumen semanal de gasto y margen disponible.

El saco sería la vista principal y el calendario una vista secundaria. Junto al saco puede aparecer: «Quedan 12 días para cobrar · próximo ingreso: lunes 27».

## Representación visual: saco de gemas

### Decisión actual

Usar gemas variadas como representación visual del dinero, sin asignarles un valor individual en euros.

- Se puede tocar el saco para abrirlo y ver las gemas.
- Las gemas pueden tener distintos colores y formas, con tamaños parecidos.
- La variedad es decorativa: un tipo de gema no vale más que otro.
- No hay equivalencia del tipo «una gema = X euros».
- El importe exacto en euros aparece siempre visible.
- El nivel de llenado representa la proporción del presupuesto que queda respecto al inicio del ciclo.
- El saco empieza lleno con el presupuesto disponible del ciclo y se vacía proporcionalmente al gastar.
- La mezcla de colores se mantiene al vaciarse; no desaparece primero un tipo de gema.

Ejemplo: «Te quedan 360 € · 60 % de tu presupuesto», con el saco aproximadamente al 60 % de su contenido inicial.

El saco representa dinero disponible para gastar durante el ciclo. Su llenado es relativo al presupuesto inicial, no una medida de riqueza absoluta ni una comparación entre usuarios.

### Animaciones propuestas

Al registrar un gasto salen gemas y baja el nivel del saco. Los gastos pequeños producen un cambio pequeño y los grandes, uno más visible. Las animaciones acompañan al importe exacto, sin obligar a contar gemas.

### Alternativa descartada

Se valoraron monedas con denominaciones de oro (1.000 €), plata (100 €), bronce (10 €) y cobre (1 €).

Se descartó esa equivalencia porque 999 € se representarían con 27 monedas y 1.000 € con una sola: al aumentar el dinero, el saco parecería más vacío. Las gemas sin denominación evitan ese salto visual.

## Propuestas pendientes de confirmar


- **Sobrante al cerrar el ciclo:** pasarlo al ahorro o sumarlo al presupuesto del siguiente ciclo. Se propuso pasarlo al ahorro, pero no se ha elegido una opción.
- **Cofre de ahorro:** mostrar el ahorro acumulado en un cofre separado del saco diario. Se planteó como recurso visual; queda por confirmar.
- **Festivos:** por ahora solo está definida la regla de sábados y domingos. Falta decidir si se tendrán en cuenta festivos y de qué localidad.
- **Días 29, 30 y 31:** se propuso usar el último día del mes cuando el día elegido no exista. Falta confirmar esa regla y aplicar después el ajuste por fin de semana.
- **Comida y transporte:** decidir si salen del saco diario o si también se pueden reservar presupuestos propios.
- **Resumen semanal:** se propuso mostrar los próximos siete días, limitados por el próximo cobro. Falta confirmar si se prefiere ese periodo o una semana de calendario.
- **Primer uso a mitad de ciclo:** definir cómo introducir el dinero disponible y los pagos ya realizados para no contar un ingreso mensual completo que ya se ha gastado parcialmente.
- **Porcentaje de ahorro:** concretar su base de cálculo; se propuso calcularlo sobre el ingreso mensual.

## Plataforma y estilo visual

- App para Android, instalable mediante APK.
- Diseño sencillo, en 2D y muy visual.
- El saco, las gemas y sus animaciones se representan en 2D.
- Importes fáciles de leer, iconos reconocibles y pocas acciones por pantalla.
- La pantalla principal da protagonismo al saco, al disponible de hoy y al botón «− Sacar» para registrar un gasto.
- Animaciones breves para acompañar los cambios de saldo sin retrasar el registro de gastos.

Se ha propuesto una primera versión sin cuenta y con funcionamiento sin internet, guardando los datos en el móvil. El almacenamiento y la copia de seguridad están pendientes de concretar.

## Alcance actual

Se ha iniciado una primera implementación Android con Kotlin, Compose y Room. El flujo parte del registro manual de gastos. Consulta README.md para conocer el alcance implementado y las funciones pendientes.


## Arquitectura

Arquitectura de referencia. Ya existe un proyecto Android Studio; README.md describe sus versiones, alcance y limitaciones actuales.

### Enfoque

App nativa Android con Kotlin y Jetpack Compose. Primera versión propuesta: datos locales, registro manual y funcionamiento sin conexión. Cada saco corresponde a un banco configurado por el usuario; no existe conexión bancaria en este alcance.

### Herramientas

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

### Organización

Un módulo de aplicación inicial, con paquetes separados:

- `ui/`: selector de sacos, creación, detalle, registro de gastos, calendario y ajustes.
- `domain/`: reglas de presupuesto, ahorro, redondeo y fechas de cobro; Kotlin sin dependencias de interfaz.
- `data/`: Room, repositorios, preferencias y copias de seguridad.

Flujo: pantalla → ViewModel → reglas y repositorio → base de datos. Los cambios de datos actualizan la pantalla mediante Flow. Inyección manual de dependencias inicialmente.

### Datos principales

- Saco: identificador, nombre del banco, moneda, ingreso mensual, día habitual de cobro y configuración de ahorro.
- Gasto fijo: saco, nombre, icono e importe.
- Ciclo: saco, fechas efectivas, ingreso y reservas del periodo. Conserva una copia de las condiciones para no modificar el historial al editar la configuración.
- Reserva del ciclo: gasto fijo reservado e información de pago.
- Movimiento: saco, ciclo, importe, fecha y categoría o concepto.

Guardar dinero en céntimos con enteros de 64 bits. Usar aritmética decimal y una regla explícita de redondeo para porcentajes. El saldo se obtiene de los datos financieros; las gemas solo lo representan.

### Fechas y actualización

Cada saco tiene su ciclo. El sábado o domingo de cobro se desplaza al lunes. Al abrir o volver a la app se comprueba la fecha y se actualiza el ciclo; si permanece abierta al cambiar de día, también se actualiza. La creación de ciclos debe evitar duplicados y contemplar varios meses sin abrir la app.

No se necesita un proceso permanente en segundo plano para los cálculos. Si posteriormente se añaden recordatorios, se evaluarán las herramientas de notificaciones y planificación de Android.

Separar el presupuesto asignado al inicio del día, lo ya gastado hoy y la previsión para los días siguientes. Definir y probar esta regla antes de implementar para evitar repartir otra vez el saldo tras cada gasto y mostrar un margen diario engañoso.

### Interfaz

1. Entrada: sacos existentes y crear saco.
2. Configuración: ingreso, fecha, salidas e importe o porcentaje de ahorro.
3. Saco: fecha actual, disponible de hoy, total, gemas y botón «− Sacar».
4. Registrar gasto: importe y categoría o concepto.
5. Calendario e historial por saco.
6. Ajustes y copia de seguridad.

Dibujos vectoriales y Canvas con animaciones breves. Gemas decorativas sin equivalencia individual en euros. El número de elementos dibujados se limita para mantener fluidez. Mostrar importes legibles, etiquetas accesibles y opción de reducir animaciones.

### Verificación y entrega

- Pruebas de fechas: fines de semana, febrero, años bisiestos y cambio de año.
- Pruebas de dinero: redondeo, ahorro, sobrantes, exceso de gasto y reservas sin doble descuento.
- Pruebas de independencia entre sacos y actualización de ciclos sin duplicados.
- Comprobar persistencia tras cerrar la app y restauración de una copia.
- Probar creación de saco y registro de gasto en emulador y móvil.
- Generar APK de prueba y después APK de distribución firmado. Conservar la misma clave para actualizaciones.

El APK se puede instalar directamente. Publicar en Google Play es un paso independiente que requiere su cuenta y un paquete AAB para el flujo habitual de publicación.

### Pendientes antes de cerrar las reglas

Resolver los puntos funcionales pendientes en DEFINICION_APP.md, especialmente sobrantes entre ciclos, primer uso a mitad de mes y días de cobro inexistentes. Definir Android mínimo, comportamiento ante saldo negativo y formato de copia de seguridad.

### Fuentes oficiales

- Arquitectura: https://developer.android.com/topic/architecture/recommendations
- Compose: https://developer.android.com/develop/ui/compose/documentation
- Datos locales: https://developer.android.com/topic/architecture/data-layer/offline-first
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Compilación: https://developer.android.com/build/building-cmdline
- Firma: https://developer.android.com/studio/publish/app-signing

### Desarrollo desde Linux y WSL 2

Se propone desarrollar y compilar desde WSL 2 usando JDK, Android SDK para Linux, Gradle Wrapper y Git. Android Studio es útil como entorno gráfico, pero no es obligatorio para compilar desde terminal. Mantener el proyecto en el sistema de archivos Linux y no mezclar herramientas SDK de Windows y Linux durante la compilación.

Flujo recomendado:

1. Escribir el código y ejecutar pruebas unitarias en WSL 2.
2. Generar y firmar el APK desde WSL 2.
3. Probar la app en un teléfono Android real o en un emulador ejecutado en Windows.

Para conectar un teléfono se puede usar ADB desde Windows. Si se quiere acceso USB directo desde WSL 2, Microsoft documenta la conexión mediante usbipd-win, que requiere configuración adicional en Windows.

El emulador dentro de WSL no se da por garantizado: la aceleración depende de la virtualización y del entorno. La opción recomendada es ejecutar el emulador en Windows o usar un móvil. En Linux nativo, el emulador puede usar KVM si el equipo cumple los requisitos.

Fuentes:

- Compilar desde terminal: https://developer.android.com/build/building-cmdline
- Aceleración del emulador: https://developer.android.com/studio/run/emulator-acceleration
- USB en WSL 2: https://learn.microsoft.com/en-us/windows/wsl/connect-usb


### Dispositivo de pruebas acordado

El usuario conectará un móvil Android por USB para las pruebas. El flujo de trabajo será desarrollar y compilar en WSL 2 e instalar el APK en ese dispositivo. El emulador queda como opción secundaria.

Para preparar el móvil se activarán las opciones de desarrollador y la depuración USB, y se autorizará la conexión del ordenador en el teléfono. Se verificará la conexión mediante ADB antes de instalar la app. Queda por elegir si ADB se ejecuta en Windows o directamente en WSL 2 mediante usbipd-win.


### Lista de instalación para WSL 2 y móvil USB

Esta lista documenta la preparación necesaria; no significa que las herramientas ya estén instaladas o verificadas. Las versiones concretas de JDK, Gradle, Android Gradle Plugin y SDK se fijarán juntas al crear el proyecto.

#### Instalar en WSL 2

- Distribución Linux actualizada, preferiblemente Ubuntu o Debian, sobre WSL 2.
- Utilidades: `git`, `curl`, `unzip`, `zip` y `ca-certificates`.
- JDK completo compatible con Gradle y Android Gradle Plugin, con `java`, `javac` y `keytool`.
- Herramientas oficiales de gestión del Android SDK para Linux. La documentación actual recomienda Android CLI (`android sdk`); `sdkmanager` sigue documentado como herramienta anterior de Command-Line Tools.
- Paquetes del SDK: `platform-tools` (incluye ADB), `platforms;android-<API>` correspondiente al `compileSdk` y `build-tools;<version>` compatible con el proyecto. Build Tools incluye las herramientas de empaquetado y firma, como `apksigner` y `zipalign`.
- Para USB directo en WSL: reglas de permisos del dispositivo y soporte udev cuando sea necesario para la distribución y el teléfono.

En Ubuntu/Debian, las utilidades básicas se pueden instalar con:

```sh
sudo apt update
sudo apt install git curl unzip zip ca-certificates
```

El paquete de JDK y las versiones del SDK se concretarán tras revisar la distribución y la matriz de compatibilidad del proyecto.

#### Configurar en WSL 2

- `JAVA_HOME` apuntando al JDK elegido.
- `ANDROID_HOME` apuntando al SDK Linux.
- Añadir las herramientas Java, Android CLI o Command-Line Tools y `platform-tools` al `PATH`.
- Revisar y aceptar las licencias necesarias del SDK.
- Mantener el código en el sistema de archivos Linux, como la carpeta actual del proyecto.
- Contar con acceso a internet para descargar SDK, Gradle y dependencias durante la preparación y las compilaciones que necesiten nuevas dependencias. La app resultante funcionará sin conexión según el alcance propuesto.

#### Dependencias que descarga el proyecto

No necesitan instalación global por separado:

- Gradle: se ejecuta con el Wrapper versionado del proyecto (`./gradlew`).
- Android Gradle Plugin, Kotlin y KSP: se declaran en la configuración de compilación.
- Compose, Material 3, Navigation, Room, DataStore, coroutines, serialization y bibliotecas de pruebas: se declaran como dependencias.
- SQLite va integrado en Android y Room lo utiliza; no requiere instalar un servidor de base de datos.

#### Preparar Windows para el móvil

Elegir una de estas dos rutas:

1. **ADB en Windows:** instalar Platform Tools para Windows y, si el fabricante lo requiere, su controlador USB. Compilar en WSL e instalar el APK con ADB de Windows.
2. **ADB en WSL:** instalar `usbipd-win` en Windows, compartir y adjuntar el teléfono a WSL 2 y usar ADB del SDK Linux. El enlace inicial del dispositivo requiere permisos de administrador en Windows. Revisar los permisos USB dentro de Linux si ADB no puede acceder al móvil.

No es necesario configurar ambas rutas. Utilizar un cable USB que transmita datos.

#### Preparar el teléfono

- Activar opciones de desarrollador y depuración USB.
- Aceptar la autorización del ordenador.
- Verificar con `adb devices` que aparece como `device`, no como `unauthorized`.

#### Verificación una vez creado el proyecto

```sh
java -version
javac -version
adb version
adb devices
./gradlew --version
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ejecutar los comandos ADB en el entorno elegido. Si se usa ADB de Windows, proporcionar una ruta al APK accesible desde Windows.

Las pruebas de interfaz y base de datos que requieren Android se ejecutarán con el móvil conectado, mediante las tareas de pruebas instrumentadas del proyecto. Para distribución, configurar una clave de firma propia y generar el APK release; la compilación debug usa una clave de desarrollo.

#### Herramientas opcionales

Android Studio aporta editor y previsualizaciones. No hace falta instalar un emulador ni imágenes de Android para el flujo acordado con móvil físico. El alcance Kotlin/Compose actual tampoco requiere NDK, CMake, Node.js, Docker ni servicios en la nube.

Referencias de instalación:

- [Gestión del SDK](https://developer.android.com/tools/sdkmanager).
- [Compatibilidad de Java en compilaciones Android](https://developer.android.com/build/jdks).
- [Conexión USB a WSL 2](https://learn.microsoft.com/en-us/windows/wsl/connect-usb).


### Entorno elegido: Android Studio

El usuario ha elegido Android Studio y un móvil físico por USB. Abrir la raíz del proyecto con Open, sincronizar Gradle y ejecutar el módulo app. El flujo WSL anterior se conserva como alternativa. La instalación temporal usada para verificar desde WSL no forma parte del proyecto portable.
