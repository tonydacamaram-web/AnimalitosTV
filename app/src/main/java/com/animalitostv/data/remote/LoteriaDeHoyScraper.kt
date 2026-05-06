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
 * Scraper para loteriadehoy.com/animalitos/resultados/
 *
 * Estructura HTML confirmada:
 *   div[class=lottoactivo|lagranjita|lottorey|...]   → contenedor por lotería
 *     h3                                              → nombre lotería "Lotto Rey"
 *     (por cada sorteo):
 *       h4                                            → "34 Venado"
 *       h5                                            → "08:00 AM"
 */
class LoteriaDeHoyScraper @Inject constructor(
    private val client: OkHttpClient
) : ResultadoScraper {

    override fun getNombreFuente() = "loteriadehoy"
    override fun getUrl() = "https://loteriadehoy.com/animalitos/resultados/"

    // Mapa de clases CSS conocidas a identificador de lotería
    private val clasesLoterias = listOf(
        "guachamoactivo", "guacharo", "guacharoactivo",
        "guacharito", "guacharitomillonario",
        "lottoactivo", "lotto_activo",
        "lagranjita", "granjita",
        "lottorey", "lotto_rey",
        "selvaplus", "selva_plus", "selva"
    )

    override suspend fun obtenerResultados(fecha: LocalDate): List<ResultadoSorteo> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(getUrl()).build()
            val html = client.newCall(request).execute().use { it.body?.string() ?: "" }
            if (html.isBlank()) return@withContext emptyList()

            val doc = Jsoup.parse(html)
            val resultados = mutableListOf<ResultadoSorteo>()

            // Estrategia 1: buscar por clases conocidas
            for (clase in clasesLoterias) {
                val bloque = doc.selectFirst("div.$clase, section.$clase") ?: continue
                val h3 = bloque.selectFirst("h3")?.text()?.trim() ?: clase
                val loteria = ScraperUtils.identificarLoteria(h3)
                    ?: ScraperUtils.identificarLoteria(clase)
                    ?: continue

                val h4s = bloque.select("h4")
                val h5s = bloque.select("h5")

                // h4 y h5 están pareados por índice
                for (i in h4s.indices) {
                    val textoAnimal = h4s[i].text().trim()  // "34 Venado"
                    val textoHora = h5s.getOrNull(i)?.text()?.trim() ?: continue

                    val hora = ScraperUtils.normalizarHora(textoHora) ?: continue
                    if (hora !in loteria.horarios) continue

                    // Parsear "34 Venado" → número=34
                    val partes = textoAnimal.split(" ", limit = 2)
                    val numero = partes[0].trim().toIntOrNull() ?: continue
                    if (!ScraperUtils.esNumeroValido(numero, loteria)) continue

                    if (resultados.none { it.loteria == loteria && it.hora == hora }) {
                        resultados.add(
                            ScraperUtils.buildResultado(numero, loteria, hora, fecha, getNombreFuente())
                        )
                    }
                }
            }

            // Estrategia 2: si la anterior no encontró nada, buscar por h3 genérico
            if (resultados.isEmpty()) {
                val h3s = doc.select("h3")
                for (h3 in h3s) {
                    val loteria = ScraperUtils.identificarLoteria(h3.text()) ?: continue
                    val bloque = h3.parent() ?: continue

                    val h4s = bloque.select("h4")
                    val h5s = bloque.select("h5")

                    for (i in h4s.indices) {
                        val textoAnimal = h4s[i].text().trim()
                        val textoHora = h5s.getOrNull(i)?.text()?.trim() ?: continue
                        val hora = ScraperUtils.normalizarHora(textoHora) ?: continue
                        if (hora !in loteria.horarios) continue

                        val numero = textoAnimal.split(" ").firstOrNull()?.toIntOrNull() ?: continue
                        if (!ScraperUtils.esNumeroValido(numero, loteria)) continue

                        if (resultados.none { it.loteria == loteria && it.hora == hora }) {
                            resultados.add(
                                ScraperUtils.buildResultado(numero, loteria, hora, fecha, getNombreFuente())
                            )
                        }
                    }
                }
            }

            resultados
        }
}
