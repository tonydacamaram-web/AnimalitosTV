package com.animalitostv.domain.usecase

import com.animalitostv.data.repository.ResultadoRepository
import com.animalitostv.domain.model.*
import java.time.LocalDate
import javax.inject.Inject

class GetEstadisticasUseCase @Inject constructor(
    private val repository: ResultadoRepository
) {
    suspend operator fun invoke(fecha: LocalDate = LocalDate.now()): List<EstadisticasLoteria> {
        return Loteria.todas.map { loteria ->
            calcularEstadisticas(loteria, fecha)
        }
    }

    private suspend fun calcularEstadisticas(
        loteria: Loteria,
        hoy: LocalDate
    ): EstadisticasLoteria {
        val catalogo = when (loteria) {
            Loteria.GUACHARITO_MILLONARIO -> ANIMALES_GUACHARITO
            Loteria.GUACHARO_ACTIVO -> ANIMALES_EXTENDIDOS
            else -> ANIMALES_ESTANDAR
        }

        val conteosHoy = repository.contarAnimalesPorLoteria(loteria, hoy, hoy)
        val conteosSemana = repository.contarAnimalesPorLoteria(loteria, hoy.minusDays(6), hoy)
        val conteosMes = repository.contarAnimalesPorLoteria(loteria, hoy.minusDays(29), hoy)

        val mapHoy = conteosHoy.associate { it.numeroAnimal to it.total }
        val mapSemana = conteosSemana.associate { it.numeroAnimal to it.total }
        val mapMes = conteosMes.associate { it.numeroAnimal to it.total }

        // Calientes: top 5 más frecuentes
        val calientesHoy = catalogo
            .mapNotNull { animal -> mapHoy[animal.numero]?.let { Pair(animal, it) } }
            .sortedByDescending { it.second }.take(5)

        val calientesSemana = catalogo
            .mapNotNull { animal -> mapSemana[animal.numero]?.let { Pair(animal, it) } }
            .sortedByDescending { it.second }.take(5)

        val calientesMes = catalogo
            .mapNotNull { animal -> mapMes[animal.numero]?.let { Pair(animal, it) } }
            .sortedByDescending { it.second }.take(5)

        // Fríos: animales que no han salido en el período
        val friosHoy = catalogo.filter { (mapHoy[it.numero] ?: 0) == 0 }
        val friosSemana = catalogo.filter { (mapSemana[it.numero] ?: 0) == 0 }
        val friosMes = catalogo.filter { (mapMes[it.numero] ?: 0) == 0 }

        // Racha: animal con más sorteos consecutivos sin salir (basado en datos disponibles)
        val rachaActual = friosSemana.firstOrNull()?.let {
            Pair(it, mapSemana.getOrDefault(it.numero, 0))
        }

        return EstadisticasLoteria(
            loteria = loteria,
            calientesHoy = calientesHoy,
            calientesSemana = calientesSemana,
            calientesMes = calientesMes,
            friosHoy = friosHoy,
            friosSemana = friosSemana,
            friosMes = friosMes,
            rachaActual = rachaActual
        )
    }
}
