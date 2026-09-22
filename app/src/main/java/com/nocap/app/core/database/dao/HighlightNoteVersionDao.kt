package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nocap.app.core.database.entity.HighlightNoteVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightNoteVersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: HighlightNoteVersionEntity)

    @Query("SELECT * FROM highlight_note_versions WHERE highlight_id = :highlightId ORDER BY created_at DESC")
    fun observeVersions(highlightId: String): Flow<List<HighlightNoteVersionEntity>>

    @Query("SELECT * FROM highlight_note_versions WHERE highlight_id = :highlightId ORDER BY created_at DESC")
    suspend fun getVersions(highlightId: String): List<HighlightNoteVersionEntity>

    @Query("SELECT * FROM highlight_note_versions WHERE id = :id LIMIT 1")
    suspend fun getVersionById(id: String): HighlightNoteVersionEntity?

    @Query("DELETE FROM highlight_note_versions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM highlight_note_versions WHERE highlight_id = :highlightId")
    suspend fun deleteByHighlightId(highlightId: String)
}
