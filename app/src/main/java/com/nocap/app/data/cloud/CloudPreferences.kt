package com.nocap.app.data.cloud

import android.content.Context
import androidx.datastore.preferences.core.*
import com.nocap.app.core.datastore.LibraryPreferencesDataStore
import com.nocap.app.core.datastore.ReaderPreferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject

internal class CloudPreferences(context: Context) {
    private val stores = mapOf("reader" to ReaderPreferencesDataStore(context).backupStore,"library" to LibraryPreferencesDataStore(context).backupStore)
    suspend fun snapshot(): JSONObject = JSONObject().apply {
        for((name,store) in stores) put(name,JSONObject().apply {
            store.data.first().asMap().forEach { (key,value) -> put(key.name,JSONObject().put("type",when(value) { is Boolean -> "boolean";is Float -> "float";is Int -> "int";is Long -> "long";else -> "string" }).put("value",value)) }
        })
    }
    suspend fun restore(data: JSONObject) {
        for((name,store) in stores) {
            val objectData=data.optJSONObject(name) ?: continue
            store.edit { preferences ->
                preferences.clear()
                for(key in objectData.keys()) {
                    val item=objectData.getJSONObject(key)
                    when(item.getString("type")) {
                        "boolean" -> preferences[booleanPreferencesKey(key)]=item.getBoolean("value")
                        "float" -> preferences[floatPreferencesKey(key)]=item.getDouble("value").toFloat()
                        "int" -> preferences[intPreferencesKey(key)]=item.getInt("value")
                        "long" -> preferences[longPreferencesKey(key)]=item.getLong("value")
                        "string" -> preferences[stringPreferencesKey(key)]=item.getString("value")
                    }
                }
            }
        }
    }
}
