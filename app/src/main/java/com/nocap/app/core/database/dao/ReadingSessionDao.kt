package com.nocap.app.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nocap.app.core.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

data class BookReadingTimeAggregate(
    @ColumnInfo(name = "book_id") val bookId: String,
    @ColumnInfo(name = "total_duration_ms") val totalDurationMs: Long,
    @ColumnInfo(name = "session_count") val sessionCount: Int
)

@Dao
interface ReadingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Update
    suspend fun updateSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): ReadingSessionEntity?

    @Query("DELETE FROM reading_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM reading_sessions WHERE duration_ms < :minDurationMs AND ended_at IS NOT NULL")
    suspend fun deleteShortSessions(minDurationMs: Long = 5000L)

    @Query("SELECT * FROM reading_sessions WHERE ended_at IS NULL")
    suspend fun getOpenSessions(): List<ReadingSessionEntity>

    @Query("""
        UPDATE reading_sessions
        SET ended_at = :endedAt,
            duration_ms = :durationMs,
            end_progress = :endProgress
        WHERE id = :sessionId AND ended_at IS NULL
    """)
    suspend fun finalizeSession(sessionId: String, endedAt: Long, durationMs: Long, endProgress: Float): Int

    @Query("SELECT COALESCE(SUM(duration_ms), 0) FROM reading_sessions WHERE started_at >= :startTime AND started_at <= :endTime")
    suspend fun getReadingTimeBetween(startTime: Long, endTime: Long): Long

    @Query("SELECT COALESCE(SUM(duration_ms), 0) FROM reading_sessions WHERE started_at >= :startTime")
    fun observeReadingTimeSince(startTime: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM reading_sessions WHERE duration_ms >= 5000")
    suspend fun getTotalSessionCount(): Int

    @Query("SELECT COUNT(DISTINCT book_id) FROM reading_sessions WHERE duration_ms >= 5000")
    suspend fun getDistinctBooksReadCount(): Int

    @Query("""
        SELECT book_id, SUM(duration_ms) as total_duration_ms, COUNT(*) as session_count 
        FROM reading_sessions 
        WHERE duration_ms >= 5000
        GROUP BY book_id 
        ORDER BY total_duration_ms DESC 
        LIMIT :limit
    """)
    suspend fun getMostReadBooks(limit: Int = 10): List<BookReadingTimeAggregate>

    @Query("SELECT * FROM reading_sessions WHERE book_id = :bookId ORDER BY started_at DESC")
    fun observeSessionsForBook(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY started_at DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 20): Flow<List<ReadingSessionEntity>>
}
