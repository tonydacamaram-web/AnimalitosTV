package com.animalitostv.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.animalitostv.data.repository.LogRepository
import com.animalitostv.data.repository.ResultadoRepository
import com.animalitostv.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val resultadoRepository: ResultadoRepository,
    private val logRepository: LogRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val fechaLimiteResultados = LocalDate.now().minusDays(Constants.DB_RETENTION_DAYS)
            resultadoRepository.limpiarAntiguos(fechaLimiteResultados)
            logRepository.limpiarAntiguos(Constants.LOG_RETENTION_DAYS)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "limpieza_diaria"

        fun programar(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calcularDelayHasta2AM(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calcularDelayHasta2AM(): Long {
            val ahora = java.time.LocalDateTime.now(Constants.ZONA_HORARIA_VE)
            var objetivo = ahora.toLocalDate().atTime(2, 0)
            if (ahora.isAfter(objetivo)) objetivo = objetivo.plusDays(1)
            return java.time.Duration.between(ahora, objetivo).toMillis().coerceAtLeast(0)
        }
    }
}
