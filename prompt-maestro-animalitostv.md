# PROMPT MAESTRO — AnimalitosTV

## Instrucción Principal

Desarrolla una aplicación Android nativa completa llamada **"AnimalitosTV"** que funcione como display de resultados de lotería de animalitos de Venezuela. La app está diseñada para ejecutarse en modo landscape (widescreen) en cualquier dispositivo Android (TV, TV Box, tablets) y funcionar como pantalla informativa permanente en agencias de lotería y comercios.

---

## 1. ESPECIFICACIONES TÉCNICAS GENERALES

- **Nombre de la app:** AnimalitosTV
- **Plataforma:** Android (cualquier dispositivo en modo landscape forzado)
- **Framework:** Kotlin nativo con Android Studio (Jetpack Compose para UI)
- **Resolución objetivo:** 1280x720 (HD), con diseño responsivo que escale correctamente
- **Orientación:** Landscape forzado (no permitir rotación a portrait)
- **Min SDK:** API 21 (Android 5.0)
- **Target SDK:** API 34
- **Conectividad:** Online con fallback offline (caché local de últimos datos)
- **Distribución:** APK directa (sin Google Play Store)
- **Idioma:** Español de Venezuela únicamente

---

## 2. ARQUITECTURA DE LA APLICACIÓN

### 2.1 Patrón de arquitectura
- **MVVM** (Model-View-ViewModel) con Jetpack Compose
- **Room Database** (SQLite con ORM) para almacenamiento local
- **WorkManager** para tareas de scraping periódico en background
- **Hilt** para inyección de dependencias
- **Retrofit + Jsoup** para scraping web

### 2.2 Estructura de paquetes
```
com.animalitostv/
├── data/
│   ├── local/          # Room DB, DAOs, Entities
│   ├── remote/         # Scrapers (Jsoup)
│   └── repository/     # Repositorios
├── di/                 # Módulos Hilt
├── domain/
│   ├── model/          # Modelos de dominio
│   └── usecase/        # Casos de uso
├── ui/
│   ├── main/           # Pantalla principal (display)
│   ├── config/         # Pantalla de configuración
│   ├── history/        # Historial de resultados
│   └── components/     # Componentes reutilizables
├── service/            # Servicios background
├── util/               # Utilidades
└── receiver/           # BroadcastReceivers (auto-inicio)
```

---

## 3. LAYOUT DE PANTALLA PRINCIPAL

La pantalla se divide en las siguientes zonas:

```
┌─────────────────────────────────────────────────────────────────┐
│ [HEADER: Logo/Nombre personalizable]          [Reloj: HH:MM:SS │
│                                                DD/MM/YYYY]      │
├──────────┬──────────────────────────────────────────────────────┤
│          │  GUACHARO | GUACHARITO | LOTTO   | LA      | LOTTO | SELVA  │
│  PANEL   │  ACTIVO   | MILLONARIO | ACTIVO  | GRANJITA| REY   | PLUS   │
│  PUBLI-  │──────────────────────────────────────────────────────┤
│  CITARIO │  8:00 AM  │  8:30 AM   │ 8:00 AM │ 8:00 AM │8:30 AM│ 9:00 AM│
│          │  9:00 AM  │  9:30 AM   │ 9:00 AM │ 9:00 AM │9:30 AM│10:00 AM│
│ (Portrait│  ...      │  ...       │ ...     │ ...     │ ...   │ ...    │
│  Videos/ │  7:00 PM  │  7:30 PM   │ 7:00 PM │ 7:00 PM │7:30 PM│ 7:00 PM│
│ Imágenes)│──────────────────────────────────────────────────────┤
│          │  [BANNER ESTADÍSTICAS - Ticker horizontal rotativo]  │
├──────────┴──────────────────────────────────────────────────────┤
│ [Botón oculto/discreto para acceder a Configuración]            │
└─────────────────────────────────────────────────────────────────┘
```

### 3.1 Header
- Logo personalizable (cargado desde `/sdcard/AnimalitosTV/logo.png`)
- Nombre de agencia/negocio personalizable (configurable en settings)
- Reloj digital con hora y fecha actual (formato 24h o 12h configurable)
- Zona horaria: Venezuela (UTC-4)

### 3.2 Panel Publicitario (Lateral Izquierdo)
- **Posición:** Lateral izquierdo, formato portrait (vertical)
- **Ancho:** Configurable por el usuario (rango: 15% a 35% del ancho total, default 25%)
- **Contenido:** Videos (MP4, AVI) e imágenes (JPG, PNG) mezclados
- **Carpeta de origen:** `/sdcard/AnimalitosAds/`
- **Rotación de imágenes:** Tiempo configurable (5s, 10s, 15s, 20s, 30s — default 10s)
- **Videos:** Reproducción completa antes de pasar al siguiente item
- **Audio en videos:** Configurable (mute/con audio — default mute)
- **Comportamiento:** Loop infinito automático de todo el contenido de la carpeta
- **Orden:** Alfabético por nombre de archivo
- **Hot-reload:** Detectar cambios en la carpeta y actualizar lista sin reiniciar

### 3.3 Grilla de Resultados (Zona Central/Derecha)
- **Columnas:** 5 columnas visibles (Lotto Rey oculto con `visible=false` en enum — reactivable)
- **Filas:** Variable según lotería (12 filas máximo)
- **Cada celda muestra:** Si existe `fondos/{numero}.png` → imagen de fondo a pantalla completa + hora en borde inferior; si no → imagen del animal (44dp) + número + nombre
- **Celdas vacías:** Mostrar "—" o indicador de "Pendiente" para sorteos no realizados aún
- **Animación de nuevo resultado:** Flash/highlight amarillo-dorado con fade-out de 4 segundos cuando llega un resultado nuevo
- **Auto-scroll:** Ciclo continuo de `primeraFila until ultimaFila` con `animateScrollBy` + `LinearEasing`, 3s por fila
- **Botón discreto:** "Ver días anteriores" para acceder a historial
- **Color de columnas:** Cada lotería con color distintivo según sus colores oficiales
- **Control de visibilidad:** `Loteria.todas` filtra por `visible=true`; `Loteria.todasIncluyendoOcultas` incluye todas

### 3.4 Banner de Estadísticas (Inferior)
- **Tipo:** Ticker horizontal rotativo (marquesina) que se desplaza continuamente
- **Contenido rotativo por lotería individual:**
  - Números fríos del día (no han salido hoy)
  - Números fríos de la semana
  - Números fríos del mes
  - Números calientes del día (más frecuentes hoy)
  - Números calientes de la semana
  - Números calientes del mes
  - Rachas: animal que lleva más sorteos consecutivos sin salir
  - Último sorteo en que salió cada animal
- **Formato del mensaje:** `"🔥 LOTTO ACTIVO — Calientes del día: 12-Caballo (3 veces), 05-León (2 veces) | ❄️ Fríos de la semana: 33-Pescado (0 veces), 17-Pavo (0 veces) | ..."`
- **Velocidad del ticker:** Configurable

---

## 4. LOTERÍAS Y HORARIOS

### 4.1 Guacharo Activo
- **Operadora:** Inversiones Unidas Plus
- **IOBPAS:** Lotería de Oriente
- **Animales:** 77 figuras (00-75)
- **Sorteos:** 12 diarios (lunes a domingo)
- **Horarios:** 8:00 AM, 9:00 AM, 10:00 AM, 11:00 AM, 12:00 PM, 1:00 PM, 2:00 PM, 3:00 PM, 4:00 PM, 5:00 PM, 6:00 PM, 7:00 PM
- **Color de columna:** #FF6B00 (naranja oscuro)

### 4.2 El Guacharito Millonario
- **IOBPAS:** Lotería de Oriente
- **Animales:** 101 figuras (00-100) ⚠️ CORREGIDO — originalmente documentado como 77, pero son 101
- **Sorteos:** 12 diarios (lunes a domingo)
- **Horarios:** 8:30 AM, 9:30 AM, 10:30 AM, 11:30 AM, 12:30 PM, 1:30 PM, 2:30 PM, 3:30 PM, 4:30 PM, 5:30 PM, 6:30 PM, 7:30 PM
- **Color de columna:** #FFD700 (dorado)

### 4.3 Lotto Activo
- **Operadora:** Juegos Activos C.A.
- **IOBPAS:** Lotería del Cojedes
- **Animales:** 38 figuras (00, 0-36)
- **Sorteos:** 12 diarios (lunes a domingo)
- **Horarios:** 8:00 AM, 9:00 AM, 10:00 AM, 11:00 AM, 12:00 PM, 1:00 PM, 2:00 PM, 3:00 PM, 4:00 PM, 5:00 PM, 6:00 PM, 7:00 PM
- **Color de columna:** #E53935 (rojo)

### 4.4 La Granjita
- **Operadora:** Global Sport 69 C.A.
- **IOBPAS:** Lotería de Margarita
- **Animales:** 38 figuras (00, 0-36)
- **Sorteos:** 12 diarios (lunes a domingo)
- **Horarios:** 8:00 AM, 9:00 AM, 10:00 AM, 11:00 AM, 12:00 PM, 1:00 PM, 2:00 PM, 3:00 PM, 4:00 PM, 5:00 PM, 6:00 PM, 7:00 PM
- **Color de columna:** #43A047 (verde)

### 4.5 Lotto Rey *(oculto actualmente — inhabilitado)*
- **Operadora:** Lotto Rey C.A.
- **IOBPAS:** Lotería de Margarita
- **Animales:** 38 figuras (00, 0-36)
- **Sorteos:** 12 diarios (lunes a domingo)
- **Horarios:** 8:30 AM, 9:30 AM, 10:30 AM, 11:30 AM, 12:30 PM, 1:30 PM, 2:30 PM, 3:30 PM, 4:30 PM, 5:30 PM, 6:30 PM, 7:30 PM
- **Color de columna:** #1E88E5 (azul)
- **Estado:** `visible = false` en enum — para reactivar, cambiar a `visible = true` en `Loteria.kt`

### 4.6 Selva Plus
- **Operadora:** Inversiones Unidas Plus C.A.
- **IOBPAS:** Lotería de Oriente
- **Animales:** 38 figuras (00, 0-36)
- **Sorteos:** 12 diarios (lunes a domingo)
- **Horarios:** 8:00 AM, 9:00 AM, 10:00 AM, 11:00 AM, 12:00 PM, 1:00 PM, 2:00 PM, 3:00 PM, 4:00 PM, 5:00 PM, 6:00 PM, 7:00 PM
- **Color de columna:** #6D4C41 (marrón selva)

---

## 5. LISTADO COMPLETO DE ANIMALES

### 5.1 Animales estándar (38 figuras — Lotto Activo, La Granjita, Lotto Rey, Selva Plus)
```
00 - Delfín (o Ballena según lotería)
0  - Ballena (o Delfín)
1  - Carnero
2  - Toro
3  - Ciempiés
4  - Alacrán
5  - León
6  - Rana
7  - Perico
8  - Ratón
9  - Águila
10 - Tigre
11 - Gato
12 - Caballo
13 - Mono
14 - Paloma
15 - Zorro
16 - Oso
17 - Pavo
18 - Burro
19 - Chivo
20 - Cochino
21 - Gallo
22 - Camello
23 - Cebra
24 - Iguana
25 - Gallina
26 - Vaca
27 - Perro
28 - Zamuro
29 - Elefante
30 - Caimán
31 - Lapa
32 - Ardilla
33 - Pescado
34 - Venado
35 - Jirafa
36 - Culebra
37 - Tortuga
```

### 5.2 Animales extendidos (77 figuras — Guacharo Activo, Guacharito Millonario)
Incluye todos los 38 anteriores más:
```
38 - Búfalo
39 - Lechuza
40 - Avispa
41 - Canguro
42 - Tucán
43 - Mariposa
44 - Chigüire
45 - Garza
46 - Puma
47 - Pavo Real
48 - Puercoespín
49 - Pereza
50 - Canario
51 - Pelícano
52 - Pulpo
53 - Caracol
54 - Grillo
55 - Oso Hormiguero
56 - Tiburón
57 - Pato
58 - Hormiga
59 - Pantera
60 - Camaleón
61 - Panda
62 - Cachicamo
63 - Cangrejo
64 - Gavilán
65 - Araña
66 - Lobo
67 - Avestruz
68 - Jaguar
69 - Conejo
70 - Bisonte
71 - Guacamaya
72 - Gorila
73 - Hipopótamo
74 - Turpial
75 - Guácharo
```

### 5.3 Imágenes de animales
- Incluir set inicial de imágenes placeholder (iconos simples en formato PNG)
- Las imágenes deben ser **descargables y actualizables** desde una carpeta: `/sdcard/AnimalitosTV/animales/`
- Formato de nombre: `{numero}.png` (ej: `00.png`, `0.png`, `1.png`, ..., `75.png`)
- Si existe imagen personalizada en la carpeta, usar esa; si no, usar la embebida por defecto
- Tamaño recomendado de imagen: 64x64 px

---

## 6. SISTEMA DE WEB SCRAPING

### 6.1 Fuentes de datos (todas en paralelo)
1. `https://www.tuazar.com/loteria/animalitos/resultados/`
2. `https://lotoven.com/animalitos/`
3. `https://loteriadehoy.com/animalitos/resultados/` ← única fuente que cubre Lotto Rey

### 6.2 Lógica de scraping ⚠️ ACTUALIZADO
- **Frecuencia:** Al iniciar la app + cada 5 minutos (loop en MainViewModel) + cada 15 minutos via WorkManager
- **Arquitectura: PARALELO** (no cascada) — las 3 fuentes se consultan simultáneamente con coroutines
- **Fusión de resultados:** Se combinan eliminando duplicados por `(loteria, hora)` — gana el primero en llegar
- **Motivo del cambio:** La cascada dejaba huecos cuando una fuente cubría loterías que otra no tenía
- **Parser:** Jsoup para parsear HTML estático de cada fuente
- **Validación:** `numero in 0..loteria.maxAnimal` (cada lotería tiene su propio máximo)
- **Timeout:** 30 segundos por request (OkHttpClient)

### 6.3 Selectores HTML verificados en producción

**TuAzarScraper:**
```
div.resultados → h2.lotResTit (nombre lotería)
              → div.col-xs-6.col-sm-3 → div.horario (hora)
                                      → span (resultado: "31 - LAPA" o "- -" si pendiente)
```

**LotoVenScraper:**
```
h3 (nombre lotería "Resultados Lotto Activo")
→ texto del bloque siguiente: "34 Venado 08:00 AM"
→ regex: (\d{1,2})\s+[nombre]\s+(\d{1,2}:\d{2}\s*[AP]M)
```

**LoteriaDeHoyScraper:**
```
div.lottoactivo / div.lagranjita / div.lottorey / div.selvaplus ...
  → h3 (nombre)
  → h4[i] (número + animal: "34 Venado")
  → h5[i] (hora: "08:00 AM")   ← mismo índice que h4
```

### 6.4 User-Agent
```
Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
```

### 6.5 Fix crítico: normalización de hora ⚠️
El método `normalizarHora()` debe **preservar el AM/PM del texto original**.
Un bug previo recalculaba AM/PM solo por el número de la hora, convirtiendo "1:00 PM" → "1:00 AM" (ya que 1 < 12), lo que causaba que todos los resultados de tarde no coincidieran con los horarios.

```kotlin
// CORRECTO: preservar AM/PM si está presente en el texto
val regexAmPm = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""")
// Solo convertir si es formato 24h sin AM/PM
```

### 6.6 Manejo de errores de scraping
- Log al iniciar scraping, al obtener resultados, y cuando una fuente devuelve 0
- Formato del log: `"OK: 51 resultados totales (tuazar=23, lotoven=23, loteriadehoy=5)"`
- Si todas las fuentes fallan → mostrar último dato en caché con indicador visual
- No sobreescribir datos válidos (índice UNIQUE en Room con `OnConflictStrategy.IGNORE`)

---

## 7. BASE DE DATOS (Room)

### 7.1 Entidades principales

```kotlin
@Entity(tableName = "resultados")
data class Resultado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loteria: String,         // "GUACHARO", "GUACHARITO", "LOTTO_ACTIVO", etc.
    val fecha: LocalDate,
    val hora: String,            // "8:00 AM", "9:30 AM", etc.
    val numeroAnimal: Int,       // 0-75
    val nombreAnimal: String,    // "TIGRE", "CABALLO", etc.
    val fuenteDatos: String,     // "tuazar", "lotoven", "loteriadehoy"
    val fechaRegistro: LocalDateTime,
    val esNuevo: Boolean = true  // Para animación de flash
)

@Entity(tableName = "configuracion")
data class Configuracion(
    @PrimaryKey val clave: String,
    val valor: String
)

@Entity(tableName = "logs_error")
data class LogError(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: LocalDateTime,
    val tipo: String,            // "SCRAPING", "CONEXION", "PARSE", "DB"
    val mensaje: String,
    val fuente: String?,
    val stackTrace: String?
)
```

### 7.2 Retención de datos
- **Resultados:** Mantener datos del último mes (31 días)
- **Limpieza automática:** Ejecutar limpieza diaria a las 2:00 AM (eliminar registros > 31 días)
- **Logs:** Mantener logs de los últimos 7 días

### 7.3 DAOs requeridos
- `ResultadoDao`: CRUD, consultas por fecha/lotería/hora, estadísticas agregadas
- `ConfiguracionDao`: Lectura/escritura de configuraciones
- `LogErrorDao`: Inserción y consulta de logs

---

## 8. ESTADÍSTICAS MOTIVACIONALES

### 8.1 Cálculos por lotería individual

**Números Fríos (no han salido):**
- Del día: Animales que no han aparecido en ningún sorteo del día actual
- De la semana: Animales con 0 apariciones en los últimos 7 días
- Del mes: Animales con 0 apariciones en los últimos 30 días

**Números Calientes (más frecuentes):**
- Del día: Top 5 animales más repetidos hoy
- De la semana: Top 5 animales más repetidos en 7 días
- Del mes: Top 5 animales más repetidos en 30 días

**Rachas:**
- Animal con más sorteos consecutivos sin salir (por lotería)
- Cantidad de sorteos desde su última aparición

**Formato del ticker:**
```
🔥 LOTTO ACTIVO — Calientes hoy: 12-Caballo (×3), 05-León (×2) | 
❄️ Fríos semana: 33-Pescado, 17-Pavo (sin salir) | 
⏳ Racha: 24-Iguana lleva 45 sorteos sin salir | 
🔥 LA GRANJITA — Calientes hoy: ...
```

---

## 9. PANTALLA DE CONFIGURACIÓN

### 9.1 Acceso
- Pantalla separada que aparece al iniciar la app por primera vez
- Acceso posterior: botón discreto en esquina inferior (o doble tap en el logo)
- **Protegida con PIN** (4-6 dígitos, configurable)
- PIN por defecto: `1234` (obligar cambio en primer uso)

### 9.2 Opciones configurables

**Branding:**
- Nombre de la agencia/negocio (texto libre)
- Logo (ruta a imagen en `/sdcard/AnimalitosTV/logo.png`)

**Panel publicitario:**
- Ancho del panel (slider: 15% - 35%)
- Tiempo de rotación de imágenes (5s, 10s, 15s, 20s, 30s)
- Audio en videos (on/off)
- Carpeta de publicidad (default: `/sdcard/AnimalitosAds/`)

**Display:**
- Formato de reloj (12h / 24h)
- Velocidad del ticker de estadísticas (lento, normal, rápido)

**Sistema:**
- Auto-inicio al encender dispositivo (on/off — default off)
- PIN de configuración (cambiar)
- Mantener pantalla encendida (on/off — default on)
- Ver logs de errores
- Limpiar base de datos manualmente
- Forzar actualización de resultados (scraping manual)

---

## 10. FUNCIONALIDADES ADICIONALES

### 10.1 Modo offline (fallback)
- Al perder conexión, mostrar los últimos resultados almacenados en Room
- Indicador visual: ícono de "sin conexión" discreto en el header
- Texto: "Última actualización: DD/MM HH:MM"
- Seguir intentando reconexión cada 15 minutos

### 10.2 Auto-inicio
- BroadcastReceiver para `BOOT_COMPLETED`
- Configurable desde pantalla de settings
- La app debe arrancar directamente en la pantalla principal (no en config)

### 10.3 Pantalla siempre encendida
- Usar `FLAG_KEEP_SCREEN_ON` cuando esté habilitado
- Configurable

### 10.4 Historial de resultados
- Accesible desde botón discreto en la grilla principal
- Vista por fecha (selector de fecha)
- Misma disposición de grilla que la pantalla principal
- Navegación: ← Día anterior | Hoy | Día siguiente →

### 10.5 Sistema de logs
- Registrar errores de scraping y conexión
- Registrar fecha/hora, tipo de error, fuente afectada y mensaje
- Retención: 7 días
- Visualizable desde pantalla de configuración (lista scrolleable)

---

## 11. TEMA VISUAL

### 11.1 Estilo general
- **Tema:** Vibrante tipo casino/lotería
- **Fondo principal:** Degradado oscuro (#1A1A2E → #16213E)
- **Fondo de celdas:** Semi-transparente con bordes luminosos
- **Texto principal:** Blanco (#FFFFFF)
- **Texto secundario:** Dorado (#FFD700)
- **Acentos:** Neón suave (bordes de columnas con glow effect sutil)
- **Header:** Fondo degradado dorado-oscuro
- **Fuente:** Sans-serif bold para números, regular para nombres

### 11.2 Colores de columnas
Cada columna de lotería tiene su color de header y bordes sutiles:
- Guacharo Activo: #FF6B00 (naranja)
- Guacharito Millonario: #FFD700 (dorado)
- Lotto Activo: #E53935 (rojo)
- La Granjita: #43A047 (verde)
- Lotto Rey: #1E88E5 (azul)
- Selva Plus: #6D4C41 (marrón)

### 11.3 Animación de resultado nuevo
- Flash dorado brillante (#FFD700) en la celda
- Duración: 3 segundos
- Transición: Flash → Fade out gradual al color normal de la celda

### 11.4 Ticker de estadísticas
- Fondo: Banda oscura semi-transparente
- Texto: Blanco con emojis de color
- Velocidad de desplazamiento: Configurable
- Separador entre secciones: " | "

---

## 12. PERMISOS ANDROID REQUERIDOS

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

## 13. DEPENDENCIAS GRADLE

```groovy
// Jetpack Compose
implementation "androidx.compose.ui:ui:1.6.+"
implementation "androidx.compose.material3:material3:1.2.+"
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.+"
implementation "androidx.navigation:navigation-compose:2.7.+"

// Room Database
implementation "androidx.room:room-runtime:2.6.+"
implementation "androidx.room:room-ktx:2.6.+"
kapt "androidx.room:room-compiler:2.6.+"

// WorkManager
implementation "androidx.work:work-runtime-ktx:2.9.+"

// Networking & Scraping
implementation "com.squareup.retrofit2:retrofit:2.9.+"
implementation "org.jsoup:jsoup:1.17.+"
implementation "com.squareup.okhttp3:okhttp:4.12.+"

// Hilt DI
implementation "com.google.dagger:hilt-android:2.50"
kapt "com.google.dagger:hilt-compiler:2.50"
implementation "androidx.hilt:hilt-work:1.2.+"

// Media playback
implementation "androidx.media3:media3-exoplayer:1.2.+"
implementation "androidx.media3:media3-ui:1.2.+"

// Image loading
implementation "io.coil-kt:coil-compose:2.5.+"

// Date/Time
implementation "org.jetbrains.kotlinx:kotlinx-datetime:0.5.+"
```

---

## 14. ESTRUCTURA DE CARPETAS EN DISPOSITIVO

```
/sdcard/AnimalitosTV/
├── logo.png                    # Logo personalizado de la agencia
├── animales/                   # Imágenes personalizadas de animales
│   ├── 00.png                  # Delfín/Ballena
│   ├── 0.png
│   ├── 1.png
│   ├── ...
│   └── 75.png                  # Guácharo
└── logs/                       # Exportación de logs (opcional)

/sdcard/AnimalitosAds/
├── publicidad1.jpg
├── publicidad2.mp4
├── publicidad3.png
└── ...
```

---

## 15. NOTAS IMPORTANTES PARA EL DESARROLLO

1. **Robustez del scraping:** Las páginas web pueden cambiar su estructura HTML sin aviso. Implementar parsers resilientes con manejo de excepciones granular. Si un campo no se encuentra, loguear el error pero no crashear.

2. **Zona horaria:** Toda la app opera en hora de Venezuela (UTC-4 / America/Caracas). Los sorteos son de lunes a domingo.

3. **Rendimiento:** La app debe funcionar de forma fluida como display permanente. Evitar memory leaks, especialmente en la reproducción de video y el ticker animado. Usar LaunchedEffect y DisposableEffect correctamente.

4. **Primer inicio:** En el primer inicio, mostrar la pantalla de configuración para que el usuario establezca su PIN, nombre de agencia y preferencias básicas. Los siguientes inicios van directamente al display principal.

5. **Manejo de almacenamiento:** Solicitar permisos de lectura de almacenamiento al inicio. Si no se conceden, el panel publicitario muestra un mensaje genérico y las imágenes de animales usan los placeholders embebidos.

6. **Watchdog de reproducción:** Si el reproductor de video se queda colgado en un archivo corrupto, implementar timeout de 60 segundos y saltar al siguiente archivo.

7. **Base de datos pre-poblada:** Al instalar, la BD debe estar vacía. Los datos se acumulan conforme el scraping trae resultados. No incluir datos de ejemplo.

8. **Compatibilidad TV Box:** Muchos TV Box en Venezuela usan Android 7-10 con hardware limitado. Optimizar para bajo consumo de RAM y CPU. Evitar animaciones excesivamente complejas.

9. **WorkManager en emuladores:** En BlueStacks y algunos TV Box, el WorkManager periódico no se ejecuta confiablemente. Implementar un loop de auto-refresh en el ViewModel como respaldo (cada 5 minutos con coroutine).

10. **Configuración con recarga automática:** Al volver de la pantalla de Config al MainScreen, recargar la configuración usando `repeatOnLifecycle(RESUMED)` para que cambios como el nombre de agencia se reflejen inmediatamente.

---

## 16. APRENDIZAJES DE PRODUCCIÓN *(añadido tras implementación real)*

Estos puntos surgieron durante el desarrollo y prueba real de la app:

### Correcciones al diseño original

| Aspecto | Especificación original | Realidad verificada |
|---------|------------------------|---------------------|
| Guacharito Millonario animales | 77 figuras (00-75) | **101 figuras (00-100)** |
| Guacharo Activo animales | 77 figuras (00-75) | 76 figuras (00-75) — correcto |
| Arquitectura scraping | Cascada (falla → siguiente) | **Paralelo + fusión** (mayor cobertura) |
| Retry con delays | 3 intentos × 3 fuentes = hasta 3 min | Sin delays — respuesta inmediata |
| WorkManager como único timer | Suficiente | Agregar loop en ViewModel como respaldo |

### Selectores HTML que NO funcionan (descartados)
- `div[class*=loteria]` — tuazar.com no usa esa clase
- `div[class*=card]` — lotoven.com no usa cards
- Regex genérica sobre texto completo — demasiado ruido, falsos positivos

### Selectores HTML que SÍ funcionan (verificados en producción)
- **tuazar.com:** `div.resultados > h2.lotResTit` + `div.col-xs-6.col-sm-3 > div.horario` + `span`
- **lotoven.com:** `h3` con nombre + regex en bloque siguiente
- **loteriadehoy.com:** `div.{clase-loteria} > h4[i]` (número+animal) + `h5[i]` (hora)

### Cobertura real de fuentes verificada
- **tuazar.com:** Cubre las 6 loterías — 23 resultados (hasta hora del scraping)
- **lotoven.com:** Cubre las 6 loterías — 23 resultados (coincide con tuazar)
- **loteriadehoy.com:** Cubre Lotto Rey (única fuente) — resultados limitados

### Bug crítico resuelto: normalización de hora
El método `normalizarHora()` recalculaba AM/PM desde cero usando solo el número de la hora.
Esto convertía "1:00 PM" → "1:00 AM" (1 < 12), causando que TODOS los resultados de tarde
(1PM, 2PM, 3PM, 4PM, 5PM, 6PM, 7PM) fueran ignorados por no coincidir con los horarios.
**Solución:** Preservar el AM/PM explícito del texto fuente; solo deducirlo si no está presente.

---

## 17. SERVIDOR WEB LOCAL (NanoHTTPD) *(añadido en v1.3)*

La app expone un servidor HTTP en el **puerto 8080** accesible desde cualquier navegador en la misma red WiFi.

### Rutas
| Método | URI | Función |
|--------|-----|---------|
| GET | `/` | Página principal — tab Publicidad |
| GET | `/logo` | Tab Logo del negocio |
| GET | `/fondos` | Tab Fondos de animales |
| POST | `/upload/ads` | Subir archivos a `/sdcard/AnimalitosAds/` |
| POST | `/upload/logo` | Subir logo → `/sdcard/AnimalitosTV/logo.png` |
| POST | `/upload/fondos` | Subir fondos → `/sdcard/AnimalitosTV/fondos/` |
| GET | `/delete/ads?file=X` | Eliminar archivo de publicidad |
| GET | `/delete/fondos?file=X` | Eliminar fondo de animal |

### Clase: `service/AdHttpServer.kt`
- Extiende `NanoHTTPD(8080)`
- Inicia en `MainActivity.onCreate()`, se detiene en `onDestroy()`
- Validación anti path-traversal en delete (`.canonicalPath.startsWith`)
- Extensiones permitidas: imágenes (`jpg, jpeg, png, webp, bmp`) y video (`mp4, avi, mkv, mov, webm`)

### Permisos requeridos para escritura
- Android ≤ 10: `WRITE_EXTERNAL_STORAGE` + `requestLegacyExternalStorage="true"`
- Android 11+: `MANAGE_EXTERNAL_STORAGE` — se solicita automáticamente al primer inicio

### AdPanel: hot-reload
`remember(carpeta, reloadTick)` con tick cada 10 segundos detecta archivos nuevos sin reiniciar la app.

---

## 18. ENTREGABLES ESPERADOS

1. Proyecto Android Studio completo y compilable ✅
2. APK debug lista para instalar ✅
3. README con instrucciones de instalación, configuración y troubleshooting ✅
4. Documentación de parsers con selectores verificados en producción ✅
5. Servidor web local para gestión de contenido ✅
6. Set de imágenes placeholder para animales (pendiente — usar nombres de archivo como fallback)
