package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nocap.app.core.database.entity.PerBookPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerBookPreferencesDao {
    @Query("SELECT * FROM per_book_preferences WHERE book_id = :bookId")
    fun observePreferences(bookId: String): Flow<PerBookPreferencesEntity?>

    @Query("SELECT * FROM per_book_preferences WHERE book_id = :bookId")
    suspend fun getPreferences(bookId: String): PerBookPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preferences: PerBookPreferencesEntity)

    @Query("DELETE FROM per_book_preferences WHERE book_id = :bookId")
    suspend fun deletePreferences(bookId: String)
}
