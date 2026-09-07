package com.nocap.app.core.datastore

enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA
}

enum class ReaderFontFamily {
    SYSTEM_DEFAULT,
    SERIF,
    SANS_SERIF,
    LORA,
    ROBOTO,
    CUSTOM
}

enum class ReaderTextAlignment {
    START,
    JUSTIFY
}

enum class ReaderOrientation {
    FOLLOW_SYSTEM,
    PORTRAIT,
    LANDSCAPE
}

data class ReaderPreferences(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SYSTEM_DEFAULT,
    val customFontName: String? = null,
    val fontSizeMultiplier: Float = 1.0f,
    val lineHeightMultiplier: Float = 1.4f,
    val textAlignment: ReaderTextAlignment = ReaderTextAlignment.JUSTIFY,
    val isScrollMode: Boolean = false,
    val volumeButtonsTurnPages: Boolean = false,
    val keepScreenOn: Boolean = false,
    val isFullscreen: Boolean = false,
    val orientation: ReaderOrientation = ReaderOrientation.FOLLOW_SYSTEM,
    val brightnessFollowSystem: Boolean = true,
    val customBrightness: Float = 0.5f,
    val showReadingProgress: Boolean = true,
    val showPercentage: Boolean = true,
    val showClock: Boolean = true
)
