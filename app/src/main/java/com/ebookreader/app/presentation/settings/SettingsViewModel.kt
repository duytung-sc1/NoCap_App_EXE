package com.ebookreader.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ebookreader.app.core.datastore.ReaderFontFamily
import com.ebookreader.app.core.datastore.ReaderPreferences
import com.ebookreader.app.core.datastore.ReaderPreferencesDataStore
import com.ebookreader.app.core.datastore.ReaderTextAlignment
import com.ebookreader.app.core.datastore.ReaderTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesDataStore: ReaderPreferencesDataStore
) : ViewModel() {

    val preferences: StateFlow<ReaderPreferences> = preferencesDataStore.readerPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderPreferences()
        )

    fun updateTheme(theme: ReaderTheme) {
        viewModelScope.launch { preferencesDataStore.updateTheme(theme) }
    }

    fun updateFontFamily(fontFamily: ReaderFontFamily) {
        viewModelScope.launch { preferencesDataStore.updateFontFamily(fontFamily) }
    }

    fun updateFontSize(multiplier: Float) {
        viewModelScope.launch { preferencesDataStore.updateFontSize(multiplier) }
    }

    fun updateLineHeight(multiplier: Float) {
        viewModelScope.launch { preferencesDataStore.updateLineHeight(multiplier) }
    }

    fun updateTextAlignment(alignment: ReaderTextAlignment) {
        viewModelScope.launch { preferencesDataStore.updateTextAlignment(alignment) }
    }

    fun updateScrollMode(isScrollMode: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateScrollMode(isScrollMode) }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            preferencesDataStore.updateTheme(ReaderTheme.LIGHT)
            preferencesDataStore.updateFontFamily(ReaderFontFamily.SYSTEM_DEFAULT)
            preferencesDataStore.updateFontSize(1.0f)
            preferencesDataStore.updateLineHeight(1.4f)
            preferencesDataStore.updateTextAlignment(ReaderTextAlignment.JUSTIFY)
            preferencesDataStore.updateScrollMode(false)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    preferencesDataStore = ReaderPreferencesDataStore(context.applicationContext)
                ) as T
            }
        }
    }
}
