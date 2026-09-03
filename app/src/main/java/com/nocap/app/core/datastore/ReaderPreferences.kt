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
    ROBOTO
}

enum class ReaderTextAlignment {
    START,
    JUSTIFY
}

data class ReaderPreferences(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SYSTEM_DEFAULT,
    val fontSizeMultiplier: Float = 1.0f,
    val lineHeightMultiplier: Float = 1.4f,
    val textAlignment: ReaderTextAlignment = ReaderTextAlignment.JUSTIFY,
    val isScrollMode: Boolean = false
)
