package com.nocap.app.domain.session

import android.content.Context
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.dao.ReadingSessionDao
import com.nocap.app.core.database.entity.ReadingSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class ReadingSessionManager(
    private val readingSessionDao: ReadingSessionDao,
    private val scope: CoroutineScope = processScope
) {

    companion object {
        const val MIN_SESSION_DURATION_MS: Long = 5_000L // 5 seconds
        const val MAX_RECOVERED_SESSION_MS: Long = 15L * 60 * 1000 // 15 minutes max for crashed sessions

        val processScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile
        private var INSTANCE: ReadingSessionManager? = null
        private val profiles = mutableMapOf<String, ReadingSessionManager>()

        fun getInstance(context: Context): ReadingSessionManager {
            return synchronized(this) {
                profiles.getOrPut(com.nocap.app.data.sync.Profiles.active.value) { ReadingSessionManager(
                    readingSessionDao = AppDatabase.getInstance(context).readingSessionDao(),
                    scope = processScope
                ) }
            }
        }
    }

    /**
     * Starts a new reading session when the Reader is opened.
     */
    suspend fun startSession(
        bookId: String,
        format: String,
        startProgress: Float = 0f,
        now: Long = System.currentTimeMillis()
    ): String {
        val sessionId = UUID.randomUUID().toString()
        val entity = ReadingSessionEntity(
            id = sessionId,
            bookId = bookId,
            startedAt = now,
            endedAt = null,
            durationMs = 0L,
            startProgress = startProgress,
            endProgress = startProgress,
            format = format
        )
        readingSessionDao.insertSession(entity)
        return sessionId
    }

    /**
     * Finishes an active reading session when the Reader is closed or backgrounded.
     * Discards very short sessions (< 5 seconds).
     * Uses atomic finalizeSession query to prevent race conditions and double-closing.
     */
    suspend fun endSession(
        sessionId: String,
        endProgress: Float,
        now: Long = System.currentTimeMillis()
    ) {
        val session = readingSessionDao.getSessionById(sessionId) ?: return
        if (session.endedAt != null) return // Already ended
        val duration = maxOf(0L, now - session.startedAt)

        if (duration < MIN_SESSION_DURATION_MS) {
            // Accidental open/exit; discard
            readingSessionDao.deleteSession(sessionId)
            return
        }

        readingSessionDao.finalizeSession(
            sessionId = sessionId,
            endedAt = now,
            durationMs = duration,
            endProgress = endProgress
        )
    }

    /**
     * Non-cancellable async trigger for ending a session from lifecycle callbacks or UI dispose.
     */
    fun endSessionAsync(
        sessionId: String,
        endProgress: Float,
        now: Long = System.currentTimeMillis()
    ): Job {
        return scope.launch {
            endSession(sessionId, endProgress, now)
        }
    }

    /**
     * Bounded crash recovery: scans for unclosed sessions from previous app runs.
     * Capped at MAX_RECOVERED_SESSION_MS to avoid inflated stats.
     */
    suspend fun recoverStaleSessions(now: Long = System.currentTimeMillis()) {
        val openSessions = readingSessionDao.getOpenSessions()
        for (session in openSessions) {
            val rawDuration = now - session.startedAt
            val cappedDuration = minOf(rawDuration, MAX_RECOVERED_SESSION_MS)
            if (cappedDuration < MIN_SESSION_DURATION_MS) {
                readingSessionDao.deleteSession(session.id)
            } else {
                readingSessionDao.finalizeSession(
                    sessionId = session.id,
                    endedAt = session.startedAt + cappedDuration,
                    durationMs = cappedDuration,
                    endProgress = session.endProgress
                )
            }
        }
    }

    /**
     * Async recovery triggered during application startup.
     */
    fun recoverStaleSessionsAsync(now: Long = System.currentTimeMillis()): Job {
        return scope.launch {
            recoverStaleSessions(now)
        }
    }
}
