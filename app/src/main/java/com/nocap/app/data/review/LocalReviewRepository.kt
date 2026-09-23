package com.nocap.app.data.review

import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.database.dao.ReviewDao
import com.nocap.app.core.database.dao.ReviewItemWithDetails
import com.nocap.app.core.database.entity.ReviewItemEntity
import com.nocap.app.domain.repository.AutoGenResult
import com.nocap.app.domain.repository.ReviewRepository
import com.nocap.app.domain.repository.ReviewStatsSummary
import com.nocap.app.domain.review.ReviewRating
import com.nocap.app.domain.review.ReviewScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LocalReviewRepository(
    private val reviewDao: ReviewDao,
    private val highlightDao: HighlightDao? = null
) : ReviewRepository {

    override fun observeDueItems(cutoffTime: Long): Flow<List<ReviewItemEntity>> {
        return reviewDao.observeDueItems(cutoffTime)
    }

    override fun observeDueItemsWithDetails(cutoffTime: Long): Flow<List<ReviewItemWithDetails>> {
        return reviewDao.observeDueItemsWithDetails(cutoffTime)
    }

    override suspend fun getDueItemsWithDetails(cutoffTime: Long): List<ReviewItemWithDetails> {
        val items = reviewDao.getDueItemsWithDetails(cutoffTime)
        val now = System.currentTimeMillis()
        // Priority ordering: items often forgotten or overdue are prioritized first
        return items.sortedByDescending { ReviewScheduler.priorityScore(it.reviewItem, now) }
    }

    override fun observeDueItemsCount(cutoffTime: Long): Flow<Int> {
        return reviewDao.observeDueItemsCount(cutoffTime)
    }

    override suspend fun getDueItemsCount(cutoffTime: Long): Int {
        return reviewDao.getDueItemsCount(cutoffTime)
    }

    override suspend fun getReviewItem(annotationId: String): ReviewItemEntity? {
        return reviewDao.getReviewItemByAnnotationId(annotationId)
    }

    override fun observeReviewItem(annotationId: String): Flow<ReviewItemEntity?> {
        return reviewDao.observeReviewItemByAnnotationId(annotationId)
    }

    override suspend fun addToReview(annotationId: String, bookId: String): Result<ReviewItemEntity> {
        return try {
            val existing = reviewDao.getReviewItemByAnnotationId(annotationId)
            if (existing != null) {
                if (!existing.isEnabled) {
                    val reEnabled = existing.copy(isEnabled = true, updatedAt = System.currentTimeMillis())
                    reviewDao.updateReviewItem(reEnabled)
                    return Result.success(reEnabled)
                }
                return Result.success(existing)
            }

            val newItem = ReviewScheduler.createInitialReviewItem(
                id = UUID.randomUUID().toString(),
                annotationId = annotationId,
                bookId = bookId,
                dueImmediately = true
            )
            reviewDao.insertReviewItem(newItem)
            Result.success(newItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromReview(annotationId: String): Result<Unit> {
        return try {
            reviewDao.deleteByAnnotationId(annotationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitReview(
        item: ReviewItemEntity,
        rating: ReviewRating,
        now: Long
    ): Result<ReviewItemEntity> {
        return try {
            val updated = ReviewScheduler.calculateNextReview(item, rating, now)
            reviewDao.updateReviewItem(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReviewEnabled(annotationId: String, isEnabled: Boolean): Result<Unit> {
        return try {
            val item = reviewDao.getReviewItemByAnnotationId(annotationId)
                ?: return Result.failure(IllegalArgumentException("Không tìm thấy mục ôn tập"))
            val updated = item.copy(isEnabled = isEnabled, updatedAt = System.currentTimeMillis())
            reviewDao.updateReviewItem(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllReviewItems(): Flow<List<ReviewItemEntity>> {
        return reviewDao.observeAllReviewItems()
    }

    override suspend fun getAllReviewItemsWithDetails(): List<ReviewItemWithDetails> {
        val items = reviewDao.getAllReviewItemsWithDetails()
        val now = System.currentTimeMillis()
        return items.sortedByDescending { ReviewScheduler.priorityScore(it.reviewItem, now) }
    }

    override fun observeAllReviewItemsWithDetails(): Flow<List<ReviewItemWithDetails>> {
        return reviewDao.observeAllReviewItemsWithDetails()
    }

    override suspend fun resetUnreviewedItemsToNow(now: Long): Int {
        return reviewDao.resetUnreviewedItemsToNow(now)
    }

    override suspend fun markAsMastered(annotationId: String): Result<ReviewItemEntity> {
        return try {
            val item = reviewDao.getReviewItemByAnnotationId(annotationId)
                ?: return Result.failure(IllegalArgumentException("Không tìm thấy mục ôn tập"))
            val now = System.currentTimeMillis()
            val mastered = ReviewScheduler.markAsMastered(item, now)
            reviewDao.updateReviewItem(mastered)
            Result.success(mastered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewStats(now: Long): ReviewStatsSummary {
        val due = reviewDao.getDueCount(now)
        val learning = reviewDao.getLearningCount(now)
        val mastered = reviewDao.getMasteredCount(now)
        return ReviewStatsSummary(
            dueCount = due,
            learningCount = learning,
            masteredCount = mastered,
            totalCount = due + learning + mastered
        )
    }

    override suspend fun autoGenerateReviewItemsDetailed(bookId: String?, onlyWithNotes: Boolean): Result<AutoGenResult> {
        return try {
            val dao = highlightDao ?: return Result.failure(IllegalStateException("HighlightDao not provided"))
            val allHighlights = if (bookId != null) dao.getHighlightsForBook(bookId) else dao.getAllHighlights()
            if (allHighlights.isEmpty()) {
                return Result.success(AutoGenResult.NoHighlightsAtAll)
            }
            if (onlyWithNotes && allHighlights.none { !it.note.isNullOrBlank() }) {
                return Result.success(AutoGenResult.NoHighlightsWithNotes)
            }
            val existingIds = reviewDao.getAllReviewAnnotationIds().toSet()
            val candidates = allHighlights.filter { hl ->
                !existingIds.contains(hl.id) && (!onlyWithNotes || !hl.note.isNullOrBlank())
            }
            if (candidates.isEmpty()) {
                val totalRelevant = if (onlyWithNotes) allHighlights.count { !it.note.isNullOrBlank() } else allHighlights.size
                return Result.success(AutoGenResult.AllAlreadyInReview(totalRelevant))
            }
            val now = System.currentTimeMillis()
            val newItems = candidates.map { hl ->
                ReviewScheduler.createInitialReviewItem(
                    id = UUID.randomUUID().toString(),
                    annotationId = hl.id,
                    bookId = hl.bookId,
                    now = now,
                    dueImmediately = true
                )
            }
            reviewDao.insertReviewItems(newItems)
            Result.success(AutoGenResult.Added(newItems.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun autoGenerateReviewItems(bookId: String?, onlyWithNotes: Boolean): Result<Int> {
        return when (val detailed = autoGenerateReviewItemsDetailed(bookId, onlyWithNotes).getOrThrow()) {
            is AutoGenResult.Added -> Result.success(detailed.count)
            else -> Result.success(0)
        }
    }
}
