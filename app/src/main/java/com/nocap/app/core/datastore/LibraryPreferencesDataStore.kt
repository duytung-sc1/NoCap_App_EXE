package com.nocap.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(name = "library_preferences")

class LibraryPreferencesDataStore(private val context: Context) {
    companion object {
        private val KEY_SORT = stringPreferencesKey("library_sort")
        private val KEY_SMART_VIEW = stringPreferencesKey("library_smart_view")
        private val KEY_GRID_MODE = booleanPreferencesKey("library_grid_mode")
    }

    val libraryPreferences: Flow<LibraryPreferences> = context.libraryDataStore.data.map { preferences ->
        val sort = preferences[KEY_SORT]?.let { runCatching { LibrarySort.valueOf(it) }.getOrNull() } ?: LibrarySort.RECENTLY_OPENED
        val smartView = preferences[KEY_SMART_VIEW]?.let { runCatching { LibrarySmartView.valueOf(it) }.getOrNull() } ?: LibrarySmartView.ALL
        val isGridMode = preferences[KEY_GRID_MODE] ?: false

        LibraryPreferences(
            sort = sort,
            smartView = smartView,
            isGridMode = isGridMode
        )
    }

    suspend fun updateSort(sort: LibrarySort) {
        context.libraryDataStore.edit { preferences ->
            preferences[KEY_SORT] = sort.name
        }
    }

    suspend fun updateSmartView(smartView: LibrarySmartView) {
        context.libraryDataStore.edit { preferences ->
            preferences[KEY_SMART_VIEW] = smartView.name
        }
    }

    suspend fun updateGridMode(isGridMode: Boolean) {
        context.libraryDataStore.edit { preferences ->
            preferences[KEY_GRID_MODE] = isGridMode
        }
    }
}
