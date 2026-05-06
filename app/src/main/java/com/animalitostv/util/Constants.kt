package com.animalitostv.util

import java.time.ZoneId

object Constants {

    val ZONA_HORARIA_VE: ZoneId = ZoneId.of("America/Caracas")

    const val SDCARD_BASE = "/sdcard/AnimalitosTV"
    const val SDCARD_ADS = "/sdcard/AnimalitosAds"
    const val LOGO_PATH = "$SDCARD_BASE/logo.png"
    const val ANIMALES_PATH = "$SDCARD_BASE/animales"
    const val FONDOS_PATH = "$SDCARD_BASE/fondos"
    const val MENSAJES_PATH = "$SDCARD_BASE/mensajes.txt"

    const val SCRAPING_INTERVAL_MINUTES = 15L
    const val SCRAPING_RETRY_MAX = 3
    const val SCRAPING_TIMEOUT_SECONDS = 30L
    const val VIDEO_WATCHDOG_SECONDS = 60L

    const val PIN_DEFAULT = "1234"
    const val DB_RETENTION_DAYS = 31L
    const val LOG_RETENTION_DAYS = 7L

    const val AD_ROTATION_DEFAULT_SECONDS = 10
    const val AD_PANEL_WIDTH_DEFAULT_PERCENT = 25

    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    // Claves de configuración
    object Config {
        const val NOMBRE_AGENCIA = "nombre_agencia"
        const val LOGO_RUTA = "logo_ruta"
        const val PIN = "pin"
        const val PRIMER_USO = "primer_uso"
        const val AD_PANEL_ANCHO = "ad_panel_ancho"
        const val AD_ROTACION_SEGUNDOS = "ad_rotacion_segundos"
        const val AD_AUDIO = "ad_audio"
        const val AD_CARPETA = "ad_carpeta"
        const val RELOJ_24H = "reloj_24h"
        const val TICKER_VELOCIDAD = "ticker_velocidad"
        const val AUTO_INICIO = "auto_inicio"
        const val PANTALLA_ENCENDIDA = "pantalla_encendida"
    }
}
