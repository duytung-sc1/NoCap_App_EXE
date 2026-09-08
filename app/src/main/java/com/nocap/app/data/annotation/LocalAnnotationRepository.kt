package com.nocap.app.data.annotation

import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.domain.repository.AnnotationRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LocalAnnotationRepository(
    private val highlightDao: HighlightDao
) : AnnotationRepository {

    override fun observeHighlights(bookId: String): Flow<List<HighlightEntity>> {
        return highlightDao.observeHighlightsForBook(bookId)
    }

    override suspend fun getHighlights(bookId: String): List<HighlightEntity> {
        return highlightDao.getHighlightsForBook(bookId)
    }

    override suspend fun addHighlight(
        bookId: String,
        locatorJson: String,
        text: String,
        color: String,
        note: String?
    ): Result<HighlightEntity> {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = HighlightEntity(
            id = id,
            bookId = bookId,
            locatorJson = locatorJson,
            text = text,
            color = color,
            note = note?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )
        return try {
            highlightDao.insertHighlight(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHighlightColor(highlightId: String, newColor: String): Result<Unit> {
        val existing = highlightDao.getHighlightById(highlightId)
            ?: return Result.failure(IllegalArgumentException("Không tìm thấy đoạn tô sáng"))
        val updated = existing.copy(
            color = newColor,
            updatedAt = System.currentTimeMillis()
        )
        return try {
            highlightDao.updateHighlight(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNote(highlightId: String, note: String?): Result<Unit> {
        val existing = highlightDao.getHighlightById(highlightId)
            ?: return Result.failure(IllegalArgumentException("Không tìm thấy đoạn tô sáng"))
        val updated = existing.copy(
            note = note?.trim()?.ifBlank { null },
            updatedAt = System.currentTimeMillis()
        )
        return try {
            highlightDao.updateHighlight(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHighlight(highlightId: String): Result<Unit> {
        return try {
            highlightDao.deleteHighlight(highlightId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHighlightsForBook(bookId: String): Result<Unit> {
        return try {
            highlightDao.deleteHighlightsByBookId(bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
