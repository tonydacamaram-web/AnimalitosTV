package com.animalitostv.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.animalitostv.domain.model.Loteria
import com.animalitostv.ui.components.*
import com.animalitostv.ui.theme.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Recargar configuración cada vez que la pantalla vuelve a ser visible
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.recargarConfiguracion()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(FondoPrincipal, FondoSecundario))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── HEADER ──────────────────────────────────────────────────────
            Header(
                nombreAgencia = state.nombreAgencia,
                reloj24h = state.reloj24h,
                hayConexion = state.hayConexion,
                ultimaActualizacion = state.ultimaActualizacion,
                tasaBcv = state.tasaBcv,
                onConfigClick = onNavigateToConfig
            )

            // ── CONTENIDO PRINCIPAL (panel ad + grilla) ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Panel publicitario lateral izquierdo
                val panelWeight = state.adPanelAnchoPercent / 100f
                val grillaWeight = 1f - panelWeight

                AdPanel(
                    carpeta = state.adCarpeta,
                    rotacionSegundos = state.adRotacionSegundos,
                    audioHabilitado = state.adAudio,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(panelWeight)
                )

                // Divisor vertical
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(BordeGlow)
                )

                // Grilla de resultados
                GrillaResultados(
                    grilla = state.grilla,
                    onHistorialClick = onNavigateToHistory,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(grillaWeight)
                )
            }

            // ── TICKER DE ESTADÍSTICAS ─────────────────────────────────────
            StatsTicker(
                estadisticas = state.estadisticas,
                mensajesPersonalizados = state.mensajesPersonalizados,
                velocidad = state.tickerVelocidad,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Header(
    nombreAgencia: String,
    reloj24h: Boolean,
    hayConexion: Boolean,
    ultimaActualizacion: LocalDateTime?,
    tasaBcv: Double?,
    onConfigClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF0F0F23), Color(0xFF1A1A2E)))
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + nombre de la agencia juntos
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val logoFile = remember { java.io.File(com.animalitostv.util.Constants.LOGO_PATH) }
            if (logoFile.exists()) {
                AsyncImage(
                    model = logoFile,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(max = 160.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            Text(
                text = nombreAgencia,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoDorado,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Indicador sin conexión
        if (!hayConexion) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalWifiOff,
                    contentDescription = "Sin conexión",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                val fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm")
                Text(
                    text = ultimaActualizacion?.format(fmt)?.let { "Últ: $it" } ?: "Sin conexión",
                    fontSize = 11.sp,
                    color = Color(0xFFFF5252)
                )
            }
        }

        // Tasa BCV
        if (tasaBcv != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text(
                    text = "BCV",
                    fontSize = 10.sp,
                    color = TextoSecundario,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Bs. %,.2f".format(tasaBcv),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Reloj
        DigitalClock(
            reloj24h = reloj24h,
            modifier = Modifier.padding(end = 12.dp)
        )

        // Botón discreto de configuración (doble tap en práctica real)
        IconButton(
            onClick = onConfigClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configuración",
                tint = TextoSecundario.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GrillaResultados(
    grilla: Map<Loteria, Map<String, com.animalitostv.domain.model.ResultadoSorteo?>>,
    onHistorialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Cabecera de columnas (nombres de loterías)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(FondoHeader)
        ) {
            Loteria.todas.forEach { loteria ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(
                            width = 0.5.dp,
                            color = loteria.color.copy(alpha = 0.5f)
                        )
                        .background(loteria.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loteria.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = loteria.color,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }

        // Filas de resultados con auto-scroll display
        val maxFilas = Loteria.todas.maxOf { it.horarios.size }
        val listState = rememberLazyListState()

        // Primera y última fila con al menos un resultado
        val primeraFila = remember(grilla) {
            (0 until maxFilas).firstOrNull { filaIndex ->
                Loteria.todas.any { loteria ->
                    val horario = loteria.horarios.getOrNull(filaIndex)
                    horario != null && grilla[loteria]?.get(horario) != null
                }
            } ?: 0
        }
        val ultimaFila = remember(grilla) {
            (maxFilas - 1 downTo 0).firstOrNull { filaIndex ->
                Loteria.todas.any { loteria ->
                    val horario = loteria.horarios.getOrNull(filaIndex)
                    horario != null && grilla[loteria]?.get(horario) != null
                }
            } ?: 0
        }

        // Auto-scroll display: ciclo continuo del primer al último sorteo.
        // Pausa 20s en la fila cuyo horario coincide con la hora actual; 3s en el resto.
        LaunchedEffect(primeraFila, ultimaFila) {
            if (ultimaFila <= primeraFila) return@LaunchedEffect

            listState.scrollToItem(primeraFila)

            while (true) {
                for (fila in (primeraFila + 1)..ultimaFila) {
                    val pausa = if (filaEsHoraActual(fila, maxFilas)) 20_000L else 3_000L
                    delay(pausa)
                    listState.animateScrollToItem(index = fila, scrollOffset = 0)
                }
                delay(3_000L)
                listState.scrollToItem(primeraFila)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 2.dp)
        ) {
            items(maxFilas) { filaIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                ) {
                    Loteria.todas.forEach { loteria ->
                        val horario = loteria.horarios.getOrNull(filaIndex)
                        val resultado = horario?.let { grilla[loteria]?.get(it) }

                        ResultadoCell(
                            hora = horario ?: "",
                            resultado = resultado,
                            colorLoteria = loteria.color,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.dp)
                        )
                    }
                }
            }
        }

        // Botón historial (discreto, al final de la grilla)
        TextButton(
            onClick = onHistorialClick,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 8.dp, bottom = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Historial",
                tint = TextoSecundario.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Ver días anteriores",
                fontSize = 11.sp,
                color = TextoSecundario.copy(alpha = 0.5f)
            )
        }
    }
}

// ── Helpers de horario ────────────────────────────────────────────────────────

/**
 * Convierte "8:00 AM" / "12:30 PM" a minutos desde medianoche.
 */
private fun parsearHorario(horario: String): Int? = runCatching {
    val partes = horario.trim().split(" ")
    val hm = partes[0].split(":")
    var h = hm[0].toInt()
    val m = hm[1].toInt()
    val ampm = partes[1].uppercase()
    if (ampm == "PM" && h != 12) h += 12
    if (ampm == "AM" && h == 12) h = 0
    h * 60 + m
}.getOrNull()

/**
 * Devuelve true si la hora actual se encuentra dentro del bloque de tiempo
 * representado por [filaIndex]: desde el mínimo horario de esa fila hasta
 * el mínimo horario de la fila siguiente (exclusive).
 */
private fun filaEsHoraActual(filaIndex: Int, maxFilas: Int): Boolean {
    val ahoraMin = LocalTime.now().let { it.hour * 60 + it.minute }

    val minEsta = Loteria.todas
        .mapNotNull { it.horarios.getOrNull(filaIndex) }
        .mapNotNull { parsearHorario(it) }
        .minOrNull() ?: return false

    val minSiguiente = if (filaIndex + 1 < maxFilas) {
        Loteria.todas
            .mapNotNull { it.horarios.getOrNull(filaIndex + 1) }
            .mapNotNull { parsearHorario(it) }
            .minOrNull()
    } else null

    return ahoraMin >= minEsta && (minSiguiente == null || ahoraMin < minSiguiente)
}
