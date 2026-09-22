package com.nocap.app

import com.nocap.app.core.database.entity.ReviewItemEntity
import com.nocap.app.domain.review.MemoryState
import com.nocap.app.domain.review.ReviewRating
import com.nocap.app.domain.review.ReviewScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {

    @Test
    fun `test initial review item defaults`() {
        val now = 1_700_000_000_000L
        val item = ReviewScheduler.createInitialReviewItem(
            id = "rev_1",
            annotationId = "hl_1",
            bookId = "book_1",
            now = now
        )

        assertEquals("rev_1", item.id)
        assertEquals("hl_1", item.annotationId)
        assertEquals("book_1", item.bookId)
        assertTrue(item.isEnabled)
        assertEquals(0, item.reviewCount)
        assertEquals(1, item.intervalDays)
        assertEquals(2.5f, item.easeFactor, 0.001f)
        assertEquals(now + ReviewScheduler.ONE_DAY_MS, item.nextReviewAt)
    }

    @Test
    fun `test AGAIN rating resets interval to 1 day and decreases ease factor`() {
        val now = 1_700_000_000_000L
        val item = ReviewItemEntity(
            id = "rev_1",
            annotationId = "hl_1",
            bookId = "book_1",
            isEnabled = true,
            nextReviewAt = now,
            lastReviewedAt = now - 5 * ReviewScheduler.ONE_DAY_MS,
            reviewCount = 3,
            intervalDays = 5,
            easeFactor = 2.5f,
            createdAt = now - 10 * ReviewScheduler.ONE_DAY_MS,
            updatedAt = now - 5 * ReviewScheduler.ONE_DAY_MS
        )

        val updated = ReviewScheduler.calculateNextReview(item, ReviewRating.AGAIN, now = now)

        assertEquals(1, updated.intervalDays)
        assertEquals(2.3f, updated.easeFactor, 0.001f)
        assertEquals(4, updated.reviewCount)
        assertEquals(now + ReviewScheduler.ONE_DAY_MS, updated.nextReviewAt)
        assertEquals(now, updated.lastReviewedAt)
    }

    @Test
    fun `test ease factor never drops below minimum bound 1_3f`() {
        val now = 1_700_000_000_000L
        var item = ReviewScheduler.createInitialReviewItem("r1", "a1", "b1", now)
        item = item.copy(easeFactor = 1.35f)

        // Multiple AGAIN ratings
        val updated1 = ReviewScheduler.calculateNextReview(item, ReviewRating.AGAIN, now)
        assertEquals(1.3f, updated1.easeFactor, 0.001f)

        val updated2 = ReviewScheduler.calculateNextReview(updated1, ReviewRating.AGAIN, now)
        assertEquals(1.3f, updated2.easeFactor, 0.001f)
    }

    @Test
    fun `test HARD rating slightly increases interval and decreases ease factor`() {
        val now = 1_700_000_000_000L
        val item = ReviewItemEntity(
            id = "rev_1",
            annotationId = "hl_1",
            bookId = "book_1",
            isEnabled = true,
            nextReviewAt = now,
            lastReviewedAt = now,
            reviewCount = 2,
            intervalDays = 4,
            easeFactor = 2.5f,
            createdAt = now,
            updatedAt = now
        )

        val updated = ReviewScheduler.calculateNextReview(item, ReviewRating.HARD, now = now)

        // interval = maxOf(1, (4 * 1.2).toInt()) = 4
        assertEquals(4, updated.intervalDays)
        assertEquals(2.35f, updated.easeFactor, 0.001f)
        assertEquals(3, updated.reviewCount)
    }

    @Test
    fun `test GOOD rating progression 1 to 3 to multiplier`() {
        val now = 1_700_000_000_000L
        val initial = ReviewScheduler.createInitialReviewItem("r1", "a1", "b1", now)

        // 1st review (count 0 -> 1)
        val rep1 = ReviewScheduler.calculateNextReview(initial, ReviewRating.GOOD, now)
        assertEquals(1, rep1.intervalDays)
        assertEquals(1, rep1.reviewCount)

        // 2nd review (count 1 -> 2)
        val rep2 = ReviewScheduler.calculateNextReview(rep1, ReviewRating.GOOD, now)
        assertEquals(3, rep2.intervalDays)
        assertEquals(2, rep2.reviewCount)

        // 3rd review (count 2 -> 3) with easeFactor 2.5: interval = 3 * 2.5 = 7
        val rep3 = ReviewScheduler.calculateNextReview(rep2, ReviewRating.GOOD, now)
        assertEquals(7, rep3.intervalDays)
        assertEquals(3, rep3.reviewCount)
    }

    @Test
    fun `test priorityScore prioritizes overdue items and lower ease factor`() {
        val now = 1_700_000_000_000L
        val overdueHard = ReviewItemEntity(
            id = "r1", annotationId = "a1", bookId = "b1", isEnabled = true,
            nextReviewAt = now - 5 * ReviewScheduler.ONE_DAY_MS, // 5 days overdue
            lastReviewedAt = now - 10 * ReviewScheduler.ONE_DAY_MS,
            reviewCount = 2, intervalDays = 1, easeFactor = 1.8f,
            createdAt = now, updatedAt = now
        )
        val notDueEasy = ReviewItemEntity(
            id = "r2", annotationId = "a2", bookId = "b1", isEnabled = true,
            nextReviewAt = now + 5 * ReviewScheduler.ONE_DAY_MS, // in future
            lastReviewedAt = now,
            reviewCount = 5, intervalDays = 20, easeFactor = 2.8f,
            createdAt = now, updatedAt = now
        )

        val score1 = ReviewScheduler.priorityScore(overdueHard, now)
        val score2 = ReviewScheduler.priorityScore(notDueEasy, now)

        assertTrue("Overdue hard card must have higher priority score than not-due easy card", score1 > score2)
    }

    @Test
    fun `test MemoryState categorization`() {
        val now = 1_700_000_000_000L
        val dueItem = ReviewItemEntity(
            id = "r1", annotationId = "a1", bookId = "b1", isEnabled = true,
            nextReviewAt = now - 1000, lastReviewedAt = null,
            reviewCount = 0, intervalDays = 1, easeFactor = 2.5f,
            createdAt = now, updatedAt = now
        )
        val learningItem = ReviewItemEntity(
            id = "r2", annotationId = "a2", bookId = "b1", isEnabled = true,
            nextReviewAt = now + 2 * ReviewScheduler.ONE_DAY_MS, lastReviewedAt = now,
            reviewCount = 2, intervalDays = 3, easeFactor = 2.5f,
            createdAt = now, updatedAt = now
        )
        val masteredItem = ReviewItemEntity(
            id = "r3", annotationId = "a3", bookId = "b1", isEnabled = true,
            nextReviewAt = now + 30 * ReviewScheduler.ONE_DAY_MS, lastReviewedAt = now,
            reviewCount = 6, intervalDays = 30, easeFactor = 2.6f,
            createdAt = now, updatedAt = now
        )

        assertEquals(MemoryState.DUE, ReviewScheduler.memoryState(dueItem, now))
        assertEquals(MemoryState.LEARNING, ReviewScheduler.memoryState(learningItem, now))
        assertEquals(MemoryState.MASTERED, ReviewScheduler.memoryState(masteredItem, now))
    }

    @Test
    fun `test interval and ease factor caps on extreme repetitions`() {
        val now = 1_700_000_000_000L
        var item = ReviewScheduler.createInitialReviewItem("r1", "a1", "b1", now)
        for (i in 0 until 30) {
            item = ReviewScheduler.calculateNextReview(item, ReviewRating.EASY, now)
        }
        assertTrue("Interval must not exceed 36500 days", item.intervalDays <= 36500)
        assertTrue("Ease factor must not exceed 3.5", item.easeFactor <= 3.5f)
        assertTrue("Next review must be positive and in future", item.nextReviewAt > now)
    }

    @Test
    fun `test overdue mastered item is categorized as DUE`() {
        val now = 1_700_000_000_000L
        val overdueMastered = ReviewItemEntity(
            id = "r1", annotationId = "a1", bookId = "b1", isEnabled = true,
            nextReviewAt = now - 1000,
            lastReviewedAt = now - 35 * ReviewScheduler.ONE_DAY_MS,
            reviewCount = 8, intervalDays = 30, easeFactor = 2.5f,
            createdAt = now, updatedAt = now
        )
        assertEquals(MemoryState.DUE, ReviewScheduler.memoryState(overdueMastered, now))
    }
}
