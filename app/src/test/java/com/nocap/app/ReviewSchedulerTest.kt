package com.nocap.app

import com.nocap.app.core.database.entity.ReviewItemEntity
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
    fun `test EASY rating boosts ease factor and assigns longer intervals`() {
        val now = 1_700_000_000_000L
        val initial = ReviewScheduler.createInitialReviewItem("r1", "a1", "b1", now)

        // 1st review (count 0 -> 1) with EASY: interval = 3
        val rep1 = ReviewScheduler.calculateNextReview(initial, ReviewRating.EASY, now)
        assertEquals(3, rep1.intervalDays)
        assertEquals(2.65f, rep1.easeFactor, 0.001f)

        // 2nd review (count 1 -> 2) with EASY: interval = 5
        val rep2 = ReviewScheduler.calculateNextReview(rep1, ReviewRating.EASY, now)
        assertEquals(5, rep2.intervalDays)
        assertEquals(2.80f, rep2.easeFactor, 0.001f)
    }
}
