package com.animalitostv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.wrapContentWidth
import com.animalitostv.domain.model.EstadisticasLoteria
import com.animalitostv.ui.theme.FondoTicker
import com.animalitostv.ui.theme.TextoDorado
import com.animalitostv.ui.theme.TextoPrincipal

@Composable
fun StatsTicker(
    estadisticas: List<EstadisticasLoteria>,
    mensajesPersonalizados: List<String> = emptyList(),
    velocidad: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val texto = remember(estadisticas, mensajesPersonalizados) {
        buildTextoTicker(estadisticas, mensajesPersonalizados)
    }
    if (texto.isBlank()) return

    var containerWidth by remember { mutableFloatStateOf(1920f) }
    var textWidth by remember { mutableFloatStateOf(0f) }

    // Velocidad de desplazamiento: píxeles por segundo
    val pixelesPorSegundo = 80f * velocidad
    val duracionMs = if (pixelesPorSegundo > 0) {
        ((containerWidth + textWidth) / pixelesPorSegundo * 1000).toInt().coerceAtLeast(5000)
    } else 15_000

    val offsetX = rememberInfiniteTransition(label = "ticker").animateFloat(
        initialValue = containerWidth,
        targetValue = -textWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = duracionMs,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "tickerOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(FondoTicker)
            .clipToBounds()
            .onGloballyPositioned { containerWidth = it.size.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = texto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = TextoPrincipal,
            softWrap = false,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .offset(x = offsetX.value.dp)
                .onGloballyPositioned { textWidth = it.size.width.toFloat() }
                .padding(horizontal = 8.dp),
            maxLines = 1
        )
    }
}

private fun buildTextoTicker(
    estadisticas: List<EstadisticasLoteria>,
    mensajesPersonalizados: List<String> = emptyList()
): String {
    // Un item corto por tipo de estadística por lotería (no un bloque gigante por lotería)
    val items = mutableListOf<String>()
    estadisticas.forEach { est ->
        val nombre = est.loteria.displayName.uppercase()

        if (est.calientesHoy.isNotEmpty()) {
            val top = est.calientesHoy.take(3).joinToString(", ") { (a, c) ->
                "${String.format("%02d", a.numero)}-${a.nombre} ×$c"
            }
            items.add("🔥 $nombre: $top")
        }

        if (est.friosSemana.isNotEmpty()) {
            val top = est.friosSemana.take(3).joinToString(", ") { a ->
                "${String.format("%02d", a.numero)}-${a.nombre}"
            }
            items.add("❄️ $nombre fríos: $top")
        }

        est.rachaActual?.let { (a, n) ->
            items.add("⏳ $nombre: ${a.nombre} lleva $n sorteos sin salir")
        }
    }

    val msgs = mensajesPersonalizados.map { "📢 $it" }

    if (items.isEmpty() && msgs.isEmpty()) return ""
    if (msgs.isEmpty()) return items.joinToString("     |     ")
    if (items.isEmpty()) return msgs.joinToString("     |     ")

    // Distribuir mensajes uniformemente a lo largo de los items de stats
    val result = mutableListOf<String>()
    val step = items.size.toDouble() / msgs.size   // stats entre cada mensaje
    var msgIdx = 0

    items.forEachIndexed { i, stat ->
        result.add(stat)
        // Insertar mensaje cuando hemos cubierto el siguiente umbral
        if (msgIdx < msgs.size && (i + 1) >= ((msgIdx + 1) * step).toInt().coerceAtLeast(1)) {
            result.add(msgs[msgIdx++])
        }
    }
    // Mensajes sobrantes (si hay más mensajes que stats)
    while (msgIdx < msgs.size) result.add(msgs[msgIdx++])

    return result.joinToString("     |     ")
}
