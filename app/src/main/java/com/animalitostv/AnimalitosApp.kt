package com.animalitostv

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AnimalitosApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // NanoHTTPD escribe los archivos multipart en java.io.tmpdir, que en Android/TV
        // suele apuntar a /data/local/tmp (no escribible por la app). Lo redirigimos al
        // cache privado para que las subidas del panel web funcionen.
        runCatching { System.setProperty("java.io.tmpdir", cacheDir.absolutePath) }
            .onFailure { Log.w("AnimalitosApp", "No se pudo ajustar java.io.tmpdir", it) }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
