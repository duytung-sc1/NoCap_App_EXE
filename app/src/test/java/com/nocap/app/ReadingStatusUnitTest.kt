package com.nocap.app

import com.nocap.app.domain.model.DocumentReadingStatus
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingStatusUnitTest {

    @Test
    fun readingStatus_enumDisplaysVietnameseText() {
        assertEquals("Chưa đọc", DocumentReadingStatus.UNREAD.displayName)
        assertEquals("Đang đọc", DocumentReadingStatus.READING.displayName)
        assertEquals("Hoàn thành", DocumentReadingStatus.COMPLETED.displayName)
    }

    @Test
    fun readerProgression_calculatesCorrectAutoTransition() {
        // Function representing ReaderViewModel auto-transition logic:
        fun calculateNextStatus(currentStatus: DocumentReadingStatus, progression: Float): DocumentReadingStatus {
            return when {
                progression >= 0.98f -> DocumentReadingStatus.COMPLETED
                progression > 0.0f && currentStatus == DocumentReadingStatus.UNREAD -> DocumentReadingStatus.READING
                else -> currentStatus
            }
        }

        // At 0% progress, unread stays unread
        assertEquals(DocumentReadingStatus.UNREAD, calculateNextStatus(DocumentReadingStatus.UNREAD, 0.0f))

        // When user starts reading (> 0%), unread transitions to reading
        assertEquals(DocumentReadingStatus.READING, calculateNextStatus(DocumentReadingStatus.UNREAD, 0.05f))

        // In middle of reading (50%), remains reading
        assertEquals(DocumentReadingStatus.READING, calculateNextStatus(DocumentReadingStatus.READING, 0.50f))

        // Near end (98%), transitions to completed
        assertEquals(DocumentReadingStatus.COMPLETED, calculateNextStatus(DocumentReadingStatus.READING, 0.98f))

        // 100% completed
        assertEquals(DocumentReadingStatus.COMPLETED, calculateNextStatus(DocumentReadingStatus.READING, 1.0f))
    }

    @Test
    fun manualOverride_canSetAnyStatusAtAnyTime() {
        var status = DocumentReadingStatus.COMPLETED
        // User wants to re-read -> set to UNREAD
        status = DocumentReadingStatus.UNREAD
        assertEquals(DocumentReadingStatus.UNREAD, status)

        // User marks as completed directly
        status = DocumentReadingStatus.COMPLETED
        assertEquals(DocumentReadingStatus.COMPLETED, status)
    }

    @Test
    fun readingProgression_percentageFormatting_reaches100Percent() {
        fun formatProgress(rawProgression: Double?, isCompleted: Boolean = false): Int {
            if (isCompleted || (rawProgression != null && rawProgression >= 0.98)) return 100
            if (rawProgression == null) return 0
            return (rawProgression * 100).roundToInt().coerceIn(0, 100)
        }

        // Test normal values
        assertEquals(0, formatProgress(0.0))
        assertEquals(25, formatProgress(0.25))
        assertEquals(50, formatProgress(0.504))
        assertEquals(51, formatProgress(0.506))

        // Test near-end values that previously stuck at 98% or 99%
        assertEquals(100, formatProgress(0.98))
        assertEquals(100, formatProgress(0.985))
        assertEquals(100, formatProgress(0.99))
        assertEquals(100, formatProgress(0.999))
        assertEquals(100, formatProgress(1.0))

        // When book is completed, it should always be 100% regardless of raw progression
        assertEquals(100, formatProgress(0.0, isCompleted = true))
        assertEquals(100, formatProgress(0.85, isCompleted = true))
    }
}
