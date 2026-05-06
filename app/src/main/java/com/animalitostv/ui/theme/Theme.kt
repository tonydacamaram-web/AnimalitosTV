package com.animalitostv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TextoDorado,
    secondary = ColorGuacharo,
    background = FondoPrincipal,
    surface = FondoSecundario,
    onPrimary = FondoPrincipal,
    onSecondary = TextoPrincipal,
    onBackground = TextoPrincipal,
    onSurface = TextoPrincipal
)

@Composable
fun AnimalitosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
