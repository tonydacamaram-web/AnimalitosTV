package com.animalitostv.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animalitostv.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.guardado) {
        if (state.guardado) {
            viewModel.resetGuardado()
            onNavigateBack()
        }
    }

    // Si no está verificado, mostrar pantalla de PIN
    if (!state.pinVerificado) {
        PinScreen(
            pinIngresado = state.pinIngresado,
            errorPin = state.errorPin,
            esPrimerUso = state.primerUso,
            onDigito = viewModel::ingresarDigitoPin,
            onBorrar = viewModel::borrarDigitoPin,
            onConfirmar = viewModel::verificarPin,
            onCancelar = onNavigateBack
        )
        return
    }

    // Pantalla de configuración
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPrincipal)
    ) {
        // Panel izquierdo: opciones
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Toolbar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = TextoDorado)
                }
                Text(
                    "Configuración",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoDorado,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.guardar() }) {
                    Icon(Icons.Default.Check, "Guardar", tint = Color(0xFF43A047))
                }
            }

            Divider(color = BordeGlow, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SeccionTitulo("Branding") }
                item {
                    ConfigTextField(
                        label = "Nombre de la agencia",
                        value = state.nombreAgencia,
                        onValueChange = viewModel::setNombreAgencia
                    )
                }

                item { SeccionTitulo("Panel Publicitario") }
                item {
                    ConfigSlider(
                        label = "Ancho del panel: ${state.adPanelAncho}%",
                        value = state.adPanelAncho.toFloat(),
                        valueRange = 15f..35f,
                        onValueChange = { viewModel.setAdPanelAncho(it.toInt()) }
                    )
                }
                item {
                    val opciones = listOf(5, 10, 15, 20, 30)
                    ConfigDropdown(
                        label = "Rotación de imágenes",
                        opciones = opciones.map { "${it}s" },
                        seleccionado = "${state.adRotacionSegundos}s",
                        onSeleccion = { idx -> viewModel.setAdRotacion(opciones[idx]) }
                    )
                }
                item {
                    ConfigSwitch(
                        label = "Audio en videos",
                        valor = state.adAudio,
                        onChange = viewModel::setAdAudio
                    )
                }
                item {
                    ConfigTextField(
                        label = "Carpeta de publicidad",
                        value = state.adCarpeta,
                        onValueChange = viewModel::setAdCarpeta
                    )
                }

                item { SeccionTitulo("Display") }
                item {
                    ConfigSwitch(
                        label = "Reloj 24 horas",
                        valor = state.reloj24h,
                        onChange = viewModel::setReloj24h
                    )
                }
                item {
                    val velocidades = listOf(0.5f to "Lento", 1.0f to "Normal", 1.5f to "Rápido", 2.0f to "Muy rápido")
                    ConfigDropdown(
                        label = "Velocidad del ticker",
                        opciones = velocidades.map { it.second },
                        seleccionado = velocidades.minByOrNull { Math.abs(it.first - state.tickerVelocidad) }?.second ?: "Normal",
                        onSeleccion = { idx -> viewModel.setTickerVelocidad(velocidades[idx].first) }
                    )
                }

                item { SeccionTitulo("Gestión de Publicidad") }
                item {
                    val ip = viewModel.ipLocal
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Servidor web activo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF43A047)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Desde un navegador en la misma red:",
                                fontSize = 11.sp,
                                color = TextoSecundario
                            )
                            Text(
                                "http://$ip:8080",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoDorado
                            )
                        }
                    }
                }

                item { SeccionTitulo("Sistema") }
                item {
                    ConfigSwitch(
                        label = "Auto-inicio al encender",
                        valor = state.autoInicio,
                        onChange = viewModel::setAutoInicio
                    )
                }
                item {
                    ConfigSwitch(
                        label = "Mantener pantalla encendida",
                        valor = state.pantallaEncendida,
                        onChange = viewModel::setPantallaEncendida
                    )
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::forzarActualizacion,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF43A047))
                    ) {
                        Text("Forzar actualización ahora")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::limpiarBaseDatos,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                    ) {
                        Text("Limpiar base de datos")
                    }
                }
            }
        }

        // Divisor
        Box(Modifier.fillMaxHeight().width(1.dp).background(BordeGlow))

        // Panel derecho: logs
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                "Logs de errores",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextoDorado
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                if (state.logs.isEmpty()) {
                    item {
                        Text(
                            "Sin errores registrados",
                            color = TextoSecundario,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    items(state.logs) { log ->
                        LogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinScreen(
    pinIngresado: String,
    errorPin: Boolean,
    esPrimerUso: Boolean,
    onDigito: (String) -> Unit,
    onBorrar: () -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(FondoPrincipal),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(320.dp),
            colors = CardDefaults.cardColors(containerColor = FondoSecundario),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (esPrimerUso) "Bienvenido\nEstablece tu PIN" else "Ingresa tu PIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoDorado,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Puntos del PIN
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { i ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    if (i < pinIngresado.length) TextoDorado else BordeCelda,
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }

                if (errorPin) {
                    Text("PIN incorrecto", color = Color(0xFFE53935), fontSize = 13.sp)
                }

                // Teclado numérico
                val filas = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("←", "0", "✓")
                )
                filas.forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { tecla ->
                            OutlinedButton(
                                onClick = {
                                    when (tecla) {
                                        "←" -> onBorrar()
                                        "✓" -> onConfirmar()
                                        else -> onDigito(tecla)
                                    }
                                },
                                modifier = Modifier.size(72.dp, 44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (tecla == "✓") Color(0xFF43A047) else TextoPrincipal
                                )
                            ) {
                                Text(tecla, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                TextButton(onClick = onCancelar) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        }
    }
}

// ── Componentes auxiliares de configuración ─────────────────────────────────

@Composable
private fun SeccionTitulo(titulo: String) {
    Text(
        titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextoDorado.copy(alpha = 0.7f),
        letterSpacing = 2.sp
    )
}

@Composable
private fun ConfigTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoSecundario, fontSize = 12.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextoPrincipal,
            unfocusedTextColor = TextoPrincipal,
            focusedBorderColor = TextoDorado,
            unfocusedBorderColor = BordeCelda
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun ConfigSwitch(label: String, valor: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextoPrincipal, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = valor,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = TextoDorado, checkedTrackColor = TextoDorado.copy(0.3f))
        )
    }
}

@Composable
private fun ConfigSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label, color = TextoPrincipal, fontSize = 14.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = ((valueRange.endInclusive - valueRange.start) / 5 - 1).toInt(),
            colors = SliderDefaults.colors(thumbColor = TextoDorado, activeTrackColor = TextoDorado)
        )
    }
}

@Composable
private fun ConfigDropdown(
    label: String,
    opciones: List<String>,
    seleccionado: String,
    onSeleccion: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = TextoPrincipal, fontSize = 14.sp)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
            ) {
                Text(seleccionado)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(FondoSecundario)
            ) {
                opciones.forEachIndexed { idx, opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion, color = TextoPrincipal) },
                        onClick = { onSeleccion(idx); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: com.animalitostv.data.local.entity.LogErrorEntity) {
    val fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(FondoSecundario, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row {
            Text(
                log.timestamp.format(fmt),
                fontSize = 10.sp,
                color = TextoSecundario,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(8.dp))
            Text(
                log.tipo,
                fontSize = 10.sp,
                color = Color(0xFFE53935),
                fontWeight = FontWeight.Bold
            )
            log.fuente?.let {
                Spacer(Modifier.width(4.dp))
                Text("[$it]", fontSize = 10.sp, color = TextoSecundario)
            }
        }
        Text(log.mensaje, fontSize = 11.sp, color = TextoPrincipal, modifier = Modifier.padding(top = 2.dp))
    }
}
