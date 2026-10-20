package com.animalitostv.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Rutas únicas del contenido compartido (publicidad, logo, fondos, animales y mensajes).
 *
 * Se resuelve con [Environment.getExternalStorageDirectory] en lugar del literal "/sdcard"
 * porque algunos TV boxes y Android TVs no exponen ese enlace o lo devuelven como una ruta
 * no escribible, aunque el permiso de almacenamiento esté concedido.
 */
object StoragePaths {

    /** Raíz del almacenamiento externo compartido (normalmente /storage/emulated/0). */
    private val raiz: File
        get() = runCatching { Environment.getExternalStorageDirectory() }
            .getOrNull() ?: File("/sdcard")

    val base: File get() = File(raiz, "AnimalitosTV")
    val ads: File get() = File(raiz, "AnimalitosAds")
    val animales: File get() = File(base, "animales")
    val fondos: File get() = File(base, "fondos")
    val logo: File get() = File(base, "logo.png")
    val mensajes: File get() = File(base, "mensajes.txt")

    /** Crea la carpeta si no existe. Devuelve true si quedó disponible para escritura. */
    fun asegurar(carpeta: File): Boolean {
        if (carpeta.isDirectory) return true
        runCatching { carpeta.mkdirs() }
        return carpeta.isDirectory
    }

    /**
     * Indica si la app puede escribir en el almacenamiento compartido.
     *
     * - API 30+: requiere "Acceso a todos los archivos" (MANAGE_EXTERNAL_STORAGE).
     * - API 23-29: requiere el permiso runtime WRITE_EXTERNAL_STORAGE.
     * - API 21-22: se concede al instalar.
     */
    fun permisoEscritura(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        else -> true
    }
}
