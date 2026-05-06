package com.animalitostv.di

import android.content.Context
import androidx.room.Room
import com.animalitostv.data.local.AnimalitosDatabase
import com.animalitostv.data.local.dao.ConfiguracionDao
import com.animalitostv.data.local.dao.LogErrorDao
import com.animalitostv.data.local.dao.ResultadoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AnimalitosDatabase =
        Room.databaseBuilder(
            context,
            AnimalitosDatabase::class.java,
            "animalitos.db"
        ).build()

    @Provides
    fun provideResultadoDao(db: AnimalitosDatabase): ResultadoDao = db.resultadoDao()

    @Provides
    fun provideConfiguracionDao(db: AnimalitosDatabase): ConfiguracionDao = db.configuracionDao()

    @Provides
    fun provideLogErrorDao(db: AnimalitosDatabase): LogErrorDao = db.logErrorDao()
}
