package com.animalitostv.data.remote

import com.animalitostv.domain.model.ResultadoSorteo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDate
import javax.inject.Inject

/**
 * Scraper para lotoven.com/animalitos/
 *
 * Estructura real observada:
 * - Cada lotería encabezada por <h3>Resultados [Nombre Lotería]</h3>
 * - Resultados en texto plano: "34 Venado 08:00 AM"
 * - Patrón por línea: [número] [nombre animal] [HH:MM AM/PM]
 */
class LotoVenScraper @Inject constructor(
    private val client: OkHttpClient
) : ResultadoScraper {

    override fun getNombreFuente() = "lotoven"
    override fun getUrl() = "https://lotoven.com/animalitos/"

    override suspend fun obtenerResultados(fecha: LocalDate): List<ResultadoSorteo> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(getUrl()).build()
            val html = client.newCall(request).execute().use { it.body?.string() ?: "" }
            if (html.isBlank()) return@withContext emptyList()
            parsear(Jsoup.parse(html), fecha)
        }

    private fun parsear(doc: Document, fecha: LocalDate): List<ResultadoSorteo> {
        val resultados = mutableListOf<ResultadoSorteo>()

        // Buscar todos los h3 que contienen nombres de loterías
        val encabezados = doc.select("h3")

        for (h3 in encabezados) {
            val tituloH3 = h3.text()
            // El h3 puede decir "Resultados Lotto Activo" o solo "Lotto Activo"
            val loteria = ScraperUtils.identificarLoteria(tituloH3) ?: continue

            // Recolectar todo el texto del bloque siguiente al h3
            // hasta el próximo h3
            val textoBloque = StringBuilder()
            var siguiente = h3.nextElementSibling()
            while (siguiente != null && siguiente.tagName() != "h3") {
                textoBloque.append(siguiente.text()).append("\n")
                siguiente = siguiente.nextElementSibling()
            }

            // Si el bloque está vacío, intentar con el texto del padre del h3
            val textoBuscar = if (textoBloque.isBlank()) {
                h3.parent()?.text() ?: ""
            } else {
                textoBloque.toString()
            }

            // Extraer resultados con patrón: número nombre hora
            // Ejemplos: "34 Venado 08:00 AM", "05 León 09:00 AM"
            val patron = Regex(
                """(\d{1,3})\s+([A-Za-záéíóúÁÉÍÓÚñÑüÜ\s]+?)\s+(\d{1,2}:\d{2}\s*[AP]M)""",
                RegexOption.IGNORE_CASE
            )

            for (match in patron.findAll(textoBuscar)) {
                val numero = ScraperUtils.parsearNumeroAnimal(match.groupValues[1]) ?: continue
                val hora = ScraperUtils.normalizarHora(match.groupValues[3]) ?: continue

                if (!ScraperUtils.esNumeroValido(numero, loteria)) continue
                if (hora !in loteria.horarios) continue
                if (resultados.none { it.loteria == loteria && it.hora == hora }) {
                    resultados.add(
                        ScraperUtils.buildResultado(numero, loteria, hora, fecha, getNombreFuente())
                    )
                }
            }
        }

        return resultados
    }
}
