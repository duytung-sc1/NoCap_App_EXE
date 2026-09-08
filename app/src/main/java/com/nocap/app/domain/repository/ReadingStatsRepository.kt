package com.nocap.app.domain.repository

import com.nocap.app.core.database.dao.BookReadingTimeAggregate
import kotlinx.coroutines.flow.Flow

data class ReadingStatsSummary(
    val timeTodayMs: Long,
    val timeLast7DaysMs: Long,
    val timeLast30DaysMs: Long,
    val totalSessionsCount: Int,
    val distinctBooksCount: Int
)

data class MostReadBookItem(
    val bookId: String,
    val title: String,
    val author: String?,
    val format: String,
    val coverUrl: String?,
    val totalDurationMs: Long,
    val sessionCount: Int
)

interface ReadingStatsRepository {
    suspend fun getStatsSummary(now: Long = System.currentTimeMillis()): ReadingStatsSummary
    fun observeStatsSummary(): Flow<ReadingStatsSummary>
    suspend fun getMostReadBooks(limit: Int = 10): List<MostReadBookItem>
}
