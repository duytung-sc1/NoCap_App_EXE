package com.nocap.app.data.stats

import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.ReadingSessionDao
import com.nocap.app.domain.repository.MostReadBookItem
import com.nocap.app.domain.repository.ReadingStatsRepository
import com.nocap.app.domain.repository.ReadingStatsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class LocalReadingStatsRepository(
    private val readingSessionDao: ReadingSessionDao,
    private val catalogDao: CatalogDao
) : ReadingStatsRepository {

    override suspend fun getStatsSummary(now: Long): ReadingStatsSummary {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000L
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L

        val timeTodayMs = readingSessionDao.getReadingTimeBetween(startOfToday, Long.MAX_VALUE)
        val time7DaysMs = readingSessionDao.getReadingTimeBetween(sevenDaysAgo, Long.MAX_VALUE)
        val time30DaysMs = readingSessionDao.getReadingTimeBetween(thirtyDaysAgo, Long.MAX_VALUE)

        val totalSessionsCount = readingSessionDao.getTotalSessionCount()
        val distinctBooksCount = readingSessionDao.getDistinctBooksReadCount()

        return ReadingStatsSummary(
            timeTodayMs = timeTodayMs,
            timeLast7DaysMs = time7DaysMs,
            timeLast30DaysMs = time30DaysMs,
            totalSessionsCount = totalSessionsCount,
            distinctBooksCount = distinctBooksCount
        )
    }

    override fun observeStatsSummary(): Flow<ReadingStatsSummary> {
        return readingSessionDao.observeReadingTimeSince(0).map {
            getStatsSummary(System.currentTimeMillis())
        }
    }

    override suspend fun getMostReadBooks(limit: Int): List<MostReadBookItem> {
        val aggregates = readingSessionDao.getMostReadBooks(limit)
        return aggregates.map { agg ->
            val book = catalogDao.getBookById(agg.bookId)
            MostReadBookItem(
                bookId = agg.bookId,
                title = book?.userTitleOverride ?: book?.title ?: "Tài liệu #${agg.bookId}",
                author = book?.userAuthorOverride ?: book?.author,
                format = book?.format?.name ?: "UNKNOWN",
                coverUrl = book?.customCoverPath ?: book?.coverUrl,
                totalDurationMs = agg.totalDurationMs,
                sessionCount = agg.sessionCount
            )
        }
    }
}
