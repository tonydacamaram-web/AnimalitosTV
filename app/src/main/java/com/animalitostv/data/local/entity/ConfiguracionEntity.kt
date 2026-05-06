package com.animalitostv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracion")
data class ConfiguracionEntity(
    @PrimaryKey val clave: String,
    val valor: String
)
