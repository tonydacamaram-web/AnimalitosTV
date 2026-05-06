package com.animalitostv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.animalitostv.domain.model.ResultadoSorteo
import com.animalitostv.ui.theme.*
import com.animalitostv.util.Constants
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ResultadoCell(
    hora: String,
    resultado: ResultadoSorteo?,
    colorLoteria: Color,
    modifier: Modifier = Modifier
) {
    val esNuevo = resultado?.esNuevo == true

    var flashActivo by remember(resultado?.id) { mutableStateOf(esNuevo) }
    LaunchedEffect(resultado?.id) {
        if (flashActivo) {
            delay(4_000L)
            flashActivo = false
        }
    }

    val fondoCelda by animateColorAsState(
        targetValue = if (flashActivo) FlashNuevoAlpha else FondoCelda,
        animationSpec = tween(durationMillis = 800),
        label = "flash"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(fondoCelda)
            .border(
                width = if (flashActivo) 1.5.dp else 0.5.dp,
                color = if (flashActivo) FlashNuevo else BordeCelda,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fondo por animal (si existe el archivo de imagen)
        val tieneFondo = remember(resultado?.numeroAnimal) {
            resultado != null && File("${Constants.FONDOS_PATH}/${resultado.numeroAnimal}.png").exists()
        }
        if (tieneFondo && resultado != null) {
            AsyncImage(
                model = remember(resultado.numeroAnimal) {
                    File("${Constants.FONDOS_PATH}/${resultado.numeroAnimal}.png")
                },
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        if (resultado != null) {
            if (tieneFondo) {
                // Con fondo: hora anclada al borde inferior para no colisionar con el diseño
                Text(
                    text = hora,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                )
            } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Hora pequeña arriba (solo cuando no hay fondo)
                Text(
                    text = hora,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorLoteria.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Imagen del animal y textos
                val imagenFile = File("${Constants.ANIMALES_PATH}/${resultado.numeroAnimal}.png")
                if (imagenFile.exists()) {
                    AsyncImage(
                        model = imagenFile,
                        contentDescription = resultado.nombreAnimal,
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = String.format("%02d", resultado.numeroAnimal),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (flashActivo) FlashNuevo else TextoPrincipal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = resultado.nombreAnimal,
                        fontSize = 8.sp,
                        color = TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = String.format("%02d", resultado.numeroAnimal),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (flashActivo) FlashNuevo else colorLoteria,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = resultado.nombreAnimal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextoPrincipal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 11.sp
                    )
                }
            }
            } // cierre else (sin fondo)
        } else {
            // Celda vacía
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = hora,
                    fontSize = 8.sp,
                    color = TextoPendiente,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "—",
                    fontSize = 20.sp,
                    color = TextoPendiente,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
