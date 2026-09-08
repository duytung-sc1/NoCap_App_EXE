package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nocap.app.core.database.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE book_id = :bookId ORDER BY created_at DESC")
    fun observeHighlightsForBook(bookId: String): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE book_id = :bookId ORDER BY created_at DESC")
    suspend fun getHighlightsForBook(bookId: String): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun getHighlightById(id: String): HighlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Update
    suspend fun updateHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: String)

    @Query("DELETE FROM highlights WHERE book_id = :bookId")
    suspend fun deleteHighlightsByBookId(bookId: String)

    @Query("SELECT * FROM highlights ORDER BY created_at DESC")
    fun observeAllHighlights(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY created_at DESC")
    suspend fun getAllHighlights(): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE note IS NOT NULL AND TRIM(note) != '' ORDER BY updated_at DESC")
    fun observeAllNotes(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE note IS NOT NULL AND TRIM(note) != '' ORDER BY updated_at DESC")
    suspend fun getAllNotes(): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE text LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY created_at DESC")
    suspend fun searchHighlights(query: String): List<HighlightEntity>
}
