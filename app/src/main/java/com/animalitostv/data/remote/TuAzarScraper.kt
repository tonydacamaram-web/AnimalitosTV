package com.animalitostv.data.remote

import com.animalitostv.domain.model.ResultadoSorteo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.time.LocalDate
import javax.inject.Inject

/**
 * Scraper para tuazar.com/loteria/animalitos/resultados/
 *
 * Estructura HTML confirmada:
 *   div.resultados                    → bloque por lotería
 *     h2.lotResTit                    → nombre de la lotería
 *     div.col-xs-6.col-sm-3           → cada sorteo individual
 *       div.horario                   → hora "8:00 AM"
 *       span                          → resultado "42 - TUCÁN"  o "- -" si pendiente
 */
class TuAzarScraper @Inject constructor(
    private val client: OkHttpClient
) : ResultadoScraper {

    override fun getNombreFuente() = "tuazar"
    override fun getUrl() = "https://www.tuazar.com/loteria/animalitos/resultados/"

    var ultimoHtmlResumen: String = ""

    override suspend fun obtenerResultados(fecha: LocalDate): List<ResultadoSorteo> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(getUrl()).build()
            val html = client.newCall(request).execute().use { it.body?.string() ?: "" }
            if (html.isBlank()) return@withContext emptyList()

            ultimoHtmlResumen = html.take(400)

            val doc = Jsoup.parse(html)
            val resultados = mutableListOf<ResultadoSorteo>()

            // 1. Encontrar todos los bloques de lotería
            val bloques = doc.select("div.resultados")

            for (bloque in bloques) {
                // 2. Nombre de la lotería
                val nombreLoteria = bloque.selectFirst("h2.lotResTit")?.text()?.trim() ?: continue
                val loteria = ScraperUtils.identificarLoteria(nombreLoteria) ?: continue

                // 3. Cada sorteo individual
                val sorteos = bloque.select("div.col-xs-6.col-sm-3")

                for (sorteo in sorteos) {
                    // 4. Hora
                    val hora = sorteo.selectFirst("div.horario")?.text()?.trim() ?: continue
                    val horaNorm = ScraperUtils.normalizarHora(hora) ?: continue
                    if (horaNorm !in loteria.horarios) continue

                    // 5. Resultado: primer span → "42 - TUCÁN" o "- -"
                    val textoResultado = sorteo.selectFirst("span")?.text()?.trim() ?: continue

                    // Saltar pendientes
                    if (textoResultado.startsWith("-") || textoResultado == "- -") continue

                    // Parsear "42 - TUCÁN"
                    val partes = textoResultado.split("-", limit = 2)
                    if (partes.size < 2) continue

                    val numeroStr = partes[0].trim()
                    if (!numeroStr.all { it.isDigit() }) continue

                    val numero = ScraperUtils.parsearNumeroAnimal(numeroStr) ?: continue
                    if (!ScraperUtils.esNumeroValido(numero, loteria)) continue

                    resultados.add(
                        ScraperUtils.buildResultado(numero, loteria, horaNorm, fecha, getNombreFuente())
                    )
                }
            }

            resultados
        }
}
