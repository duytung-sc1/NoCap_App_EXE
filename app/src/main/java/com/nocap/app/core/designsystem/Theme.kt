package com.nocap.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.nocap.app.core.datastore.ReaderTheme

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

val SepiaColorScheme = lightColorScheme(
    primary = SepiaPrimary,
    onPrimary = SepiaOnPrimary,
    primaryContainer = SepiaPrimaryContainer,
    onPrimaryContainer = SepiaOnPrimaryContainer,
    secondary = SepiaSecondary,
    onSecondary = SepiaOnSecondary,
    background = SepiaBackground,
    onBackground = SepiaOnBackground,
    surface = SepiaSurface,
    onSurface = SepiaOnSurface,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = SepiaOnSurfaceVariant
)

@Composable
fun EbookAppTheme(
    readerTheme: ReaderTheme? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (readerTheme) {
        ReaderTheme.SEPIA -> SepiaColorScheme
        ReaderTheme.DARK -> DarkColorScheme
        ReaderTheme.LIGHT -> LightColorScheme
        null -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
