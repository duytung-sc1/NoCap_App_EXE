package com.nocap.app.domain.repository

import com.nocap.app.core.database.dao.ReviewItemWithDetails
import com.nocap.app.core.database.entity.ReviewItemEntity
import com.nocap.app.domain.review.ReviewRating
import kotlinx.coroutines.flow.Flow

data class ReviewStatsSummary(
    val dueCount: Int,
    val learningCount: Int,
    val masteredCount: Int,
    val totalCount: Int
)

interface ReviewRepository {
    fun observeDueItems(cutoffTime: Long = System.currentTimeMillis()): Flow<List<ReviewItemEntity>>
    fun observeDueItemsWithDetails(cutoffTime: Long = System.currentTimeMillis()): Flow<List<ReviewItemWithDetails>>
    suspend fun getDueItemsWithDetails(cutoffTime: Long = System.currentTimeMillis()): List<ReviewItemWithDetails>
    fun observeDueItemsCount(cutoffTime: Long = System.currentTimeMillis()): Flow<Int>
    suspend fun getDueItemsCount(cutoffTime: Long = System.currentTimeMillis()): Int
    suspend fun getReviewItem(annotationId: String): ReviewItemEntity?
    fun observeReviewItem(annotationId: String): Flow<ReviewItemEntity?>
    suspend fun addToReview(annotationId: String, bookId: String): Result<ReviewItemEntity>
    suspend fun removeFromReview(annotationId: String): Result<Unit>
    suspend fun submitReview(item: ReviewItemEntity, rating: ReviewRating, now: Long = System.currentTimeMillis()): Result<ReviewItemEntity>
    suspend fun toggleReviewEnabled(annotationId: String, isEnabled: Boolean): Result<Unit>
    fun observeAllReviewItems(): Flow<List<ReviewItemEntity>>
    suspend fun getReviewStats(now: Long = System.currentTimeMillis()): ReviewStatsSummary
    suspend fun autoGenerateReviewItems(bookId: String? = null, onlyWithNotes: Boolean = false): Result<Int>
}
