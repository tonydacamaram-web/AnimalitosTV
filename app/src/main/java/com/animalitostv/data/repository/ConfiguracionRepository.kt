package com.animalitostv.data.repository

import com.animalitostv.data.local.dao.ConfiguracionDao
import com.animalitostv.data.local.entity.ConfiguracionEntity
import com.animalitostv.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfiguracionRepository @Inject constructor(
    private val dao: ConfiguracionDao
) {
    suspend fun get(clave: String, defecto: String = ""): String =
        dao.obtener(clave)?.valor ?: defecto

    fun observe(clave: String): Flow<String?> =
        dao.observar(clave).map { it?.valor }

    suspend fun set(clave: String, valor: String) =
        dao.guardar(ConfiguracionEntity(clave, valor))

    suspend fun esPrimerUso(): Boolean =
        get(Constants.Config.PRIMER_USO, "true").toBoolean()

    suspend fun marcarPrimerUsoCompletado() =
        set(Constants.Config.PRIMER_USO, "false")

    suspend fun getPin(): String =
        get(Constants.Config.PIN, Constants.PIN_DEFAULT)

    suspend fun setPin(pin: String) =
        set(Constants.Config.PIN, pin)

    suspend fun getNombreAgencia(): String =
        get(Constants.Config.NOMBRE_AGENCIA, "Mi Agencia")

    suspend fun getPantallaEncendida(): Boolean =
        get(Constants.Config.PANTALLA_ENCENDIDA, "true").toBoolean()

    suspend fun getAutoInicio(): Boolean =
        get(Constants.Config.AUTO_INICIO, "false").toBoolean()

    suspend fun getReloj24h(): Boolean =
        get(Constants.Config.RELOJ_24H, "true").toBoolean()

    suspend fun getAdPanelAncho(): Int =
        get(Constants.Config.AD_PANEL_ANCHO, Constants.AD_PANEL_WIDTH_DEFAULT_PERCENT.toString()).toIntOrNull()
            ?: Constants.AD_PANEL_WIDTH_DEFAULT_PERCENT

    suspend fun getAdRotacionSegundos(): Int =
        get(Constants.Config.AD_ROTACION_SEGUNDOS, Constants.AD_ROTATION_DEFAULT_SECONDS.toString()).toIntOrNull()
            ?: Constants.AD_ROTATION_DEFAULT_SECONDS

    suspend fun getAdAudio(): Boolean =
        get(Constants.Config.AD_AUDIO, "false").toBoolean()

    suspend fun getAdCarpeta(): String =
        get(Constants.Config.AD_CARPETA, Constants.SDCARD_ADS)

    suspend fun getTickerVelocidad(): Float =
        get(Constants.Config.TICKER_VELOCIDAD, "1.0").toFloatOrNull() ?: 1.0f
}
