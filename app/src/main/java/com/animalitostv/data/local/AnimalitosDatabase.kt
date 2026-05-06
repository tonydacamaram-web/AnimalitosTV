package com.animalitostv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.animalitostv.data.local.dao.ConfiguracionDao
import com.animalitostv.data.local.dao.LogErrorDao
import com.animalitostv.data.local.dao.ResultadoDao
import com.animalitostv.data.local.entity.ConfiguracionEntity
import com.animalitostv.data.local.entity.LogErrorEntity
import com.animalitostv.data.local.entity.ResultadoEntity

@Database(
    entities = [
        ResultadoEntity::class,
        ConfiguracionEntity::class,
        LogErrorEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AnimalitosDatabase : RoomDatabase() {
    abstract fun resultadoDao(): ResultadoDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun logErrorDao(): LogErrorDao
}
