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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animalitostv.ui.theme.*
import com.animalitostv.util.Constants
import com.animalitostv.util.StoragePaths
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConfigTextField(
                            label = "Nombre de la agencia",
                            value = state.nombreAgencia,
                            onValueChange = viewModel::setNombreAgencia
                        )
                        TecladoNombreAgencia(
                            valor = state.nombreAgencia,
                            onCambio = viewModel::setNombreAgencia
                        )
                        Button(
                            onClick = { viewModel.guardar() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Guardar y volver",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
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
                item { AvisoPermisoAlmacenamiento() }
                item {
                    val ip = viewModel.ipLocal
                    val puerto = Constants.AD_SERVER_PORT
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
                                "Nombre fijo (Windows, macOS, iPhone/iPad):",
                                fontSize = 11.sp,
                                color = TextoSecundario
                            )
                            Text(
                                "http://${Constants.MDNS_SERVICE_NAME}.local:$puerto",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoDorado
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Respaldo por IP (Android y Smart TV):",
                                fontSize = 11.sp,
                                color = TextoSecundario
                            )
                            Text(
                                "http://$ip:$puerto",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoDorado
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "El nombre .local no se resuelve en navegadores de Android ni en Smart TV: usa la IP en esos casos.",
                                fontSize = 9.sp,
                                color = TextoSecundario
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Si animalitostv.local no responde, fija el nombre del dispositivo del TV como \"animalitostv\" en Ajustes y reconecta la red.",
                                fontSize = 9.sp,
                                color = TextoSecundario
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

// ── Teclado en pantalla para Smart TV ────────────────────────────────────────

/**
 * Teclado alfanumérico navegable con el control remoto (D-pad).
 *
 * En muchos televisores el teclado virtual del sistema no escribe en los
 * OutlinedTextField de Compose, por lo que el nombre de la agencia nunca se
 * actualizaba. Estos botones modifican el estado directamente.
 */
@Composable
private fun TecladoNombreAgencia(
    valor: String,
    onCambio: (String) -> Unit
) {
    val filas = listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "Ñ", "O", "P", "Q"),
        listOf("R", "S", "T", "U", "V", "W"),
        listOf("X", "Y", "Z", "&", ",", "."),
        listOf("0", "1", "2", "3", "4", "5"),
        listOf("6", "7", "8", "9", "-", "←")
    )

    // Empieza en mayúsculas y, tras cada letra, vuelve a minúsculas (como un
    // teclado de celular). El botón MAYÚS/minús permite forzar la próxima letra.
    var mayusculas by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Teclado en pantalla: selecciona con el control remoto",
            fontSize = 10.sp,
            color = TextoSecundario
        )
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fila.forEach { tecla ->
                    val etiqueta = if (tecla.length == 1 && tecla[0].isLetter() && !mayusculas) {
                        tecla.lowercase()
                    } else {
                        tecla
                    }
                    OutlinedButton(
                        onClick = {
                            when {
                                tecla == "←" -> onCambio(valor.dropLast(1))
                                valor.length < 60 -> {
                                    onCambio(valor + etiqueta)
                                    if (tecla.length == 1 && tecla[0].isLetter()) {
                                        mayusculas = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(44.dp, 38.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
                    ) {
                        Text(etiqueta, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                onClick = { mayusculas = !mayusculas },
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoDorado)
            ) {
                Text(
                    if (mayusculas) "MAYÚS" else "minús",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { if (valor.length < 60) onCambio("$valor ") },
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
            ) {
                Text("ESPACIO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onCambio("") },
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
            ) {
                Text("LIMPIAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Permiso de almacenamiento ────────────────────────────────────────────────

@Composable
private fun AvisoPermisoAlmacenamiento() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var concedido by remember { mutableStateOf(hayPermisoAlmacenamiento()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                concedido = hayPermisoAlmacenamiento()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!concedido) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1515)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Sin permiso de almacenamiento",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80)
                    )
                    Text(
                        "El TV no puede guardar la publicidad que subes desde el panel web. " +
                            "Concede \"Acceso a todos los archivos\" y vuelve a esta pantalla.",
                        fontSize = 11.sp,
                        color = TextoPrincipal
                    )
                    OutlinedButton(
                        onClick = { abrirAjustesAlmacenamiento(context) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoDorado)
                    ) {
                        Text(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                "Abrir ajustes de almacenamiento"
                            else "Abrir ajustes de la app"
                        )
                    }
                }
            }
        }
        Text(
            "Carpeta de publicidad en el TV: ${StoragePaths.ads.absolutePath}",
            fontSize = 10.sp,
            color = TextoSecundario
        )
    }
}

private fun hayPermisoAlmacenamiento(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }

private fun abrirAjustesAlmacenamiento(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val directo = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        val abierto = runCatching { context.startActivity(directo) }.isSuccess
        if (!abierto) {
            // Algunos TV boxes no tienen la pantalla específica por app.
            runCatching {
                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    } else {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }
}
