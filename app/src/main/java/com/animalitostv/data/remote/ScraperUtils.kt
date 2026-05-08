package com.animalitostv.data.remote

import com.animalitostv.domain.model.Loteria
import com.animalitostv.domain.model.nombreAnimal
import com.animalitostv.domain.model.nombreAnimalPorLoteria

object ScraperUtils {

    /**
     * Normaliza el nombre de una lotería encontrado en el HTML
     * al enum Loteria correspondiente.
     */
    fun identificarLoteria(texto: String): Loteria? {
        val t = texto.uppercase()
            .replace("Á", "A").replace("É", "E").replace("Í", "I")
            .replace("Ó", "O").replace("Ú", "U")
        return when {
            t.contains("GUACHARITO") || t.contains("MILLONARIO") -> Loteria.GUACHARITO_MILLONARIO
            t.contains("GUACHARO") || t.contains("GUACHARO ACTIVO") -> Loteria.GUACHARO_ACTIVO
            t.contains("LOTTO ACTIVO") || t.contains("LOTTO_ACTIVO") -> Loteria.LOTTO_ACTIVO
            t.contains("GRANJITA") -> Loteria.LA_GRANJITA
            t.contains("LOTTO REY") || t.contains("LOTTORY") -> Loteria.LOTTO_REY
            t.contains("SELVA") -> Loteria.SELVA_PLUS
            else -> null
        }
    }

    /**
     * Parsea el string de número de animal desde el HTML.
     * "00" se mapea a -1 internamente para distinguirlo de 0 (Delfín).
     */
    fun parsearNumeroAnimal(str: String): Int? {
        val limpio = str.trim()
        if (limpio == "00") return -1
        return limpio.toIntOrNull()
    }

    /**
     * Valida que el número de animal sea coherente con la lotería.
     */
    fun esNumeroValido(numero: Int, loteria: Loteria): Boolean {
        if (numero == -1) return true  // -1 = Ballena ("00"), válida en todas las loterías
        return numero in 0..loteria.maxAnimal
    }

    /**
     * Normaliza una hora al formato usado en la app ("8:00 AM", "9:30 AM", etc.)
     */
    fun normalizarHora(texto: String): String? {
        val limpio = texto.trim().uppercase()

        // Caso 1: ya trae AM/PM explícito — usarlo directamente
        val regexAmPm = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""")
        val matchAmPm = regexAmPm.find(limpio)
        if (matchAmPm != null) {
            val hora = matchAmPm.groupValues[1].toIntOrNull() ?: return null
            val minutos = matchAmPm.groupValues[2]
            val amPm = matchAmPm.groupValues[3]
            return "$hora:$minutos $amPm"
        }

        // Caso 2: formato 24h (sin AM/PM) — convertir
        val regex24 = Regex("""(\d{1,2}):(\d{2})""")
        val match24 = regex24.find(limpio) ?: return null
        val hora = match24.groupValues[1].toIntOrNull() ?: return null
        val minutos = match24.groupValues[2]
        val amPm = if (hora >= 12) "PM" else "AM"
        val hora12 = when {
            hora == 0 -> 12
            hora > 12 -> hora - 12
            else -> hora
        }
        return "$hora12:$minutos $amPm"
    }

    /**
     * Construye un ResultadoSorteo básico a partir de número y lotería,
     * deduciendo el nombre del animal automáticamente.
     */
    fun buildResultado(
        numero: Int,
        loteria: Loteria,
        hora: String,
        fecha: java.time.LocalDate,
        fuente: String
    ): com.animalitostv.domain.model.ResultadoSorteo {
        return com.animalitostv.domain.model.ResultadoSorteo(
            loteria = loteria,
            fecha = fecha,
            hora = hora,
            numeroAnimal = numero,
            nombreAnimal = nombreAnimalPorLoteria(numero, loteria),
            fuenteDatos = fuente,
            fechaRegistro = java.time.LocalDateTime.now(),
            esNuevo = true
        )
    }
}
