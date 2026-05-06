package com.animalitostv.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class ResultadoSorteo(
    val id: Long = 0,
    val loteria: Loteria,
    val fecha: LocalDate,
    val hora: String,
    val numeroAnimal: Int,
    val nombreAnimal: String,
    val fuenteDatos: String,
    val fechaRegistro: LocalDateTime,
    val esNuevo: Boolean = false
)

data class EstadisticasLoteria(
    val loteria: Loteria,
    val calientesHoy: List<Pair<Animal, Int>>,
    val calientesSemana: List<Pair<Animal, Int>>,
    val calientesMes: List<Pair<Animal, Int>>,
    val friosHoy: List<Animal>,
    val friosSemana: List<Animal>,
    val friosMes: List<Animal>,
    val rachaActual: Pair<Animal, Int>?   // (animal, cantidad de sorteos sin salir)
)
