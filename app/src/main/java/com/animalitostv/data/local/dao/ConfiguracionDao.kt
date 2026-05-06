package com.animalitostv.data.local.dao

import androidx.room.*
import com.animalitostv.data.local.entity.ConfiguracionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracionDao {

    @Query("SELECT * FROM configuracion WHERE clave = :clave")
    suspend fun obtener(clave: String): ConfiguracionEntity?

    @Query("SELECT * FROM configuracion WHERE clave = :clave")
    fun observar(clave: String): Flow<ConfiguracionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(config: ConfiguracionEntity)

    @Query("SELECT * FROM configuracion")
    suspend fun obtenerTodas(): List<ConfiguracionEntity>

    @Query("DELETE FROM configuracion WHERE clave = :clave")
    suspend fun eliminar(clave: String)
}
