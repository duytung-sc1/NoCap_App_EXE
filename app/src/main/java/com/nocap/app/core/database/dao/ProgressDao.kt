package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nocap.app.core.database.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId")
    fun observeProgress(bookId: String): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId")
    suspend fun getProgress(bookId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress ORDER BY last_read_at DESC")
    fun observeRecentlyRead(): Flow<List<ReadingProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE book_id = :bookId")
    suspend fun deleteProgress(bookId: String)
}
