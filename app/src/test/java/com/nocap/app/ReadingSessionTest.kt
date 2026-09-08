package com.nocap.app

import com.nocap.app.core.database.dao.BookReadingTimeAggregate
import com.nocap.app.core.database.dao.ReadingSessionDao
import com.nocap.app.core.database.entity.ReadingSessionEntity
import com.nocap.app.domain.session.ReadingSessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSessionTest {

    private class FakeReadingSessionDao : ReadingSessionDao {
        val sessions = mutableMapOf<String, ReadingSessionEntity>()

        override suspend fun insertSession(session: ReadingSessionEntity) {
            sessions[session.id] = session
        }

        override suspend fun updateSession(session: ReadingSessionEntity) {
            sessions[session.id] = session
        }

        override suspend fun getSessionById(id: String): ReadingSessionEntity? {
            return sessions[id]
        }

        override suspend fun deleteSession(id: String) {
            sessions.remove(id)
        }

        override suspend fun deleteShortSessions(minDurationMs: Long) {
            val toRemove = sessions.values.filter { it.durationMs < minDurationMs && it.endedAt != null }.map { it.id }
            toRemove.forEach { sessions.remove(it) }
        }

        override suspend fun getOpenSessions(): List<ReadingSessionEntity> {
            return sessions.values.filter { it.endedAt == null }
        }

        override suspend fun finalizeSession(sessionId: String, endedAt: Long, durationMs: Long, endProgress: Float): Int {
            val existing = sessions[sessionId] ?: return 0
            if (existing.endedAt != null) return 0
            sessions[sessionId] = existing.copy(
                endedAt = endedAt,
                durationMs = durationMs,
                endProgress = endProgress
            )
            return 1
        }

        override suspend fun getReadingTimeBetween(startTime: Long, endTime: Long): Long {
            return sessions.values
                .filter { it.startedAt in startTime..endTime }
                .sumOf { it.durationMs }
        }

        override fun observeReadingTimeSince(startTime: Long): Flow<Long> {
            return flowOf(sessions.values.filter { it.startedAt >= startTime }.sumOf { it.durationMs })
        }

        override suspend fun getTotalSessionCount(): Int {
            return sessions.values.count { it.durationMs >= 5000 }
        }

        override suspend fun getDistinctBooksReadCount(): Int {
            return sessions.values.filter { it.durationMs >= 5000 }.map { it.bookId }.distinct().size
        }

        override suspend fun getMostReadBooks(limit: Int): List<BookReadingTimeAggregate> {
            return sessions.values
                .filter { it.durationMs >= 5000 }
                .groupBy { it.bookId }
                .map { (bId, list) ->
                    BookReadingTimeAggregate(
                        bookId = bId,
                        totalDurationMs = list.sumOf { it.durationMs },
                        sessionCount = list.size
                    )
                }
                .sortedByDescending { it.totalDurationMs }
                .take(limit)
        }

        override fun observeSessionsForBook(bookId: String): Flow<List<ReadingSessionEntity>> {
            return flowOf(sessions.values.filter { it.bookId == bookId })
        }

        override fun observeRecentSessions(limit: Int): Flow<List<ReadingSessionEntity>> {
            return flowOf(sessions.values.sortedByDescending { it.startedAt }.take(limit))
        }
    }

    @Test
    fun `test short session under 5 seconds is discarded`() = runBlocking {
        val dao = FakeReadingSessionDao()
        val manager = ReadingSessionManager(dao)
        val startTime = 1_000_000L

        val sessionId = manager.startSession("book_1", "EPUB", 0.1f, now = startTime)
        assertNotNull(dao.getSessionById(sessionId))

        // End session only 3 seconds later (less than MIN_SESSION_DURATION_MS)
        manager.endSession(sessionId, 0.12f, now = startTime + 3_000L)

        // Must be deleted to avoid cluttering stats
        assertNull(dao.getSessionById(sessionId))
    }

    @Test
    fun `test valid session above 5 seconds is recorded with accurate duration`() = runBlocking {
        val dao = FakeReadingSessionDao()
        val manager = ReadingSessionManager(dao)
        val startTime = 1_000_000L

        val sessionId = manager.startSession("book_1", "EPUB", 0.1f, now = startTime)
        // End session 60 seconds later
        manager.endSession(sessionId, 0.25f, now = startTime + 60_000L)

        val recorded = dao.getSessionById(sessionId)
        assertNotNull(recorded)
        assertEquals(startTime + 60_000L, recorded?.endedAt)
        assertEquals(60_000L, recorded?.durationMs)
        assertEquals(0.25f, recorded?.endProgress ?: 0f, 0.001f)
    }

    @Test
    fun `test crash recovery caps long running sessions at 15 minutes`() = runBlocking {
        val dao = FakeReadingSessionDao()
        val manager = ReadingSessionManager(dao)
        val startTime = 1_000_000L

        // Simulate app crash while reading: session started, never ended
        val sessionId = manager.startSession("book_1", "PDF", 0.05f, now = startTime)
        assertTrue(dao.getOpenSessions().isNotEmpty())

        // App reopens 5 hours later
        val recoveryTime = startTime + (5L * 60 * 60 * 1000)
        manager.recoverStaleSessions(now = recoveryTime)

        val recovered = dao.getSessionById(sessionId)
        assertNotNull(recovered)
        // Duration must be capped at MAX_RECOVERED_SESSION_MS (15 min = 900,000 ms)
        assertEquals(ReadingSessionManager.MAX_RECOVERED_SESSION_MS, recovered?.durationMs)
        assertEquals(startTime + ReadingSessionManager.MAX_RECOVERED_SESSION_MS, recovered?.endedAt)
        assertTrue(dao.getOpenSessions().isEmpty())
    }

    @Test
    fun `test crash recovery discards open sessions shorter than 5 seconds`() = runBlocking {
        val dao = FakeReadingSessionDao()
        val manager = ReadingSessionManager(dao)
        val startTime = 1_000_000L

        // Session started, app immediately died within 2 seconds
        val sessionId = manager.startSession("book_1", "PDF", 0.05f, now = startTime)

        manager.recoverStaleSessions(now = startTime + 2_000L)

        // Must be discarded
        assertNull(dao.getSessionById(sessionId))
        assertTrue(dao.getOpenSessions().isEmpty())
    }

    @Test
    fun `test ending session twice is idempotent and does not overwrite duration`() = runBlocking {
        val dao = FakeReadingSessionDao()
        val manager = ReadingSessionManager(dao)
        val startTime = 1_000_000L

        val sessionId = manager.startSession("book_1", "EPUB", 0.1f, now = startTime)
        manager.endSession(sessionId, 0.2f, now = startTime + 30_000L)

        val firstRecorded = dao.getSessionById(sessionId)
        assertNotNull(firstRecorded)
        assertEquals(30_000L, firstRecorded?.durationMs)
        assertEquals(0.2f, firstRecorded?.endProgress ?: 0f, 0.001f)

        // Second call (e.g. BackHandler + onCleared)
        manager.endSession(sessionId, 0.5f, now = startTime + 90_000L)

        val secondRecorded = dao.getSessionById(sessionId)
        assertEquals(30_000L, secondRecorded?.durationMs) // Still 30s
        assertEquals(startTime + 30_000L, secondRecorded?.endedAt)
        assertEquals(0.2f, secondRecorded?.endProgress ?: 0f, 0.001f)
    }

    @Test
    fun `test endSessionAsync completes and finalizes session`() = runBlocking {
        val dao = FakeReadingSessionDao()
        val manager = ReadingSessionManager(dao)
        val startTime = 1_000_000L

        val sessionId = manager.startSession("book_1", "TXT", 0.0f, now = startTime)
        val job = manager.endSessionAsync(sessionId, 0.4f, now = startTime + 45_000L)
        job.join()

        val recorded = dao.getSessionById(sessionId)
        assertNotNull(recorded)
        assertEquals(45_000L, recorded?.durationMs)
        assertEquals(0.4f, recorded?.endProgress ?: 0f, 0.001f)
    }

    @Test
    fun `test recoverStaleSessions is harmless when all sessions already closed`() = runBlocking {
        val dao = FakeReadingSessionDao()
        val manager = ReadingSessionManager(dao)
        val startTime = 1_000_000L

        val sessionId = manager.startSession("book_1", "EPUB", 0.1f, now = startTime)
        manager.endSession(sessionId, 0.2f, now = startTime + 30_000L)

        // Run recovery 2 hours later
        manager.recoverStaleSessions(now = startTime + (2L * 3600 * 1000))

        val recorded = dao.getSessionById(sessionId)
        assertEquals(30_000L, recorded?.durationMs)
        assertEquals(startTime + 30_000L, recorded?.endedAt)
    }
}

