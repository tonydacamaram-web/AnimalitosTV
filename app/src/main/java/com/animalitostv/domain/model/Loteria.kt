package com.animalitostv.domain.model

import androidx.compose.ui.graphics.Color

enum class Loteria(
    val id: String,
    val displayName: String,
    val color: Color,
    val esExtendida: Boolean,
    val maxAnimal: Int,          // número máximo de animal permitido
    val horarios: List<String>,
    val visible: Boolean = true  // false = ocultar de la grilla principal
) {
    GUACHARO_ACTIVO(
        id = "GUACHARO",
        displayName = "Guacharo Activo",
        color = Color(0xFFFF6B00),
        esExtendida = true,
        maxAnimal = 75,
        horarios = listOf(
            "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM"
        )
    ),
    GUACHARITO_MILLONARIO(
        id = "GUACHARITO",
        displayName = "Guacharito Millonario",
        color = Color(0xFFFFD700),
        esExtendida = true,
        maxAnimal = 100,         // 101 figuras: 00-100
        horarios = listOf(
            "8:30 AM", "9:30 AM", "10:30 AM", "11:30 AM", "12:30 PM",
            "1:30 PM", "2:30 PM", "3:30 PM", "4:30 PM", "5:30 PM", "6:30 PM", "7:30 PM"
        )
    ),
    LOTTO_ACTIVO(
        id = "LOTTO_ACTIVO",
        displayName = "Lotto Activo",
        color = Color(0xFFE53935),
        esExtendida = false,
        maxAnimal = 37,
        horarios = listOf(
            "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM"
        )
    ),
    LA_GRANJITA(
        id = "GRANJITA",
        displayName = "La Granjita",
        color = Color(0xFF43A047),
        esExtendida = false,
        maxAnimal = 37,
        horarios = listOf(
            "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM"
        )
    ),
    LOTTO_REY(
        id = "LOTTO_REY",
        displayName = "Lotto Rey",
        color = Color(0xFF1E88E5),
        esExtendida = false,
        maxAnimal = 37,
        horarios = listOf(
            "8:30 AM", "9:30 AM", "10:30 AM", "11:30 AM", "12:30 PM",
            "1:30 PM", "2:30 PM", "3:30 PM", "4:30 PM", "5:30 PM", "6:30 PM", "7:30 PM"
        ),
        visible = false  // inhabilitado — oculto de la grilla hasta reactivación
    ),
    SELVA_PLUS(
        id = "SELVA",
        displayName = "Selva Plus",
        color = Color(0xFF6D4C41),
        esExtendida = false,
        maxAnimal = 37,
        horarios = listOf(
            "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM"
        )
    );

    companion object {
        fun fromId(id: String): Loteria? = values().firstOrNull { it.id == id }
        val todas: List<Loteria> = values().filter { it.visible }
        val todasIncluyendoOcultas: List<Loteria> = values().toList()
    }
}
