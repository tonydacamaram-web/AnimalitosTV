package com.animalitostv.service

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.animalitostv.util.Constants

/**
 * Intenta fijar el nombre de red del dispositivo a "animalitostv" para que el
 * hostname mDNS `animalitostv.local` resuelva desde la PC.
 *
 * Importante: [MdnsAdvertiser] solo publica un *servicio* DNS-SD; el hostname
 * `*.local` lo anuncia el responder mDNS del sistema a partir del nombre del
 * dispositivo (`Settings.Global` → `device_name`, Android 7.1+). Sin este
 * nombre, `animalitostv.local` no resuelve aunque el servicio NSD esté activo.
 * `NsdManager` no expone un `setHostname()` público (API 34), por eso se usa
 * `Settings.Global.DEVICE_NAME`.
 *
 * Requiere el permiso especial `WRITE_SETTINGS` ("Modificar ajustes del
 * sistema"). Si no está concedido, el cambio falla en silencio (solo se
 * registra en log) y debe hacerse a mano en Ajustes del TV → Acerca de /
 * Preferencias del dispositivo → Nombre del dispositivo.
 *
 * El sistema puede tardar en anunciar el nuevo hostname: reconectar la red o
 * reiniciar el TV fuerza el anuncio.
 */
object DeviceNameHelper {

    private const val TAG = "DeviceNameHelper"

    /**
     * Best-effort: fija el nombre del dispositivo si hay permiso para escribir
     * ajustes. Nunca lanza excepciones ni muestra diálogos (modo kiosco).
     */
    fun asegurarNombreDispositivo(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            Log.i(TAG, "Android < 7.1: no se puede fijar el hostname .local por API; use la IP")
            return
        }

        runCatching {
            if (!Settings.System.canWrite(context)) {
                Log.i(
                    TAG,
                    "WRITE_SETTINGS no concedido: fije el nombre del dispositivo a mano en Ajustes del TV"
                )
                return@runCatching
            }

            val resolver = context.contentResolver
            val actual = Settings.Global.getString(resolver, Settings.Global.DEVICE_NAME)
            if (actual == Constants.MDNS_SERVICE_NAME) {
                Log.i(TAG, "Nombre de dispositivo ya es '${Constants.MDNS_SERVICE_NAME}'")
                return@runCatching
            }

            Settings.Global.putString(
                resolver,
                Settings.Global.DEVICE_NAME,
                Constants.MDNS_SERVICE_NAME
            )

            val nuevo = Settings.Global.getString(resolver, Settings.Global.DEVICE_NAME)
            if (nuevo == Constants.MDNS_SERVICE_NAME) {
                Log.i(
                    TAG,
                    "Nombre de dispositivo fijado a '${Constants.MDNS_SERVICE_NAME}'. " +
                        "Puede requerir reconectar la red o reiniciar el TV"
                )
            } else {
                Log.w(TAG, "El sistema no aplicó el cambio de nombre (actual: '$nuevo')")
            }
        }.onFailure { e ->
            Log.w(TAG, "No se pudo fijar el nombre del dispositivo: ${e.message}", e)
        }
    }
}
