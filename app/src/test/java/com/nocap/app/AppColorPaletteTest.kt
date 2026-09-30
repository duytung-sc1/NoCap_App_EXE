package com.nocap.app

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nocap.app.core.datastore.AppColorPalette
import com.nocap.app.core.datastore.ReaderPreferences
import com.nocap.app.core.designsystem.DarkColorScheme
import com.nocap.app.core.designsystem.LightColorScheme
import com.nocap.app.core.designsystem.SageDarkColorScheme
import com.nocap.app.core.designsystem.SageLightColorScheme
import com.nocap.app.core.designsystem.VioletDarkColorScheme
import com.nocap.app.core.designsystem.VioletLightColorScheme
import com.nocap.app.core.designsystem.staticAppColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class AppColorPaletteTest {
    @Test
    fun `existing users keep NoCap Navy by default`() {
        assertEquals(AppColorPalette.NOCAP_NAVY, ReaderPreferences().appColorPalette)
        assertEquals(LightColorScheme, staticAppColorScheme(AppColorPalette.NOCAP_NAVY, darkTheme = false))
        assertEquals(DarkColorScheme, staticAppColorScheme(AppColorPalette.NOCAP_NAVY, darkTheme = true))
    }

    @Test
    fun `static palettes resolve to matching light and dark schemes`() {
        assertEquals(SageLightColorScheme, staticAppColorScheme(AppColorPalette.SAGE, darkTheme = false))
        assertEquals(SageDarkColorScheme, staticAppColorScheme(AppColorPalette.SAGE, darkTheme = true))
        assertEquals(VioletLightColorScheme, staticAppColorScheme(AppColorPalette.VIOLET, darkTheme = false))
        assertEquals(VioletDarkColorScheme, staticAppColorScheme(AppColorPalette.VIOLET, darkTheme = true))
    }

    @Test
    fun `all static application palettes meet WCAG AA body text contrast`() {
        val schemes = listOf(
            LightColorScheme,
            DarkColorScheme,
            SageLightColorScheme,
            SageDarkColorScheme,
            VioletLightColorScheme,
            VioletDarkColorScheme
        )

        schemes.forEach { scheme ->
            assertContrastAtLeast(scheme, scheme.primary, scheme.onPrimary, "primary")
            assertContrastAtLeast(scheme, scheme.background, scheme.onBackground, "background")
            assertContrastAtLeast(scheme, scheme.surface, scheme.onSurface, "surface")
            assertContrastAtLeast(
                scheme,
                scheme.surfaceVariant,
                scheme.onSurfaceVariant,
                "surfaceVariant"
            )
        }
    }

    private fun assertContrastAtLeast(
        scheme: ColorScheme,
        background: Color,
        foreground: Color,
        role: String
    ) {
        val ratio = contrastRatio(background, foreground)
        assertTrue(
            "$role contrast was $ratio for primary ${scheme.primary}",
            ratio >= 4.5
        )
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        val red = ((argb shr 16) and 0xFF) / 255.0
        val green = ((argb shr 8) and 0xFF) / 255.0
        val blue = (argb and 0xFF) / 255.0
        return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
    }

    private fun linearize(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
