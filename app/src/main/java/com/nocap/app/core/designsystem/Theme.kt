package com.nocap.app.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nocap.app.core.datastore.AppColorPalette
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

val SageLightColorScheme = lightColorScheme(
    primary = SageLightPrimary,
    onPrimary = SageLightOnPrimary,
    primaryContainer = SageLightPrimaryContainer,
    onPrimaryContainer = SageLightOnPrimaryContainer,
    secondary = SageLightSecondary,
    onSecondary = SageLightOnSecondary,
    background = SageLightBackground,
    onBackground = SageLightOnBackground,
    surface = SageLightSurface,
    onSurface = SageLightOnSurface,
    surfaceVariant = SageLightSurfaceVariant,
    onSurfaceVariant = SageLightOnSurfaceVariant
)

val SageDarkColorScheme = darkColorScheme(
    primary = SageDarkPrimary,
    onPrimary = SageDarkOnPrimary,
    primaryContainer = SageDarkPrimaryContainer,
    onPrimaryContainer = SageDarkOnPrimaryContainer,
    secondary = SageDarkSecondary,
    onSecondary = SageDarkOnSecondary,
    background = SageDarkBackground,
    onBackground = SageDarkOnBackground,
    surface = SageDarkSurface,
    onSurface = SageDarkOnSurface,
    surfaceVariant = SageDarkSurfaceVariant,
    onSurfaceVariant = SageDarkOnSurfaceVariant
)

val VioletLightColorScheme = lightColorScheme(
    primary = VioletLightPrimary,
    onPrimary = VioletLightOnPrimary,
    primaryContainer = VioletLightPrimaryContainer,
    onPrimaryContainer = VioletLightOnPrimaryContainer,
    secondary = VioletLightSecondary,
    onSecondary = VioletLightOnSecondary,
    background = VioletLightBackground,
    onBackground = VioletLightOnBackground,
    surface = VioletLightSurface,
    onSurface = VioletLightOnSurface,
    surfaceVariant = VioletLightSurfaceVariant,
    onSurfaceVariant = VioletLightOnSurfaceVariant
)

val VioletDarkColorScheme = darkColorScheme(
    primary = VioletDarkPrimary,
    onPrimary = VioletDarkOnPrimary,
    primaryContainer = VioletDarkPrimaryContainer,
    onPrimaryContainer = VioletDarkOnPrimaryContainer,
    secondary = VioletDarkSecondary,
    onSecondary = VioletDarkOnSecondary,
    background = VioletDarkBackground,
    onBackground = VioletDarkOnBackground,
    surface = VioletDarkSurface,
    onSurface = VioletDarkOnSurface,
    surfaceVariant = VioletDarkSurfaceVariant,
    onSurfaceVariant = VioletDarkOnSurfaceVariant
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

internal fun staticAppColorScheme(
    appColorPalette: AppColorPalette,
    darkTheme: Boolean
): ColorScheme = when (appColorPalette) {
    AppColorPalette.NOCAP_NAVY,
    AppColorPalette.SYSTEM_DYNAMIC -> if (darkTheme) DarkColorScheme else LightColorScheme
    AppColorPalette.SAGE -> if (darkTheme) SageDarkColorScheme else SageLightColorScheme
    AppColorPalette.VIOLET -> if (darkTheme) VioletDarkColorScheme else VioletLightColorScheme
}

@Composable
fun EbookAppTheme(
    readerTheme: ReaderTheme? = null,
    appColorPalette: AppColorPalette = AppColorPalette.NOCAP_NAVY,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (readerTheme) {
        ReaderTheme.SEPIA -> SepiaColorScheme
        ReaderTheme.DARK -> DarkColorScheme
        ReaderTheme.LIGHT -> LightColorScheme
        null -> when {
            appColorPalette == AppColorPalette.SYSTEM_DYNAMIC &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> staticAppColorScheme(appColorPalette, darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
