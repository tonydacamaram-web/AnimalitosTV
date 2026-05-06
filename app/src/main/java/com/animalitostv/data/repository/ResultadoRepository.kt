package com.animalitostv.data.repository

import com.animalitostv.data.local.dao.AnimalConteo
import com.animalitostv.data.local.dao.ResultadoDao
import com.animalitostv.data.local.entity.ResultadoEntity
import com.animalitostv.domain.model.Loteria
import com.animalitostv.domain.model.ResultadoSorteo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultadoRepository @Inject constructor(
    private val dao: ResultadoDao
) {
    fun observarResultadosHoy(): Flow<List<ResultadoSorteo>> =
        dao.observarPorFecha(LocalDate.now()).map { lista ->
            lista.map { it.toDomain() }
        }

    fun observarResultadosPorFecha(fecha: LocalDate): Flow<List<ResultadoSorteo>> =
        dao.observarPorFecha(fecha).map { lista ->
            lista.map { it.toDomain() }
        }

    suspend fun obtenerPorFecha(fecha: LocalDate): List<ResultadoSorteo> =
        dao.obtenerPorFecha(fecha).map { it.toDomain() }

    suspend fun guardar(resultado: ResultadoSorteo): Boolean {
        val entity = resultado.toEntity()
        val rowId = dao.insertar(entity)
        return rowId > 0
    }

    suspend fun guardarTodos(resultados: List<ResultadoSorteo>) {
        dao.insertarTodos(resultados.map { it.toEntity() })
    }

    suspend fun obtenerNuevos(): List<ResultadoSorteo> =
        dao.obtenerNuevos().map { it.toDomain() }

    suspend fun marcarVisto(id: Long) = dao.marcarVisto(id)

    suspend fun contarAnimalesPorLoteria(
        loteria: Loteria,
        desde: LocalDate,
        hasta: LocalDate
    ): List<AnimalConteo> = dao.contarAnimalesPorLoteria(loteria.id, desde, hasta)

    suspend fun limpiarAntiguos(fechaLimite: LocalDate) =
        dao.eliminarAntiguos(fechaLimite)

    // Mappers
    private fun ResultadoEntity.toDomain(): ResultadoSorteo {
        val lot = Loteria.fromId(loteria) ?: Loteria.LOTTO_ACTIVO
        return ResultadoSorteo(
            id = id,
            loteria = lot,
            fecha = fecha,
            hora = hora,
            numeroAnimal = numeroAnimal,
            nombreAnimal = nombreAnimal,
            fuenteDatos = fuenteDatos,
            fechaRegistro = fechaRegistro,
            esNuevo = esNuevo
        )
    }

    private fun ResultadoSorteo.toEntity() = ResultadoEntity(
        id = id,
        loteria = loteria.id,
        fecha = fecha,
        hora = hora,
        numeroAnimal = numeroAnimal,
        nombreAnimal = nombreAnimal,
        fuenteDatos = fuenteDatos,
        fechaRegistro = fechaRegistro,
        esNuevo = esNuevo
    )
}
