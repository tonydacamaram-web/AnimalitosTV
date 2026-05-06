package com.animalitostv.data.remote

import com.animalitostv.data.repository.LogRepository
import com.animalitostv.data.repository.ResultadoRepository
import com.animalitostv.domain.model.ResultadoSorteo
import com.animalitostv.util.Constants
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScrapingOrchestrator @Inject constructor(
    private val tuAzarScraper: TuAzarScraper,
    private val lotoVenScraper: LotoVenScraper,
    private val loteriaDeHoyScraper: LoteriaDeHoyScraper,
    private val resultadoRepository: ResultadoRepository,
    private val logRepository: LogRepository
) {
    /**
     * Consulta TODAS las fuentes en paralelo y combina los resultados.
     * Los duplicados (misma lotería + hora) se descartan — gana el primero en llegar.
     * Esto maximiza la cobertura: si una fuente no tiene Lotto Rey,
     * otra puede tenerlo.
     */
    suspend fun ejecutar(fecha: LocalDate = LocalDate.now()): Boolean = coroutineScope {
        logRepository.registrar(
            tipo = "SCRAPING",
            mensaje = "Iniciando scraping paralelo para $fecha"
        )

        // Lanzar las 3 fuentes en paralelo
        val deferredTuazar = async { intentar(tuAzarScraper, fecha) }
        val deferredLotoven = async { intentar(lotoVenScraper, fecha) }
        val deferredLoteriadehoy = async { intentar(loteriaDeHoyScraper, fecha) }

        val deTuazar = deferredTuazar.await()
        val deLotoven = deferredLotoven.await()
        val deLoteriadehoy = deferredLoteriadehoy.await()

        // Combinar resultados eliminando duplicados (lotería + hora)
        val combinados = mutableListOf<ResultadoSorteo>()
        val vistos = mutableSetOf<String>() // "LOTERIA|hora"

        for (lista in listOf(deTuazar, deLotoven, deLoteriadehoy)) {
            for (r in lista) {
                val clave = "${r.loteria.id}|${r.hora}"
                if (vistos.add(clave)) {
                    combinados.add(r)
                }
            }
        }

        // Si estamos scrapeando el día actual, descartar resultados cuyo horario de sorteo
        // aún no ha transcurrido. Esto evita guardar resultados del día anterior que los
        // sitios web siguen mostrando al inicio de un nuevo día operativo.
        val ahoraVE = ZonedDateTime.now(Constants.ZONA_HORARIA_VE)
        val hoyVE = ahoraVE.toLocalDate()
        val aGuardar = if (fecha == hoyVE) {
            val horaActualVE = ahoraVE.toLocalTime()
            combinados.filter { resultado ->
                val sorteoTime = parsearHoraLocal(resultado.hora)
                sorteoTime == null || !sorteoTime.isAfter(horaActualVE.plusMinutes(5))
            }
        } else {
            combinados
        }

        if (aGuardar.isNotEmpty()) {
            resultadoRepository.guardarTodos(aGuardar)
            val descartados = combinados.size - aGuardar.size
            logRepository.registrar(
                tipo = "SCRAPING",
                mensaje = "OK: ${aGuardar.size} resultados guardados" +
                    (if (descartados > 0) ", $descartados descartados (sorteo aún no ocurrido)" else "") +
                    " (tuazar=${deTuazar.size}, lotoven=${deLotoven.size}, loteriadehoy=${deLoteriadehoy.size})"
            )
            true
        } else {
            logRepository.registrar(
                tipo = "SCRAPING",
                mensaje = "0 resultados válidos para $fecha — todos los sorteos son futuros o las fuentes no devolvieron datos"
            )
            false
        }
    }

    private fun parsearHoraLocal(hora: String): LocalTime? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            LocalTime.parse(hora.uppercase(Locale.US), formatter)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun intentar(
        scraper: ResultadoScraper,
        fecha: LocalDate
    ): List<ResultadoSorteo> {
        return try {
            val resultados = scraper.obtenerResultados(fecha)
            if (resultados.isEmpty()) {
                val htmlResumen = if (scraper is TuAzarScraper) scraper.ultimoHtmlResumen.take(200) else ""
                logRepository.registrar(
                    tipo = "PARSE",
                    mensaje = "0 resultados en ${scraper.getNombreFuente()}. $htmlResumen",
                    fuente = scraper.getNombreFuente()
                )
            }
            resultados
        } catch (e: Exception) {
            logRepository.registrar(
                tipo = "SCRAPING",
                mensaje = "Error en ${scraper.getNombreFuente()}: ${e.message}",
                fuente = scraper.getNombreFuente()
            )
            emptyList()
        }
    }
}
