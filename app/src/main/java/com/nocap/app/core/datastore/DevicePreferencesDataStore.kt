package com.nocap.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_preferences")

class DevicePreferencesDataStore(private val context: Context) {
    companion object {
        private val KEY_INSTALLATION_ID = stringPreferencesKey("installation_id")
    }

    suspend fun getOrCreateInstallationId(): String {
        val currentId = context.deviceDataStore.data.map { it[KEY_INSTALLATION_ID] }.first()
        if (!currentId.isNullOrBlank()) {
            return currentId
        }
        val newId = UUID.randomUUID().toString()
        context.deviceDataStore.edit { preferences ->
            preferences[KEY_INSTALLATION_ID] = newId
        }
        return newId
    }
}

