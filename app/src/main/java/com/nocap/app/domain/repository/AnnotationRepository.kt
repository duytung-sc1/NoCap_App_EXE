package com.nocap.app.domain.repository

import com.nocap.app.core.database.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

interface AnnotationRepository {
    fun observeHighlights(bookId: String): Flow<List<HighlightEntity>>
    suspend fun getHighlights(bookId: String): List<HighlightEntity>
    suspend fun addHighlight(
        bookId: String,
        locatorJson: String,
        text: String,
        color: String,
        note: String? = null
    ): Result<HighlightEntity>
    suspend fun updateHighlightColor(highlightId: String, newColor: String): Result<Unit>
    suspend fun updateNote(highlightId: String, note: String?): Result<Unit>
    suspend fun deleteHighlight(highlightId: String): Result<Unit>
    suspend fun deleteHighlightsForBook(bookId: String): Result<Unit>
}
