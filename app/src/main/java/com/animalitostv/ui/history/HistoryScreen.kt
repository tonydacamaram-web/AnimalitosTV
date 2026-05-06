package com.animalitostv.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animalitostv.domain.model.Loteria
import com.animalitostv.ui.components.ResultadoCell
import com.animalitostv.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoPrincipal, FondoSecundario)))
    ) {
        // ── HEADER ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(FondoHeader)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextoDorado)
            }
            Text(
                "Historial de Resultados",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextoDorado,
                modifier = Modifier.weight(1f)
            )
            // Navegación de fecha
            IconButton(onClick = viewModel::diaAnterior) {
                Icon(Icons.Default.ChevronLeft, "Día anterior", tint = TextoPrincipal)
            }
            val fmt = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale("es", "VE"))
            Text(
                state.fecha.format(fmt),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal,
                modifier = Modifier.widthIn(min = 180.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = viewModel::diaSiguiente,
                enabled = state.fecha.isBefore(LocalDate.now())
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    "Día siguiente",
                    tint = if (state.fecha.isBefore(LocalDate.now())) TextoPrincipal else TextoSecundario
                )
            }
            IconButton(onClick = viewModel::irAHoy) {
                Icon(Icons.Default.Today, "Hoy", tint = TextoDorado)
            }
        }

        if (state.cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextoDorado)
            }
        } else {
            // ── GRILLA HISTÓRICA ─────────────────────────────────────────────
            // Cabecera de columnas
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
                            .border(0.5.dp, loteria.color.copy(alpha = 0.5f))
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

            // Filas
            val maxFilas = Loteria.todas.maxOf { it.horarios.size }
            val filas = (0 until maxFilas).toList()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp)
            ) {
                items(filas) { filaIndex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    ) {
                        Loteria.todas.forEach { loteria ->
                            val horario = loteria.horarios.getOrNull(filaIndex)
                            val resultado = horario?.let { h -> state.grilla[loteria]?.get(h) }
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
        }
    }
}
