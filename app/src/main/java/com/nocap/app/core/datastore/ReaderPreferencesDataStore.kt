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
        private val KEY_CUSTOM_FONT_NAME = stringPreferencesKey("reader_custom_font_name")
        private val KEY_FONT_SIZE = floatPreferencesKey("reader_font_size")
        private val KEY_LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        private val KEY_TEXT_ALIGNMENT = stringPreferencesKey("reader_text_alignment")
        private val KEY_SCROLL_MODE = booleanPreferencesKey("reader_scroll_mode")
        private val KEY_VOLUME_PAGE_TURN = booleanPreferencesKey("reader_volume_page_turn")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("reader_keep_screen_on")
        private val KEY_FULLSCREEN = booleanPreferencesKey("reader_fullscreen")
        private val KEY_ORIENTATION = stringPreferencesKey("reader_orientation")
        private val KEY_BRIGHTNESS_FOLLOW_SYSTEM = booleanPreferencesKey("reader_brightness_follow_system")
        private val KEY_CUSTOM_BRIGHTNESS = floatPreferencesKey("reader_custom_brightness")
        private val KEY_SHOW_PROGRESS = booleanPreferencesKey("reader_show_progress")
        private val KEY_SHOW_PERCENTAGE = booleanPreferencesKey("reader_show_percentage")
        private val KEY_SHOW_CLOCK = booleanPreferencesKey("reader_show_clock")
    }

    val readerPreferences: Flow<ReaderPreferences> = context.dataStore.data.map { preferences ->
        val theme = preferences[KEY_THEME]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: ReaderTheme.LIGHT
        val fontFamily = preferences[KEY_FONT_FAMILY]?.let { runCatching { ReaderFontFamily.valueOf(it) }.getOrNull() } ?: ReaderFontFamily.SYSTEM_DEFAULT
        val customFontName = preferences[KEY_CUSTOM_FONT_NAME]
        val fontSize = preferences[KEY_FONT_SIZE] ?: 1.0f
        val lineHeight = preferences[KEY_LINE_HEIGHT] ?: 1.4f
        val alignment = preferences[KEY_TEXT_ALIGNMENT]?.let { runCatching { ReaderTextAlignment.valueOf(it) }.getOrNull() } ?: ReaderTextAlignment.JUSTIFY
        val isScrollMode = preferences[KEY_SCROLL_MODE] ?: false
        val volumeTurn = preferences[KEY_VOLUME_PAGE_TURN] ?: false
        val keepScreen = preferences[KEY_KEEP_SCREEN_ON] ?: false
        val fullscreen = preferences[KEY_FULLSCREEN] ?: false
        val orientation = preferences[KEY_ORIENTATION]?.let { runCatching { ReaderOrientation.valueOf(it) }.getOrNull() } ?: ReaderOrientation.FOLLOW_SYSTEM
        val brightnessFollow = preferences[KEY_BRIGHTNESS_FOLLOW_SYSTEM] ?: true
        val customBrightness = preferences[KEY_CUSTOM_BRIGHTNESS] ?: 0.5f
        val showProgress = preferences[KEY_SHOW_PROGRESS] ?: true
        val showPercentage = preferences[KEY_SHOW_PERCENTAGE] ?: true
        val showClock = preferences[KEY_SHOW_CLOCK] ?: true

        ReaderPreferences(
            theme = theme,
            fontFamily = fontFamily,
            customFontName = customFontName,
            fontSizeMultiplier = fontSize,
            lineHeightMultiplier = lineHeight,
            textAlignment = alignment,
            isScrollMode = isScrollMode,
            volumeButtonsTurnPages = volumeTurn,
            keepScreenOn = keepScreen,
            isFullscreen = fullscreen,
            orientation = orientation,
            brightnessFollowSystem = brightnessFollow,
            customBrightness = customBrightness,
            showReadingProgress = showProgress,
            showPercentage = showPercentage,
            showClock = showClock
        )
    }

    suspend fun updateTheme(theme: ReaderTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun updateFontFamily(fontFamily: ReaderFontFamily, customFontName: String? = null) {
        context.dataStore.edit {
            it[KEY_FONT_FAMILY] = fontFamily.name
            if (customFontName != null) {
                it[KEY_CUSTOM_FONT_NAME] = customFontName
            } else {
                it.remove(KEY_CUSTOM_FONT_NAME)
            }
        }
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

    suspend fun updateVolumeButtonsTurnPages(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VOLUME_PAGE_TURN] = enabled }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateFullscreen(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FULLSCREEN] = enabled }
    }

    suspend fun updateOrientation(orientation: ReaderOrientation) {
        context.dataStore.edit { it[KEY_ORIENTATION] = orientation.name }
    }

    suspend fun updateBrightness(followSystem: Boolean, customBrightness: Float = 0.5f) {
        context.dataStore.edit {
            it[KEY_BRIGHTNESS_FOLLOW_SYSTEM] = followSystem
            it[KEY_CUSTOM_BRIGHTNESS] = customBrightness.coerceIn(0.05f, 1.0f)
        }
    }

    suspend fun updateChromeOptions(showProgress: Boolean, showPercentage: Boolean, showClock: Boolean) {
        context.dataStore.edit {
            it[KEY_SHOW_PROGRESS] = showProgress
            it[KEY_SHOW_PERCENTAGE] = showPercentage
            it[KEY_SHOW_CLOCK] = showClock
        }
    }
}
