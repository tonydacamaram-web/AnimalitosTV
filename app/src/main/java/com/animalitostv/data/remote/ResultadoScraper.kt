package com.animalitostv.data.remote

import com.animalitostv.domain.model.ResultadoSorteo
import java.time.LocalDate

interface ResultadoScraper {
    suspend fun obtenerResultados(fecha: LocalDate): List<ResultadoSorteo>
    fun getNombreFuente(): String
    fun getUrl(): String
}
