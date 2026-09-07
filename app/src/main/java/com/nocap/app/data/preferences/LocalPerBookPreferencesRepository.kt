package com.nocap.app.data.preferences

import com.nocap.app.core.database.dao.PerBookPreferencesDao
import com.nocap.app.core.database.entity.PerBookPreferencesEntity
import com.nocap.app.domain.repository.PerBookPreferencesRepository
import kotlinx.coroutines.flow.Flow

class LocalPerBookPreferencesRepository(
    private val perBookPreferencesDao: PerBookPreferencesDao
) : PerBookPreferencesRepository {

    override fun observePreferences(bookId: String): Flow<PerBookPreferencesEntity?> {
        return perBookPreferencesDao.observePreferences(bookId)
    }

    override suspend fun getPreferences(bookId: String): PerBookPreferencesEntity? {
        return perBookPreferencesDao.getPreferences(bookId)
    }

    override suspend fun savePreferences(preferences: PerBookPreferencesEntity): Result<Unit> {
        return try {
            perBookPreferencesDao.insertOrUpdate(preferences)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetToDefaults(bookId: String): Result<Unit> {
        return try {
            perBookPreferencesDao.deletePreferences(bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
