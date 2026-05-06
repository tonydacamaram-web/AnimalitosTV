package com.animalitostv.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BcvRateFetcher @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    /**
     * Obtiene la tasa BCV con dos intentos:
     * 1. ve.dolarapi.com  — JSON rápido, valida que la fecha coincida con hoy
     * 2. Scraping bcv.org.ve — fallback con Jsoup
     *
     * Devuelve null si ambas fuentes fallan (sin lanzar excepción).
     */
    suspend fun obtenerTasa(): Double? = withContext(Dispatchers.IO) {
        fetchDesdeApi() ?: fetchDesdeBcv()
    }

    private fun fetchDesdeApi(): Double? = runCatching {
        val request = Request.Builder()
            .url("https://ve.dolarapi.com/v1/dolares/oficial")
            .header("Accept", "application/json")
            .build()

        val body = okHttpClient.newCall(request).execute().use { res ->
            if (!res.isSuccessful) return null
            res.body?.string() ?: return null
        }

        // Parse manual para evitar dependencia de serialización extra
        val promedio = Regex(""""promedio"\s*:\s*([\d.]+)""").find(body)?.groupValues?.get(1)?.toDoubleOrNull()
        val venta    = Regex(""""venta"\s*:\s*([\d.]+)""").find(body)?.groupValues?.get(1)?.toDoubleOrNull()
        val tasa = promedio ?: venta ?: return null

        // Validar que la fecha sea del día actual
        val fechaApi = Regex(""""fechaActualizacion"\s*:\s*"(\d{4}-\d{2}-\d{2})""").find(body)?.groupValues?.get(1)
        val hoy = java.time.LocalDate.now().toString()
        if (fechaApi != null && fechaApi != hoy) return null

        tasa.takeIf { it > 0 }
    }.getOrNull()

    private fun fetchDesdeBcv(): Double? = runCatching {
        val clientSinVerificacion = okHttpClient.newBuilder()
            .hostnameVerifier { _, _ -> true }
            .sslSocketFactory(
                createTrustAllSslContext().socketFactory,
                createTrustAllTrustManager()
            )
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://www.bcv.org.ve/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "es-VE,es;q=0.9")
            .build()

        val html = clientSinVerificacion.newCall(request).execute().use { res ->
            if (!res.isSuccessful) return null
            res.body?.string() ?: return null
        }

        // Misma expresión regular que victory-victoria
        val match = Regex("""id="dolar"[\s\S]*?<strong[^>]*>\s*([\d,]+)\s*</strong>""", RegexOption.IGNORE_CASE)
            .find(html) ?: return null

        match.groupValues[1].replace(",", ".").trim().toDoubleOrNull()?.takeIf { it > 0 }
    }.getOrNull()

    // ── TrustAll para bcv.org.ve (certificado intermedio no reconocido) ────────

    private fun createTrustAllTrustManager(): javax.net.ssl.X509TrustManager =
        object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
        }

    private fun createTrustAllSslContext(): javax.net.ssl.SSLContext =
        javax.net.ssl.SSLContext.getInstance("TLS").also {
            it.init(null, arrayOf(createTrustAllTrustManager()), java.security.SecureRandom())
        }
}
