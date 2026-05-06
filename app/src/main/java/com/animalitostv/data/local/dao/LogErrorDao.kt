package com.animalitostv.data.local.dao

import androidx.room.*
import com.animalitostv.data.local.entity.LogErrorEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface LogErrorDao {

    @Insert
    suspend fun insertar(log: LogErrorEntity)

    @Query("SELECT * FROM logs_error ORDER BY timestamp DESC LIMIT 200")
    fun observarLogs(): Flow<List<LogErrorEntity>>

    @Query("SELECT * FROM logs_error ORDER BY timestamp DESC LIMIT 200")
    suspend fun obtenerLogs(): List<LogErrorEntity>

    @Query("DELETE FROM logs_error WHERE timestamp < :fechaLimite")
    suspend fun eliminarAntiguos(fechaLimite: LocalDateTime)

    @Query("DELETE FROM logs_error")
    suspend fun limpiarTodo()
}
