package com.animalitostv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.Manifest
import android.content.pm.PackageManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.animalitostv.data.repository.ConfiguracionRepository
import com.animalitostv.data.repository.LogRepository
import com.animalitostv.service.AdHttpServer
import com.animalitostv.service.CleanupWorker
import com.animalitostv.service.DeviceNameHelper
import com.animalitostv.service.MdnsAdvertiser
import com.animalitostv.service.ScrapingWorker
import com.animalitostv.ui.navigation.AnimalitosNavGraph
import com.animalitostv.ui.theme.AnimalitosTheme
import com.animalitostv.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var configRepository: ConfiguracionRepository

    @Inject
    lateinit var logRepository: LogRepository

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (!concedido) {
                lifecycleScope.launch {
                    logRepository.registrar(
                        tipo = "ALMACENAMIENTO",
                        mensaje = "El usuario denegó WRITE_EXTERNAL_STORAGE; las subidas del panel web fallarán",
                        fuente = "MainActivity"
                    )
                }
            }
        }

    private var adHttpServer: AdHttpServer? = null
    private var mdnsAdvertiser: MdnsAdvertiser? = null
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
            // API 30+: el acceso al almacenamiento compartido se concede en Ajustes.
            if (!Environment.isExternalStorageManager()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    )
                }.onFailure {
                    android.util.Log.e("MainActivity", "No se pudo abrir los ajustes de almacenamiento", it)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23-29: permiso runtime clásico.
            val permiso = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val concedido = ContextCompat.checkSelfPermission(this, permiso) ==
                PackageManager.PERMISSION_GRANTED
            if (!concedido) {
                storagePermissionLauncher.launch(permiso)
            }
        }
    }

    private fun iniciarServidorAds() {
        try {
            adHttpServer = AdHttpServer(
                port = Constants.AD_SERVER_PORT,
                contexto = applicationContext,
                registrarLog = { tipo, mensaje, stack ->
                    lifecycleScope.launch {
                        logRepository.registrar(tipo, mensaje, "AdHttpServer", stack)
                    }
                }
            ).apply { start() }
            android.util.Log.i("MainActivity", "Servidor de ads iniciado en puerto ${Constants.AD_SERVER_PORT}")

            // Fijar el hostname y publicar el servicio DNS-SD (mDNS → animalitostv.local)
            DeviceNameHelper.asegurarNombreDispositivo(this)
            mdnsAdvertiser = MdnsAdvertiser(this).also { it.register() }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "No se pudo iniciar servidor de ads", e)
            lifecycleScope.launch {
                logRepository.registrar(
                    tipo = "SERVIDOR",
                    mensaje = "No se pudo iniciar el servidor de ads: ${e.message}",
                    fuente = "MainActivity",
                    stackTrace = e.stackTraceToString()
                )
            }
        }
    }

    override fun onDestroy() {
        mdnsAdvertiser?.unregister()
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
