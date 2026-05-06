package com.animalitostv.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.animalitostv.ui.theme.FondoPrincipal
import com.animalitostv.ui.theme.TextoSecundario
import kotlinx.coroutines.delay
import java.io.File

enum class TipoMedia { IMAGEN, VIDEO }

data class ArchivoMedia(val file: File, val tipo: TipoMedia)

@Composable
fun AdPanel(
    carpeta: String,
    rotacionSegundos: Int,
    audioHabilitado: Boolean,
    modifier: Modifier = Modifier
) {
    // Recarga la lista cada 10 segundos para detectar archivos nuevos subidos via HTTP
    var reloadTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(carpeta) {
        while (true) {
            delay(10_000L)
            reloadTick = System.currentTimeMillis()
        }
    }
    val archivos = remember(carpeta, reloadTick) { cargarArchivos(carpeta) }

    if (archivos.isEmpty()) {
        Box(
            modifier = modifier.background(FondoPrincipal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin publicidad\nAgregar archivos en\n$carpeta",
                color = TextoSecundario,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var indiceActual by remember { mutableIntStateOf(0) }
    val archivoActual = archivos[indiceActual % archivos.size]

    val avanzar = { indiceActual = (indiceActual + 1) % archivos.size }

    when (archivoActual.tipo) {
        TipoMedia.IMAGEN -> {
            LaunchedEffect(indiceActual) {
                delay(rotacionSegundos * 1000L)
                avanzar()
            }
            AsyncImage(
                model = archivoActual.file,
                contentDescription = null,
                modifier = modifier.background(FondoPrincipal),
                contentScale = ContentScale.Fit
            )
        }
        TipoMedia.VIDEO -> {
            VideoPlayer(
                file = archivoActual.file,
                audioHabilitado = audioHabilitado,
                modifier = modifier,
                onTerminado = avanzar
            )
        }
    }
}

@Composable
private fun VideoPlayer(
    file: File,
    audioHabilitado: Boolean,
    modifier: Modifier = Modifier,
    onTerminado: () -> Unit
) {
    val context = LocalContext.current

    // Player se crea y destruye junto con este composable
    // La clave `file` garantiza que se recrea un player limpio para cada archivo
    val exoPlayer = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            volume = if (audioHabilitado) 1f else 0f
            repeatMode = Player.REPEAT_MODE_OFF
            val item = MediaItem.fromUri(Uri.fromFile(file))
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    // Listener de fin de video
    var videoTerminado by remember(file) { mutableStateOf(false) }

    DisposableEffect(file) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && !videoTerminado) {
                    videoTerminado = true
                    onTerminado()
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!videoTerminado) {
                    videoTerminado = true
                    onTerminado()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Watchdog independiente: si el video lleva 60s sin terminar, saltar
    LaunchedEffect(file) {
        delay(60_000L)
        if (!videoTerminado) {
            videoTerminado = true
            onTerminado()
        }
    }

    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        update = { view ->
            view.player = exoPlayer
        },
        modifier = modifier
    )
}

private fun cargarArchivos(carpetaPath: String): List<ArchivoMedia> {
    val carpeta = File(carpetaPath)
    if (!carpeta.exists() || !carpeta.isDirectory) return emptyList()
    return carpeta.listFiles()
        ?.sortedBy { it.name }
        ?.mapNotNull { file ->
            when (file.extension.lowercase()) {
                "jpg", "jpeg", "png", "webp", "bmp" -> ArchivoMedia(file, TipoMedia.IMAGEN)
                "mp4", "avi", "mkv", "mov", "webm" -> ArchivoMedia(file, TipoMedia.VIDEO)
                else -> null
            }
        } ?: emptyList()
}
