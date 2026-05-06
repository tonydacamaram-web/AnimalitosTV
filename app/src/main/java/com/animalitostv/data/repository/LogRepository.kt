package com.animalitostv.data.repository

import com.animalitostv.data.local.dao.LogErrorDao
import com.animalitostv.data.local.entity.LogErrorEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val dao: LogErrorDao
) {
    fun observarLogs(): Flow<List<LogErrorEntity>> = dao.observarLogs()

    suspend fun registrar(
        tipo: String,
        mensaje: String,
        fuente: String? = null,
        stackTrace: String? = null
    ) {
        dao.insertar(
            LogErrorEntity(
                timestamp = LocalDateTime.now(),
                tipo = tipo,
                mensaje = mensaje,
                fuente = fuente,
                stackTrace = stackTrace
            )
        )
    }

    suspend fun limpiarAntiguos(diasRetener: Long) {
        val limite = LocalDateTime.now().minusDays(diasRetener)
        dao.eliminarAntiguos(limite)
    }

    suspend fun limpiarTodo() = dao.limpiarTodo()
}
