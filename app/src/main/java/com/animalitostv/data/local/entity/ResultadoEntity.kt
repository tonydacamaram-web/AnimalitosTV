package com.animalitostv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "resultados",
    indices = [
        Index(value = ["loteria", "fecha", "hora"], unique = true),
        Index(value = ["fecha"]),
        Index(value = ["loteria"])
    ]
)
data class ResultadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loteria: String,
    val fecha: LocalDate,
    val hora: String,
    val numeroAnimal: Int,
    val nombreAnimal: String,
    val fuenteDatos: String,
    val fechaRegistro: LocalDateTime,
    val esNuevo: Boolean = true
)
