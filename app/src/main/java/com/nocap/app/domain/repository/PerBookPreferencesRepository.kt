package com.nocap.app.domain.repository

import com.nocap.app.core.database.entity.PerBookPreferencesEntity
import kotlinx.coroutines.flow.Flow

interface PerBookPreferencesRepository {
    fun observePreferences(bookId: String): Flow<PerBookPreferencesEntity?>
    suspend fun getPreferences(bookId: String): PerBookPreferencesEntity?
    suspend fun savePreferences(preferences: PerBookPreferencesEntity): Result<Unit>
    suspend fun resetToDefaults(bookId: String): Result<Unit>
}
