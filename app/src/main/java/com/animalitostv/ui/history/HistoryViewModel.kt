package com.animalitostv.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animalitostv.domain.model.Loteria
import com.animalitostv.domain.model.ResultadoSorteo
import com.animalitostv.domain.usecase.GetResultadosHoyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val fecha: LocalDate = LocalDate.now(),
    val grilla: Map<Loteria, Map<String, ResultadoSorteo?>> = emptyMap(),
    val cargando: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getResultadosHoyUseCase: GetResultadosHoyUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var grillaJob: kotlinx.coroutines.Job? = null

    init {
        cargarFecha(LocalDate.now())
    }

    fun cargarFecha(fecha: LocalDate) {
        grillaJob?.cancel()
        _state.update { it.copy(fecha = fecha, cargando = true) }
        grillaJob = viewModelScope.launch {
            getResultadosHoyUseCase.porFecha(fecha).collect { grilla ->
                _state.update { it.copy(grilla = grilla, cargando = false) }
            }
        }
    }

    fun diaAnterior() = cargarFecha(_state.value.fecha.minusDays(1))
    fun diaSiguiente() {
        val nueva = _state.value.fecha.plusDays(1)
        if (!nueva.isAfter(LocalDate.now())) cargarFecha(nueva)
    }

    fun irAHoy() = cargarFecha(LocalDate.now())
}
