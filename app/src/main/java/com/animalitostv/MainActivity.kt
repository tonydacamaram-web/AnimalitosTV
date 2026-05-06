package com.animalitostv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.animalitostv.data.repository.ConfiguracionRepository
import com.animalitostv.service.AdHttpServer
import com.animalitostv.service.CleanupWorker
import com.animalitostv.service.ScrapingWorker
import com.animalitostv.ui.navigation.AnimalitosNavGraph
import com.animalitostv.ui.theme.AnimalitosTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var configRepository: ConfiguracionRepository

    private var adHttpServer: AdHttpServer? = null
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configurarPantalla()
        solicitarPermisoStorage()
        programarWorkers()

        setContent {
            AnimalitosTheme {
                val navController = rememberNavController()
                AnimalitosNavGraph(navController = navController)
            }
        }

        iniciarServidorAds()
    }

    private fun configurarPantalla() {
        // Pantalla siempre encendida — se puede aplicar antes del DecorView
        val pantallaEncendida = runBlocking { configRepository.getPantallaEncendida() }
        if (pantallaEncendida) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Modo inmersivo — requiere que el DecorView exista; se difiere al siguiente frame
        window.decorView.post {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(
                        android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.navigationBars()
                    )
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }
        }
    }

    private fun solicitarPermisoStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    )
                }
            }
        }
    }

    private fun iniciarServidorAds() {
        try {
            adHttpServer = AdHttpServer().apply { start() }
            android.util.Log.i("MainActivity", "Servidor de ads iniciado en puerto 8080")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "No se pudo iniciar servidor de ads", e)
        }
    }

    override fun onDestroy() {
        adHttpServer?.stop()
        super.onDestroy()
    }

    private fun programarWorkers() {
        ScrapingWorker.programar(this)
        CleanupWorker.programar(this)
        // Ejecutar scraping inmediato al iniciar
        ScrapingWorker.ejecutarAhora(this)
    }

    override fun onResume() {
        super.onResume()
        registrarMonitorConexion()
    }

    override fun onPause() {
        super.onPause()
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    private fun registrarMonitorConexion() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                ScrapingWorker.ejecutarAhora(this@MainActivity)
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }
}
