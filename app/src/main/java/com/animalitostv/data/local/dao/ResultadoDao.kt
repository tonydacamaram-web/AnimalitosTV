package com.animalitostv.data.local.dao

import androidx.room.*
import com.animalitostv.data.local.entity.ResultadoEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ResultadoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(resultado: ResultadoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodos(resultados: List<ResultadoEntity>)

    @Query("SELECT * FROM resultados WHERE fecha = :fecha ORDER BY loteria, hora")
    fun observarPorFecha(fecha: LocalDate): Flow<List<ResultadoEntity>>

    @Query("SELECT * FROM resultados WHERE fecha = :fecha ORDER BY loteria, hora")
    suspend fun obtenerPorFecha(fecha: LocalDate): List<ResultadoEntity>

    @Query("SELECT * FROM resultados WHERE loteria = :loteria AND fecha = :fecha ORDER BY hora")
    suspend fun obtenerPorLoteriayFecha(loteria: String, fecha: LocalDate): List<ResultadoEntity>

    @Query("SELECT * FROM resultados WHERE esNuevo = 1")
    suspend fun obtenerNuevos(): List<ResultadoEntity>

    @Query("UPDATE resultados SET esNuevo = 0 WHERE id = :id")
    suspend fun marcarVisto(id: Long)

    @Query("UPDATE resultados SET esNuevo = 0")
    suspend fun marcarTodosVistos()

    // Estadísticas: conteo de cada animal por lotería en rango de fechas
    @Query("""
        SELECT numeroAnimal, COUNT(*) as total
        FROM resultados
        WHERE loteria = :loteria AND fecha BETWEEN :desde AND :hasta
        GROUP BY numeroAnimal
        ORDER BY total DESC
    """)
    suspend fun contarAnimalesPorLoteria(
        loteria: String,
        desde: LocalDate,
        hasta: LocalDate
    ): List<AnimalConteo>

    // Limpieza automática: eliminar registros más antiguos que la fecha indicada
    @Query("DELETE FROM resultados WHERE fecha < :fechaLimite")
    suspend fun eliminarAntiguos(fechaLimite: LocalDate)

    @Query("SELECT COUNT(*) FROM resultados")
    suspend fun contarTotal(): Int
}

data class AnimalConteo(
    val numeroAnimal: Int,
    val total: Int
)
