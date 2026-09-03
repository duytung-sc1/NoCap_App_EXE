package com.nocap.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_preferences")

class ReaderPreferencesDataStore(private val context: Context) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("reader_theme")
        private val KEY_FONT_FAMILY = stringPreferencesKey("reader_font_family")
        private val KEY_FONT_SIZE = floatPreferencesKey("reader_font_size")
        private val KEY_LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        private val KEY_TEXT_ALIGNMENT = stringPreferencesKey("reader_text_alignment")
        private val KEY_SCROLL_MODE = booleanPreferencesKey("reader_scroll_mode")
    }

    val readerPreferences: Flow<ReaderPreferences> = context.dataStore.data.map { preferences ->
        val theme = preferences[KEY_THEME]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: ReaderTheme.LIGHT
        val fontFamily = preferences[KEY_FONT_FAMILY]?.let { runCatching { ReaderFontFamily.valueOf(it) }.getOrNull() } ?: ReaderFontFamily.SYSTEM_DEFAULT
        val fontSize = preferences[KEY_FONT_SIZE] ?: 1.0f
        val lineHeight = preferences[KEY_LINE_HEIGHT] ?: 1.4f
        val alignment = preferences[KEY_TEXT_ALIGNMENT]?.let { runCatching { ReaderTextAlignment.valueOf(it) }.getOrNull() } ?: ReaderTextAlignment.JUSTIFY
        val isScrollMode = preferences[KEY_SCROLL_MODE] ?: false

        ReaderPreferences(
            theme = theme,
            fontFamily = fontFamily,
            fontSizeMultiplier = fontSize,
            lineHeightMultiplier = lineHeight,
            textAlignment = alignment,
            isScrollMode = isScrollMode
        )
    }

    suspend fun updateTheme(theme: ReaderTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun updateFontFamily(fontFamily: ReaderFontFamily) {
        context.dataStore.edit { it[KEY_FONT_FAMILY] = fontFamily.name }
    }

    suspend fun updateFontSize(multiplier: Float) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = multiplier.coerceIn(0.8f, 2.0f) }
    }

    suspend fun updateLineHeight(multiplier: Float) {
        context.dataStore.edit { it[KEY_LINE_HEIGHT] = multiplier.coerceIn(1.2f, 2.0f) }
    }

    suspend fun updateTextAlignment(alignment: ReaderTextAlignment) {
        context.dataStore.edit { it[KEY_TEXT_ALIGNMENT] = alignment.name }
    }

    suspend fun updateScrollMode(isScrollMode: Boolean) {
        context.dataStore.edit { it[KEY_SCROLL_MODE] = isScrollMode }
    }
}
