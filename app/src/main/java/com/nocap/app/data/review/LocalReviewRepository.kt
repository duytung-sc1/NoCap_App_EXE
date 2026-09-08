package com.nocap.app.data.review

import com.nocap.app.core.database.dao.ReviewDao
import com.nocap.app.core.database.dao.ReviewItemWithDetails
import com.nocap.app.core.database.entity.ReviewItemEntity
import com.nocap.app.domain.repository.ReviewRepository
import com.nocap.app.domain.review.ReviewRating
import com.nocap.app.domain.review.ReviewScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LocalReviewRepository(
    private val reviewDao: ReviewDao
) : ReviewRepository {

    override fun observeDueItems(cutoffTime: Long): Flow<List<ReviewItemEntity>> {
        return reviewDao.observeDueItems(cutoffTime)
    }

    override fun observeDueItemsWithDetails(cutoffTime: Long): Flow<List<ReviewItemWithDetails>> {
        return reviewDao.observeDueItemsWithDetails(cutoffTime)
    }

    override suspend fun getDueItemsWithDetails(cutoffTime: Long): List<ReviewItemWithDetails> {
        return reviewDao.getDueItemsWithDetails(cutoffTime)
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
                bookId = bookId
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
}
