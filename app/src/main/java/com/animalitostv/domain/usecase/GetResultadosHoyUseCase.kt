package com.animalitostv.domain.usecase

import com.animalitostv.data.repository.ResultadoRepository
import com.animalitostv.domain.model.Loteria
import com.animalitostv.domain.model.ResultadoSorteo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Devuelve un mapa de Lotería -> (hora -> resultado?) para el día de hoy.
 * Las celdas sin resultado aparecen como null (mostrar "Pendiente").
 */
class GetResultadosHoyUseCase @Inject constructor(
    private val repository: ResultadoRepository
) {
    operator fun invoke(): Flow<Map<Loteria, Map<String, ResultadoSorteo?>>> {
        return repository.observarResultadosHoy().map { lista ->
            buildGrilla(lista, LocalDate.now())
        }
    }

    fun porFecha(fecha: LocalDate): Flow<Map<Loteria, Map<String, ResultadoSorteo?>>> {
        return repository.observarResultadosPorFecha(fecha).map { lista ->
            buildGrilla(lista, fecha)
        }
    }

    private fun buildGrilla(
        lista: List<ResultadoSorteo>,
        fecha: LocalDate
    ): Map<Loteria, Map<String, ResultadoSorteo?>> {
        val porLoteria = lista.groupBy { it.loteria }
        return Loteria.todas.associateWith { loteria ->
            val resultadosLoteria = porLoteria[loteria]?.associateBy { it.hora } ?: emptyMap()
            // Para cada horario configurado devolvemos resultado o null
            loteria.horarios.associateWith { hora -> resultadosLoteria[hora] }
        }
    }
}
