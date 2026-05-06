package com.animalitostv.di

import com.animalitostv.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(Constants.SCRAPING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.SCRAPING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", Constants.USER_AGENT)
                    .header("Accept-Language", "es-VE,es;q=0.9")
                    .build()
                chain.proceed(request)
            }
            .build()
}
