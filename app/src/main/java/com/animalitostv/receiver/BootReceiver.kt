package com.animalitostv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.animalitostv.MainActivity
import com.animalitostv.data.local.dao.ConfiguracionDao
import com.animalitostv.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var configuracionDao: ConfiguracionDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != "com.htc.intent.action.QUICKBOOT_POWERON") return

        CoroutineScope(Dispatchers.IO).launch {
            val autoInicio = configuracionDao.obtener(Constants.Config.AUTO_INICIO)?.valor?.toBoolean() ?: false
            if (autoInicio) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
