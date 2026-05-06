package com.animalitostv.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animalitostv.data.repository.ConfiguracionRepository
import com.animalitostv.util.Constants
import java.io.File
import com.animalitostv.data.repository.ResultadoRepository
import com.animalitostv.domain.model.EstadisticasLoteria
import com.animalitostv.domain.model.Loteria
import com.animalitostv.domain.model.ResultadoSorteo
import com.animalitostv.domain.usecase.GetEstadisticasUseCase
import com.animalitostv.domain.usecase.GetResultadosHoyUseCase
import com.animalitostv.data.remote.BcvRateFetcher
import com.animalitostv.data.remote.ScrapingOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class MainUiState(
    val grilla: Map<Loteria, Map<String, ResultadoSorteo?>> = emptyMap(),
    val estadisticas: List<EstadisticasLoteria> = emptyList(),
    val mensajesPersonalizados: List<String> = emptyList(),
    val hayConexion: Boolean = true,
    val ultimaActualizacion: LocalDateTime? = null,
    val nombreAgencia: String = "AnimalitosTV",
    val reloj24h: Boolean = true,
    val adPanelAnchoPercent: Int = 25,
    val adRotacionSegundos: Int = 10,
    val adAudio: Boolean = false,
    val adCarpeta: String = "/sdcard/AnimalitosAds",
    val tickerVelocidad: Float = 1.0f,
    val pantallaEncendida: Boolean = true,
    val cargando: Boolean = true,
    val tasaBcv: Double? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getResultadosHoyUseCase: GetResultadosHoyUseCase,
    private val getEstadisticasUseCase: GetEstadisticasUseCase,
    private val configRepository: ConfiguracionRepository,
    private val resultadoRepository: ResultadoRepository,
    private val scrapingOrchestrator: ScrapingOrchestrator,
    private val bcvRateFetcher: BcvRateFetcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        cargarConfiguracion()
        observarResultados()
        actualizarEstadisticas()
        iniciarRefreshAutomatico()
        observarMensajes()
        vigilarCambioDeDia()
        iniciarRefreshTasaBcv()
    }

    private fun cargarConfiguracion() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    nombreAgencia = configRepository.getNombreAgencia(),
                    reloj24h = configRepository.getReloj24h(),
                    adPanelAnchoPercent = configRepository.getAdPanelAncho(),
                    adRotacionSegundos = configRepository.getAdRotacionSegundos(),
                    adAudio = configRepository.getAdAudio(),
                    adCarpeta = configRepository.getAdCarpeta(),
                    tickerVelocidad = configRepository.getTickerVelocidad(),
                    pantallaEncendida = configRepository.getPantallaEncendida()
                )
            }
        }
    }

    private var jobResultados: Job? = null

    private fun observarResultados(fecha: LocalDate = LocalDate.now()) {
        jobResultados?.cancel()
        jobResultados = viewModelScope.launch {
            getResultadosHoyUseCase.porFecha(fecha).collect { grilla ->
                _uiState.update { it.copy(grilla = grilla, cargando = false) }
                resultadoRepository.obtenerNuevos().forEach { nuevo ->
                    resultadoRepository.marcarVisto(nuevo.id)
                }
            }
        }
    }

    private fun vigilarCambioDeDia() {
        viewModelScope.launch {
            var fechaActual = LocalDate.now()
            while (true) {
                // Calcular milisegundos hasta la medianoche siguiente (zona Venezuela)
                val ahora = ZonedDateTime.now(Constants.ZONA_HORARIA_VE)
                val medianoche = ahora.toLocalDate().plusDays(1)
                    .atStartOfDay(Constants.ZONA_HORARIA_VE)
                val msHastaMedianoche = medianoche.toInstant().toEpochMilli() -
                    ahora.toInstant().toEpochMilli()

                delay(msHastaMedianoche + 2_000L) // +2s de margen

                val nuevaFecha = LocalDate.now()
                if (nuevaFecha != fechaActual) {
                    fechaActual = nuevaFecha
                    // Reiniciar grilla con el nuevo día (vacía al principio)
                    _uiState.update { it.copy(grilla = emptyMap(), cargando = true) }
                    observarResultados(fechaActual)
                    actualizarEstadisticas()
                    scrapingOrchestrator.ejecutar(fechaActual)
                }
            }
        }
    }

    fun actualizarEstadisticas() {
        viewModelScope.launch {
            try {
                val stats = getEstadisticasUseCase(LocalDate.now())
                _uiState.update { it.copy(estadisticas = stats, ultimaActualizacion = LocalDateTime.now()) }
            } catch (_: Exception) {}
        }
    }

    private fun iniciarRefreshAutomatico() {
        viewModelScope.launch {
            // Scraping inmediato al iniciar, luego cada 5 minutos
            scrapingOrchestrator.ejecutar(LocalDate.now())
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutos
                scrapingOrchestrator.ejecutar(LocalDate.now())
                actualizarEstadisticas()
            }
        }
    }

    private fun observarMensajes() {
        viewModelScope.launch {
            while (true) {
                val mensajes = runCatching {
                    File(Constants.MENSAJES_PATH)
                        .takeIf { it.exists() }
                        ?.readLines()
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()
                }.getOrDefault(emptyList())
                _uiState.update { it.copy(mensajesPersonalizados = mensajes) }
                delay(30_000L) // releer cada 30 segundos
            }
        }
    }

    private fun iniciarRefreshTasaBcv() {
        viewModelScope.launch {
            while (true) {
                val tasa = bcvRateFetcher.obtenerTasa()
                if (tasa != null) {
                    _uiState.update { it.copy(tasaBcv = tasa) }
                }
                delay(60 * 60 * 1000L) // refresca cada hora
            }
        }
    }

    fun setHayConexion(conectado: Boolean) {
        _uiState.update { it.copy(hayConexion = conectado) }
    }

    fun recargarConfiguracion() = cargarConfiguracion()
}
