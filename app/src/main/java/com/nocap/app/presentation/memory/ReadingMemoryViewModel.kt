package com.nocap.app.presentation.memory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.dao.BookmarkDao
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.core.database.dao.ReviewItemWithDetails
import com.nocap.app.data.review.LocalReviewRepository
import com.nocap.app.data.search.LocalKnowledgeSearchRepository
import com.nocap.app.data.stats.LocalReadingStatsRepository
import com.nocap.app.domain.export.KnowledgeExporter
import com.nocap.app.domain.export.PdfKnowledgeExporter
import com.nocap.app.domain.model.BookmarkWithBook
import com.nocap.app.domain.model.HighlightWithBook
import com.nocap.app.domain.repository.AutoGenResult
import com.nocap.app.domain.repository.KnowledgeSearchRepository
import com.nocap.app.domain.repository.MostReadBookItem
import com.nocap.app.domain.repository.ReadingStatsRepository
import com.nocap.app.domain.repository.ReadingStatsSummary
import com.nocap.app.domain.repository.ReviewRepository
import com.nocap.app.domain.repository.ReviewStatsSummary
import com.nocap.app.domain.review.ReviewScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

enum class MemoryFilter {
    ALL,
    DUE,
    LEARNING,
    MASTERED
}

data class ReadingMemoryUiState(
    val isLoading: Boolean = true,
    val statsSummary: ReadingStatsSummary = ReadingStatsSummary(0, 0, 0, 0, 0),
    val dueTodayCount: Int = 0,
    val reviewStats: ReviewStatsSummary = ReviewStatsSummary(0, 0, 0, 0),
    val allReviewItems: List<ReviewItemWithDetails> = emptyList(),
    val selectedFilter: MemoryFilter = MemoryFilter.ALL,
    val recentHighlights: List<HighlightWithBook> = emptyList(),
    val recentNotes: List<HighlightWithBook> = emptyList(),
    val recentBookmarks: List<BookmarkWithBook> = emptyList(),
    val mostReadBooks: List<MostReadBookItem> = emptyList(),
    val allBooks: List<CatalogBookEntity> = emptyList()
) {
    val filteredReviewItems: List<ReviewItemWithDetails>
        get() {
            val now = System.currentTimeMillis()
            return when (selectedFilter) {
                MemoryFilter.ALL -> allReviewItems
                MemoryFilter.DUE -> allReviewItems.filter { it.reviewItem.nextReviewAt <= now }
                MemoryFilter.LEARNING -> allReviewItems.filter {
                    it.reviewItem.nextReviewAt > now && it.reviewItem.intervalDays < ReviewScheduler.MASTERED_INTERVAL_DAYS
                }
                MemoryFilter.MASTERED -> allReviewItems.filter {
                    it.reviewItem.nextReviewAt > now && it.reviewItem.intervalDays >= ReviewScheduler.MASTERED_INTERVAL_DAYS
                }
            }
        }
}

class ReadingMemoryViewModel(
    private val statsRepository: ReadingStatsRepository,
    private val reviewRepository: ReviewRepository,
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    private val catalogDao: CatalogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingMemoryUiState())
    val uiState: StateFlow<ReadingMemoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val now = System.currentTimeMillis()

            // Auto-repair unreviewed items scheduled in future to today
            withContext(Dispatchers.IO) {
                reviewRepository.resetUnreviewedItemsToNow(now)
            }

            val stats = withContext(Dispatchers.IO) {
                statsRepository.getStatsSummary(now)
            }
            val dueCount = withContext(Dispatchers.IO) {
                reviewRepository.getDueItemsCount(now)
            }
            val revStats = withContext(Dispatchers.IO) {
                reviewRepository.getReviewStats(now)
            }
            val mostRead = withContext(Dispatchers.IO) {
                statsRepository.getMostReadBooks(5)
            }

            val allCatalogBooks = withContext(Dispatchers.IO) {
                catalogDao.getAllBooks()
            }
            val books = allCatalogBooks.associateBy { it.id }

            val allHighlights = withContext(Dispatchers.IO) {
                highlightDao.getAllHighlights()
            }
            val allBookmarks = withContext(Dispatchers.IO) {
                bookmarkDao.getAllBookmarks()
            }

            val allRevItems = withContext(Dispatchers.IO) {
                reviewRepository.getAllReviewItemsWithDetails()
            }
            val reviewAnnotationIds = allRevItems.filter { it.reviewItem.isEnabled }.map { it.reviewItem.annotationId }.toSet()

            val recentHls = allHighlights
                .filter { it.note.isNullOrBlank() }
                .take(10)
                .mapNotNull { hl ->
                    books[hl.bookId]?.let { book ->
                        HighlightWithBook(
                            highlight = hl,
                            book = book,
                            isUnderReview = reviewAnnotationIds.contains(hl.id)
                        )
                    }
                }

            val recentNotes = allHighlights
                .filter { !it.note.isNullOrBlank() }
                .take(10)
                .mapNotNull { note ->
                    books[note.bookId]?.let { book ->
                        HighlightWithBook(
                            highlight = note,
                            book = book,
                            isUnderReview = reviewAnnotationIds.contains(note.id)
                        )
                    }
                }

            val recentBms = allBookmarks
                .filter { !it.isDeleted }
                .take(10)
                .mapNotNull { bm ->
                    books[bm.bookId]?.let { book ->
                        BookmarkWithBook(
                            bookmark = bm,
                            book = book
                        )
                    }
                }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    statsSummary = stats,
                    dueTodayCount = dueCount,
                    reviewStats = revStats,
                    allReviewItems = allRevItems,
                    recentHighlights = recentHls,
                    recentNotes = recentNotes,
                    recentBookmarks = recentBms,
                    mostReadBooks = mostRead,
                    allBooks = allCatalogBooks
                )
            }
        }
    }

    fun setFilter(filter: MemoryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    suspend fun autoGenerateReviewDetailed(onlyWithNotes: Boolean = false): Result<AutoGenResult> {
        val result = withContext(Dispatchers.IO) {
            reviewRepository.autoGenerateReviewItemsDetailed(null, onlyWithNotes)
        }
        if (result.isSuccess) {
            loadData()
        }
        return result
    }

    suspend fun autoGenerateReview(onlyWithNotes: Boolean = false): Result<Int> {
        val result = withContext(Dispatchers.IO) {
            reviewRepository.autoGenerateReviewItems(null, onlyWithNotes)
        }
        if (result.isSuccess) {
            loadData()
        }
        return result
    }

    fun markItemAsMastered(annotationId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                reviewRepository.markAsMastered(annotationId)
            }
            loadData()
        }
    }

    fun removeFromReview(annotationId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                reviewRepository.removeFromReview(annotationId)
            }
            loadData()
        }
    }

    fun toggleReview(annotationId: String, bookId: String, currentInReview: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (currentInReview) {
                    reviewRepository.removeFromReview(annotationId)
                } else {
                    reviewRepository.addToReview(annotationId, bookId)
                }
            }
            loadData()
        }
    }

    private suspend fun getExportData(
        bookId: String? = null,
        topicColor: String? = null
    ): List<Triple<CatalogBookEntity, List<HighlightEntity>, List<BookmarkEntity>>> = withContext(Dispatchers.IO) {
        val allBooks = catalogDao.getAllBooks()
        val targetBooks = if (!bookId.isNullOrBlank()) allBooks.filter { it.id == bookId } else allBooks
        val allHighlights = highlightDao.getAllHighlights()
        val filteredHighlights = if (!topicColor.isNullOrBlank() && topicColor != "ALL") {
            allHighlights.filter { it.color.equals(topicColor, ignoreCase = true) }
        } else {
            allHighlights
        }
        val allBookmarks = bookmarkDao.getAllBookmarks()

        val hlMap = filteredHighlights.groupBy { it.bookId }
        val bmMap = allBookmarks.groupBy { it.bookId }

        targetBooks.mapNotNull { book ->
            val hls = hlMap[book.id].orEmpty()
            val bms = bmMap[book.id].orEmpty()
            if (hls.isNotEmpty() || bms.isNotEmpty()) {
                Triple(book, hls, bms)
            } else null
        }
    }

    suspend fun exportMarkdown(
        outputStream: OutputStream,
        bookId: String? = null,
        topicColor: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val booksWithData = getExportData(bookId, topicColor)
            val markdown = if (!topicColor.isNullOrBlank() && topicColor != "ALL") {
                KnowledgeExporter.formatTopicKnowledge(topicColor, booksWithData)
            } else {
                KnowledgeExporter.formatAllKnowledge(booksWithData)
            }
            KnowledgeExporter.writeMarkdownToStream(outputStream, markdown)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportPdf(
        outputStream: OutputStream,
        bookId: String? = null,
        topicColor: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val booksWithData = getExportData(bookId, topicColor)
            val titleOverride = if (!topicColor.isNullOrBlank() && topicColor != "ALL") {
                "NoCap — Tổng hợp theo màu: $topicColor"
            } else if (!bookId.isNullOrBlank()) {
                val book = booksWithData.firstOrNull()?.first
                "NoCap — ${book?.userTitleOverride ?: book?.title ?: "Tài liệu"}"
            } else null

            PdfKnowledgeExporter.exportToPdf(booksWithData, outputStream, titleOverride)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportAnki(
        outputStream: OutputStream,
        bookId: String? = null,
        topicColor: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val booksWithData = getExportData(bookId, topicColor)
            val highlightsWithBook = booksWithData.flatMap { (book, hls, _) ->
                hls.map { hl ->
                    HighlightWithBook(highlight = hl, book = book)
                }
            }
            val tsv = KnowledgeExporter.formatAnkiCards(highlightsWithBook)
            KnowledgeExporter.writeStringToStream(outputStream, tsv)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportAllKnowledge(outputStream: OutputStream): Boolean {
        return exportMarkdown(outputStream)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val statsRepo = LocalReadingStatsRepository(db.readingSessionDao(), db.catalogDao())
                val reviewRepo = LocalReviewRepository(db.reviewDao(), db.highlightDao())

                return ReadingMemoryViewModel(
                    statsRepository = statsRepo,
                    reviewRepository = reviewRepo,
                    highlightDao = db.highlightDao(),
                    bookmarkDao = db.bookmarkDao(),
                    catalogDao = db.catalogDao()
                ) as T
            }
        }
    }
}
