package com.nocap.app.domain.review

import com.nocap.app.core.database.entity.ReviewItemEntity

enum class ReviewRating {
    AGAIN, // Chưa nhớ
    HARD,  // Khó
    GOOD,  // Tốt
    EASY   // Dễ
}

enum class MemoryState {
    DUE,       // Cần ôn lại
    LEARNING,  // Đang học
    MASTERED   // Đã nhớ
}

object ReviewScheduler {

    const val ONE_DAY_MS: Long = 24L * 60 * 60 * 1000 // 86,400,000 ms

    /**
     * Determines the memory status of an item: DUE, LEARNING, or MASTERED.
     */
    fun memoryState(item: ReviewItemEntity, now: Long = System.currentTimeMillis()): MemoryState {
        return when {
            item.nextReviewAt <= now -> MemoryState.DUE
            item.intervalDays >= 21 -> MemoryState.MASTERED
            else -> MemoryState.LEARNING
        }
    }

    /**
     * Calculates a priority score for queue ordering:
     * Higher score = higher priority in review queue (items often forgotten or overdue).
     */
    fun priorityScore(item: ReviewItemEntity, now: Long = System.currentTimeMillis()): Float {
        val overdueDays = maxOf(0f, (now - item.nextReviewAt).toFloat() / ONE_DAY_MS)
        val difficultyWeight = (3.0f - item.easeFactor).coerceAtLeast(0.1f)
        val forgottenBonus = if (item.reviewCount > 0 && item.intervalDays <= 1) 5.0f else 0f
        return overdueDays * 2.5f + difficultyWeight * 3.0f + forgottenBonus
    }

    /**
     * Calculates the next review state given the current item and the user's rating.
     */
    fun calculateNextReview(
        item: ReviewItemEntity,
        rating: ReviewRating,
        now: Long = System.currentTimeMillis()
    ): ReviewItemEntity {
        var newInterval: Int
        var newEaseFactor = item.easeFactor
        val newReviewCount = item.reviewCount + 1

        when (rating) {
            ReviewRating.AGAIN -> {
                newInterval = 1
                newEaseFactor = maxOf(1.3f, newEaseFactor - 0.20f)
            }
            ReviewRating.HARD -> {
                newInterval = maxOf(1, (item.intervalDays * 1.2f).toInt())
                newEaseFactor = maxOf(1.3f, newEaseFactor - 0.15f)
            }
            ReviewRating.GOOD -> {
                newInterval = when (item.reviewCount) {
                    0 -> 1
                    1 -> 3
                    else -> maxOf(3, (item.intervalDays * newEaseFactor).toInt())
                }
            }
            ReviewRating.EASY -> {
                newInterval = when (item.reviewCount) {
                    0 -> 3
                    1 -> 5
                    else -> maxOf(5, (item.intervalDays * (newEaseFactor + 0.5f)).toInt())
                }
                newEaseFactor = minOf(3.5f, newEaseFactor + 0.15f)
            }
        }

        newInterval = newInterval.coerceIn(1, 36500)
        val nextReviewAt = now + (newInterval.toLong() * ONE_DAY_MS)

        return item.copy(
            nextReviewAt = nextReviewAt,
            lastReviewedAt = now,
            reviewCount = newReviewCount,
            intervalDays = newInterval,
            easeFactor = newEaseFactor,
            updatedAt = now
        )
    }

    /**
     * Creates a new review item for an annotation with initial 1-day schedule.
     */
    fun createInitialReviewItem(
        id: String,
        annotationId: String,
        bookId: String,
        now: Long = System.currentTimeMillis()
    ): ReviewItemEntity {
        return ReviewItemEntity(
            id = id,
            annotationId = annotationId,
            bookId = bookId,
            isEnabled = true,
            nextReviewAt = now + ONE_DAY_MS,
            lastReviewedAt = null,
            reviewCount = 0,
            intervalDays = 1,
            easeFactor = 2.5f,
            createdAt = now,
            updatedAt = now
        )
    }
}
