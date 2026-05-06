package com.animalitostv.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animalitostv.data.local.entity.LogErrorEntity
import com.animalitostv.data.remote.ScrapingOrchestrator
import com.animalitostv.data.repository.ConfiguracionRepository
import com.animalitostv.data.repository.LogRepository
import com.animalitostv.data.repository.ResultadoRepository
import com.animalitostv.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.LocalDate
import javax.inject.Inject

data class ConfigUiState(
    val pinIngresado: String = "",
    val pinVerificado: Boolean = false,
    val errorPin: Boolean = false,
    val primerUso: Boolean = false,
    val nombreAgencia: String = "",
    val reloj24h: Boolean = true,
    val adPanelAncho: Int = 25,
    val adRotacionSegundos: Int = 10,
    val adAudio: Boolean = false,
    val adCarpeta: String = Constants.SDCARD_ADS,
    val tickerVelocidad: Float = 1.0f,
    val autoInicio: Boolean = false,
    val pantallaEncendida: Boolean = true,
    val logs: List<LogErrorEntity> = emptyList(),
    val guardado: Boolean = false
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val configRepository: ConfiguracionRepository,
    private val logRepository: LogRepository,
    private val resultadoRepository: ResultadoRepository,
    private val scrapingOrchestrator: ScrapingOrchestrator
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigUiState())
    val state: StateFlow<ConfigUiState> = _state.asStateFlow()

    init {
        cargarConfig()
        observarLogs()
    }

    private fun cargarConfig() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    primerUso = configRepository.esPrimerUso(),
                    nombreAgencia = configRepository.getNombreAgencia(),
                    reloj24h = configRepository.getReloj24h(),
                    adPanelAncho = configRepository.getAdPanelAncho(),
                    adRotacionSegundos = configRepository.getAdRotacionSegundos(),
                    adAudio = configRepository.getAdAudio(),
                    adCarpeta = configRepository.getAdCarpeta(),
                    tickerVelocidad = configRepository.getTickerVelocidad(),
                    autoInicio = configRepository.getAutoInicio(),
                    pantallaEncendida = configRepository.getPantallaEncendida()
                )
            }
        }
    }

    private fun observarLogs() {
        viewModelScope.launch {
            logRepository.observarLogs().collect { logs ->
                _state.update { it.copy(logs = logs) }
            }
        }
    }

    fun ingresarDigitoPin(digito: String) {
        val actual = _state.value.pinIngresado
        if (actual.length < 6) {
            _state.update { it.copy(pinIngresado = actual + digito, errorPin = false) }
        }
    }

    fun borrarDigitoPin() {
        val actual = _state.value.pinIngresado
        if (actual.isNotEmpty()) {
            _state.update { it.copy(pinIngresado = actual.dropLast(1)) }
        }
    }

    fun verificarPin() {
        viewModelScope.launch {
            val pinCorrecto = configRepository.getPin()
            val pinIngresado = _state.value.pinIngresado
            if (pinIngresado == pinCorrecto || _state.value.primerUso) {
                _state.update { it.copy(pinVerificado = true, errorPin = false) }
            } else {
                _state.update { it.copy(pinIngresado = "", errorPin = true) }
            }
        }
    }

    fun setNombreAgencia(nombre: String) = _state.update { it.copy(nombreAgencia = nombre) }
    fun setReloj24h(v: Boolean) = _state.update { it.copy(reloj24h = v) }
    fun setAdPanelAncho(v: Int) = _state.update { it.copy(adPanelAncho = v) }
    fun setAdRotacion(v: Int) = _state.update { it.copy(adRotacionSegundos = v) }
    fun setAdAudio(v: Boolean) = _state.update { it.copy(adAudio = v) }
    fun setAdCarpeta(v: String) = _state.update { it.copy(adCarpeta = v) }
    fun setTickerVelocidad(v: Float) = _state.update { it.copy(tickerVelocidad = v) }
    fun setAutoInicio(v: Boolean) = _state.update { it.copy(autoInicio = v) }
    fun setPantallaEncendida(v: Boolean) = _state.update { it.copy(pantallaEncendida = v) }

    fun guardar(nuevoPin: String? = null) {
        viewModelScope.launch {
            val s = _state.value
            configRepository.setPin(nuevoPin ?: configRepository.getPin())
            configRepository.set(Constants.Config.NOMBRE_AGENCIA, s.nombreAgencia)
            configRepository.set(Constants.Config.RELOJ_24H, s.reloj24h.toString())
            configRepository.set(Constants.Config.AD_PANEL_ANCHO, s.adPanelAncho.toString())
            configRepository.set(Constants.Config.AD_ROTACION_SEGUNDOS, s.adRotacionSegundos.toString())
            configRepository.set(Constants.Config.AD_AUDIO, s.adAudio.toString())
            configRepository.set(Constants.Config.AD_CARPETA, s.adCarpeta)
            configRepository.set(Constants.Config.TICKER_VELOCIDAD, s.tickerVelocidad.toString())
            configRepository.set(Constants.Config.AUTO_INICIO, s.autoInicio.toString())
            configRepository.set(Constants.Config.PANTALLA_ENCENDIDA, s.pantallaEncendida.toString())
            if (s.primerUso) configRepository.marcarPrimerUsoCompletado()
            _state.update { it.copy(guardado = true) }
        }
    }

    fun limpiarBaseDatos() {
        viewModelScope.launch {
            resultadoRepository.limpiarAntiguos(LocalDate.now().plusDays(1))
            logRepository.limpiarTodo()
        }
    }

    fun forzarActualizacion() {
        viewModelScope.launch {
            scrapingOrchestrator.ejecutar(LocalDate.now())
        }
    }

    fun resetGuardado() = _state.update { it.copy(guardado = false) }

    val ipLocal: String
        get() = runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "desconocida"
        }.getOrDefault("desconocida")
}
