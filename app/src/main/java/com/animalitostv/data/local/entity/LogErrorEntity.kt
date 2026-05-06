package com.animalitostv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "logs_error")
data class LogErrorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: LocalDateTime,
    val tipo: String,         // "SCRAPING", "CONEXION", "PARSE", "DB"
    val mensaje: String,
    val fuente: String? = null,
    val stackTrace: String? = null
)
