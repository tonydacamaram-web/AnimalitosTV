# AnimalitosTV

Display permanente de resultados de lotería de animalitos de Venezuela para Android (TV, TV Box, tablets).

---

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior — seleccionar JVM 17 al abrir el proyecto
- JDK 17 (no JVM 21 — incompatible con Gradle 8.4)
- Android SDK: API 21 mínimo, API 34 target
- Dispositivo/emulador Android en modo landscape

---

## Compilación

En Android Studio:
1. **File → Open** → seleccionar carpeta `AnimalitosTV`
2. Clic en **"Use JVM 17"** si aparece el diálogo
3. Esperar que termine el Gradle sync
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**

El APK queda en:
```
app\build\outputs\apk\debug\app-debug.apk
```

---

## Instalación en el dispositivo

```bash
# Via ADB
adb install -r app-debug.apk
```

O copiar el APK al dispositivo y abrir desde el gestor de archivos.
Activar **"Instalar desde fuentes desconocidas"** en Ajustes → Seguridad.

---

## Configuración de carpetas en el dispositivo

```
/sdcard/AnimalitosTV/
├── logo.png              ← Logo de la agencia
├── animales/             ← Imágenes de animales para la grilla (opcional)
│   └── 0.png ... 100.png ← Formato: {numero}.png, 64x64 px
└── fondos/               ← Fondos por animal para las celdas de resultado
    └── 0.png ... 100.png ← Formato: {numero}.png, 360x120 px (ratio 3:1)

/sdcard/AnimalitosAds/
├── publicidad1.jpg       ← Imágenes y videos para el panel lateral
├── publicidad2.mp4
└── ...
```

> **Todo el contenido se puede gestionar desde el navegador** conectado a la misma red WiFi — ver sección Panel de administración web.

---

## Primer uso

1. Iniciar la app → aparece pantalla de configuración
2. PIN por defecto: `1234`
3. Cambiar nombre de agencia y PIN → **Guardar**
4. La app pasa al display principal automáticamente

**Acceso posterior:** ícono de engranaje en esquina superior derecha del header.

---

## Funcionalidades

### Pantalla principal
- Grilla de **5 loterías** × 12 sorteos en modo landscape (Lotto Rey oculto temporalmente)
- Reloj digital en hora Venezuela (UTC-4), formato 12h/24h configurable
- Panel publicitario lateral izquierdo (imágenes + videos, ancho configurable)
- Ticker de estadísticas en la parte inferior (calientes, fríos, rachas)
- Indicador de última actualización cuando no hay conexión
- Actualización automática de resultados cada 5 minutos (sin depender del WorkManager)
- **Auto-scroll** de filas cada 3 segundos con transición LinearEasing suave
- **Flash dorado 4 segundos** al llegar un resultado nuevo
- **Fondos por animal** en celdas: imagen de fondo según el animal ganador (`/sdcard/AnimalitosTV/fondos/{numero}.png`)

### Panel de administración web
- Servidor HTTP en puerto 8080 (NanoHTTPD), accesible desde cualquier navegador en la misma red WiFi
- URL visible en Configuración → sección "Gestión de Publicidad"
- **Tab Publicidad:** subir/eliminar imágenes y videos de `/sdcard/AnimalitosAds/`
- **Tab Logo:** subir logo del negocio (se guarda como `logo.png` automáticamente)
- **Tab Fondos animales:** subir hasta 101 fondos PNG (nombrar como `0.png` … `100.png`)
- El panel publicitario recarga automáticamente cada 10 segundos los archivos nuevos

### Pantalla de configuración (PIN requerido)
- Branding: nombre de agencia
- Panel publicitario: ancho, rotación, audio, carpeta
- Display: reloj 24h, velocidad del ticker
- Sistema: auto-inicio, pantalla encendida
- **Gestión de Publicidad:** muestra URL `http://IP:8080` del servidor web
- **Botón "Forzar actualización ahora"** — ejecuta scraping inmediato
- Visor de logs de errores con timestamp y fuente

### Historial
- Navegación por fecha con ← / Hoy / →
- Misma disposición de grilla que la pantalla principal

---

## Catálogo de animales

| Lotería | Figuras | Rango |
|---------|---------|-------|
| Guacharo Activo | 76 | 00–75 |
| Guacharito Millonario | 101 | 00–100 |
| Lotto Activo | 38 | 00–37 |
| La Granjita | 38 | 00–37 |
| Lotto Rey | 38 | 00–37 |
| Selva Plus | 38 | 00–37 |

---

## Sistema de Scraping

### Arquitectura: Paralelo + Combinación

Las 3 fuentes se consultan **simultáneamente** en paralelo. Los resultados se fusionan eliminando duplicados (gana el primero en llegar para cada lotería+hora). Esto maximiza la cobertura — si una fuente no tiene Lotto Rey, otra puede tenerlo.

```
TuAzarScraper ──┐
LotoVenScraper ─┼─→ ScrapingOrchestrator → combinar → Room DB
LoteriaDeHoyScraper ─┘
```

### Fuentes y selectores HTML confirmados

#### TuAzarScraper — tuazar.com
**URL:** `https://www.tuazar.com/loteria/animalitos/resultados/`

**Estructura HTML verificada:**
```html
<div class="resultados">
  <h2 class="lotResTit">Guacharo Activo</h2>
  <div class="col-xs-6 col-sm-3">
    <div class="horario">8:00 AM</div>
    <span>31 - LAPA</span>
  </div>
  ...
</div>
```

**Selectores Jsoup:**
- Bloque por lotería: `div.resultados`
- Nombre: `h2.lotResTit`
- Sorteo: `div.col-xs-6.col-sm-3`
- Hora: `div.horario`
- Resultado: primer `span` → `"31 - LAPA"` (pendiente = `"- -"`)

**Si cambia la estructura:** Abrir tuazar.com en browser → F12 → inspeccionar y actualizar selectores en `TuAzarScraper.kt`.

---

#### LotoVenScraper — lotoven.com
**URL:** `https://lotoven.com/animalitos/`

**Estructura HTML verificada:**
```
h3 → "Resultados Lotto Activo"
(texto por sorteo) → "34 Venado 08:00 AM"
```

**Estrategia:** Encontrar `h3` con nombre de lotería → recolectar texto del bloque siguiente → regex `(\d{1,2})\s+[nombre]\s+(\d{1,2}:\d{2}\s*[AP]M)`.

**Si cambia:** Actualizar `parsear()` en `LotoVenScraper.kt`.

---

#### LoteriaDeHoyScraper — loteriadehoy.com
**URL:** `https://loteriadehoy.com/animalitos/resultados/`

**Estructura HTML verificada:**
```html
<div class="lottoactivo">
  <h3>Lotto Activo</h3>
  <h4>34 Venado</h4>   ← número + nombre
  <h5>08:00 AM</h5>    ← hora (mismo índice)
</div>
<div class="lottorey"> ... </div>
```

**Selectores Jsoup:**
- Bloque: `div.lottoactivo`, `div.lagranjita`, `div.lottorey`, `div.selvaplus`, etc.
- Nombre: `h3`
- Animal: `h4` (texto: `"34 Venado"`)
- Hora: `h5` (mismo índice que h4)

**Nota:** Lotto Rey aparece en esta fuente. Si no muestra resultados, el sitio puede tener carga dinámica JS para esa sección.

**Si cambia:** Actualizar clases en `clasesLoterias` list y selectores en `LoteriaDeHoyScraper.kt`.

---

### Validación de resultados (`ScraperUtils`)

- `esNumeroValido(numero, loteria)`: verifica `numero in 0..loteria.maxAnimal`
- `normalizarHora(texto)`: preserva AM/PM del texto original; convierte 24h si no hay AM/PM explícito
- `identificarLoteria(texto)`: mapea texto libre a enum `Loteria` (normaliza acentos)
- `buildResultado(...)`: construye `ResultadoSorteo` usando `nombreAnimalPorLoteria()` con el catálogo correcto por lotería

---

### Frecuencia de actualización

- **Al iniciar la app:** scraping inmediato
- **Automático:** cada 5 minutos desde `MainViewModel` (loop coroutine)
- **WorkManager:** cada 15 minutos como respaldo (útil cuando la app está en background)
- **Manual:** botón "Forzar actualización ahora" en Configuración

---

## Permisos requeridos

| Permiso | Uso |
|---------|-----|
| `INTERNET` | Scraping web + servidor HTTP local |
| `ACCESS_NETWORK_STATE` | Detectar conexión/desconexión |
| `READ_EXTERNAL_STORAGE` | Publicidad y logos (API ≤ 32) |
| `READ_MEDIA_IMAGES` | Imágenes publicitarias (API 33+) |
| `READ_MEDIA_VIDEO` | Videos publicitarios (API 33+) |
| `WRITE_EXTERNAL_STORAGE` | Guardar archivos subidos via HTTP (API ≤ 28) |
| `MANAGE_EXTERNAL_STORAGE` | Guardar archivos subidos via HTTP (API 29+) |
| `RECEIVE_BOOT_COMPLETED` | Auto-inicio al encender |
| `WAKE_LOCK` | Mantener pantalla encendida |
| `FOREGROUND_SERVICE` | WorkManager |

> **Android 11+:** Al primer inicio la app solicita el permiso "Acceso a todos los archivos" — necesario para que el servidor web pueda guardar los archivos subidos.

---

## Optimización para TV Box

- App funciona en Android 5.0+ (API 21)
- ExoPlayer con watchdog de 60s para videos corruptos
- Room con índice único `(loteria, fecha, hora)` — inserta sin duplicados (`IGNORE`)
- Ticker con `LinearEasing` — bajo costo de CPU
- Scraping paralelo — las 3 fuentes corren simultáneamente en coroutines separadas

---

## Troubleshooting

**"Sin resultados" en la grilla:**
1. Ir a Config → "Forzar actualización ahora"
2. Revisar Logs de errores — deben mostrar `OK: X resultados totales (tuazar=X, lotoven=X, loteriadehoy=X)`
3. Si todas las fuentes devuelven 0 → verificar conexión a internet en el dispositivo

**"HTML recibido pero 0 resultados parseados":**
- La estructura HTML del sitio cambió → revisar selectores en el scraper correspondiente

**Resultados de tarde (1PM–7PM) no aparecen:**
- Verificar que el sitio ya publicó esos resultados (algunos sitios tienen retraso)
- El fix de `normalizarHora` asegura que `"1:00 PM"` no se confunda con `"1:00 AM"`

**Lotto Rey vacío:**
- Única fuente que lo cubre: `loteriadehoy.com`
- Si aparece como `loteriadehoy=0` en los logs, el sitio no tiene datos o usa JS dinámico para esa sección

**Configuración no se actualiza en pantalla principal:**
- Al volver de Config, `MainScreen` recarga la configuración automáticamente via `repeatOnLifecycle(RESUMED)`

---

## Estructura del proyecto

```
app/src/main/java/com/animalitostv/
├── AnimalitosApp.kt          ← Application + Hilt + WorkManager config
├── MainActivity.kt           ← Entrypoint, pantalla completa, workers, monitor red
├── data/
│   ├── local/                ← Room DB, DAOs, Entities, TypeConverters
│   ├── remote/               ← TuAzarScraper, LotoVenScraper, LoteriaDeHoyScraper
│   │                            ScrapingOrchestrator (paralelo), ScraperUtils
│   └── repository/           ← ResultadoRepository, ConfiguracionRepository, LogRepository
├── di/                       ← DatabaseModule, NetworkModule (OkHttpClient + User-Agent)
├── domain/
│   ├── model/                ← Animal (catálogos: estándar/extendido/guacharito)
│   │                            Loteria (enum con maxAnimal), ResultadoSorteo, EstadisticasLoteria
│   └── usecase/              ← GetResultadosHoyUseCase, GetEstadisticasUseCase
├── receiver/                 ← BootReceiver (auto-inicio configurable)
├── service/                  ← ScrapingWorker (15min), CleanupWorker (diario 2AM)
├── ui/
│   ├── components/           ← DigitalClock, ResultadoCell (flash dorado), AdPanel, StatsTicker
│   ├── config/               ← ConfigScreen (PIN + opciones + logs + forzar actualización)
│   ├── history/              ← HistoryScreen (navegación por fecha)
│   ├── main/                 ← MainScreen (grilla + header + ticker), MainViewModel (auto-refresh 5min)
│   ├── navigation/           ← NavGraph (Main / Config / History)
│   └── theme/                ← Color, Type, Theme (estilo casino oscuro)
└── util/                     ← Constants (rutas, claves config, zona horaria VE)
```

---

## Changelog

### v1.3 (actual)
- **Lotto Rey oculto** — campo `visible=false` en el enum; fácilmente reactivable
- **Servidor web local (NanoHTTPD puerto 8080)** — 3 tabs: Publicidad, Logo, Fondos animales
- **Gestión de publicidad remota** desde cualquier navegador en la misma red WiFi
- **Fondos de imagen por animal** en celdas — cuando existe `fondos/{numero}.png`, el texto se oculta y solo se muestra la hora abajo (9sp)
- **Auto-reload del AdPanel** cada 10 segundos para detectar archivos subidos via HTTP
- **Permiso `MANAGE_EXTERNAL_STORAGE`** — solicitado al primer inicio para escritura en Android 11+
- **Fix crash en smartphones/tablets** — `window.insetsController` diferido con `decorView.post {}`
- **Fix crash HistoryScreen** — eliminado `return@Column` anti-patrón que corrompía el slot table de Compose; reemplazado por `if/else`
- **Flash de nuevos resultados** reducido a 4 segundos; fix del bug que lo mantenía encendido indefinidamente
- **Auto-scroll mejorado** — ciclo `primeraFila until ultimaFila` (sin tiempo muerto al fondo)

### v1.2
- **ResultadoCell rediseñado** — altura 80dp, imagen 44dp, layout vertical con hora/imagen/número/nombre
- **Logo en header** — `AsyncImage` junto al nombre de la agencia (56dp de alto)
- **Auto-scroll de grilla** — ciclo continuo con `animateScrollBy` + LinearEasing cada 3 segundos
- **Videos en AdPanel** — watchdog de 60s con flag `videoTerminado`; `remember(file)` para player fresco por archivo

### v1.1
- **Scrapers reescritos** con selectores HTML verificados en producción
- **Arquitectura cambiada** de cascada a paralelo — las 3 fuentes corren simultáneamente
- **Fix crítico** `normalizarHora`: preserva AM/PM original (resolvía resultados de tarde vacíos)
- **Guacharito Millonario** ampliado a 101 figuras (00-100), catálogo `ANIMALES_GUACHARITO`
- **`esNumeroValido`** usa `loteria.maxAnimal` en vez de valores hardcodeados
- **Auto-refresh** cada 5 minutos en `MainViewModel` (independiente del WorkManager)
- **Botón "Forzar actualización"** en pantalla de Configuración
- **Config recarga automática** al volver a MainScreen (`repeatOnLifecycle`)
- Agregadas dependencias `appcompat` y `material` faltantes
- Fix íconos adaptive-icon movidos a `mipmap-anydpi-v26/`
