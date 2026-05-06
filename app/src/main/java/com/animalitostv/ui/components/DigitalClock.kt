package com.animalitostv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalitostv.ui.theme.TextoDorado
import com.animalitostv.ui.theme.TextoSecundario
import com.animalitostv.util.Constants
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DigitalClock(
    reloj24h: Boolean = true,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(ZonedDateTime.now(Constants.ZONA_HORARIA_VE)) }

    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now(Constants.ZONA_HORARIA_VE)
            delay(1_000L)
        }
    }

    val horaFormatter = if (reloj24h)
        DateTimeFormatter.ofPattern("HH:mm:ss")
    else
        DateTimeFormatter.ofPattern("hh:mm:ss a")

    val fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = now.format(horaFormatter),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextoDorado
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = now.format(fechaFormatter),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = TextoSecundario
        )
    }
}
