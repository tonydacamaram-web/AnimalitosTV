package com.animalitostv.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.animalitostv.util.Constants

/**
 * Publica el servicio HTTP local (NanoHTTPD) en la red mediante DNS-SD
 * (NsdManager, tipo "_http._tcp.") para que aparezca en herramientas Bonjour.
 *
 * Importante: esto registra un *servicio*, NO un hostname. La URL
 * "http://animalitostv.local:8080" resuelve gracias al nombre de dispositivo
 * del TV (ver DeviceNameHelper), no por este registro.
 *
 * El nombre del servicio coincide con Constants.MDNS_SERVICE_NAME ("animalitostv").
 *
 * Nota: la resolución de nombres *.local depende del dispositivo cliente.
 * Funciona en Windows 10+, macOS e iPhone/iPad. Los navegadores de Android y
 * las Smart TV normalmente NO resuelven *.local, por lo que la IP se sigue
 * mostrando como respaldo en Configuración.
 *
 * Si el registro falla (red sin multicast, nombre duplicado, etc.) el servidor
 * HTTP continúa funcionando con normalidad vía IP: el fallo es silencioso.
 */
class MdnsAdvertiser(private val context: Context) {

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    @Volatile
    private var servicioRegistrado = false

    /**
     * Registra el servicio HTTP en mDNS. Es seguro llamarlo más de una vez:
     * si ya hay un registro activo no hace nada.
     */
    fun register(
        serviceName: String = Constants.MDNS_SERVICE_NAME,
        port: Int = Constants.AD_SERVER_PORT
    ) {
        if (servicioRegistrado) return
        try {
            val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsdManager = manager

            val info = NsdServiceInfo().apply {
                this.serviceName = serviceName
                this.serviceType = Constants.MDNS_SERVICE_TYPE
                this.port = port
            }

            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(registered: NsdServiceInfo) {
                    servicioRegistrado = true
                    Log.i(TAG, "mDNS registrado: http://${registered.serviceName}.local:$port")
                }

                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    servicioRegistrado = false
                    Log.w(TAG, "No se pudo registrar mDNS ($errorCode). El servidor sigue disponible por IP")
                }

                override fun onServiceUnregistered(info: NsdServiceInfo) {
                    servicioRegistrado = false
                    Log.i(TAG, "mDNS desregistrado: ${info.serviceName}")
                }

                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Fallo al desregistrar mDNS ($errorCode)")
                }
            }
            registrationListener = listener
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            servicioRegistrado = false
            Log.e(TAG, "Error al registrar mDNS", e)
        }
    }

    /**
     * Desregistra el servicio mDNS. Seguro de llamar aunque no esté registrado.
     */
    fun unregister() {
        val manager = nsdManager ?: return
        val listener = registrationListener ?: return
        runCatching { manager.unregisterService(listener) }
            .onFailure { Log.w(TAG, "Error al desregistrar mDNS", it) }
        registrationListener = null
        servicioRegistrado = false
    }

    companion object {
        private const val TAG = "MdnsAdvertiser"
    }
}