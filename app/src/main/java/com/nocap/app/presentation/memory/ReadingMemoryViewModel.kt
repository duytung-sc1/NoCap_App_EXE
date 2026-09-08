package com.nocap.app.presentation.memory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.dao.BookmarkDao
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.data.review.LocalReviewRepository
import com.nocap.app.data.search.LocalKnowledgeSearchRepository
import com.nocap.app.data.stats.LocalReadingStatsRepository
import com.nocap.app.domain.export.KnowledgeExporter
import com.nocap.app.domain.model.BookmarkWithBook
import com.nocap.app.domain.model.HighlightWithBook
import com.nocap.app.domain.repository.KnowledgeSearchRepository
import com.nocap.app.domain.repository.MostReadBookItem
import com.nocap.app.domain.repository.ReadingStatsRepository
import com.nocap.app.domain.repository.ReadingStatsSummary
import com.nocap.app.domain.repository.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

data class ReadingMemoryUiState(
    val isLoading: Boolean = true,
    val statsSummary: ReadingStatsSummary = ReadingStatsSummary(0, 0, 0, 0, 0),
    val dueTodayCount: Int = 0,
    val recentHighlights: List<HighlightWithBook> = emptyList(),
    val recentNotes: List<HighlightWithBook> = emptyList(),
    val recentBookmarks: List<BookmarkWithBook> = emptyList(),
    val mostReadBooks: List<MostReadBookItem> = emptyList()
)

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

            val stats = withContext(Dispatchers.IO) {
                statsRepository.getStatsSummary(now)
            }
            val dueCount = withContext(Dispatchers.IO) {
                reviewRepository.getDueItemsCount(now)
            }
            val mostRead = withContext(Dispatchers.IO) {
                statsRepository.getMostReadBooks(5)
            }

            val books = withContext(Dispatchers.IO) {
                catalogDao.getAllBooks().associateBy { it.id }
            }

            val allHighlights = withContext(Dispatchers.IO) {
                highlightDao.getAllHighlights()
            }
            val allBookmarks = withContext(Dispatchers.IO) {
                bookmarkDao.getAllBookmarks()
            }

            // Check which highlights are currently in review queue
            val reviewItems = withContext(Dispatchers.IO) {
                reviewRepository.getDueItemsWithDetails(Long.MAX_VALUE)
            }
            val reviewAnnotationIds = reviewItems.filter { it.reviewItem.isEnabled }.map { it.reviewItem.annotationId }.toSet()

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
                    recentHighlights = recentHls,
                    recentNotes = recentNotes,
                    recentBookmarks = recentBms,
                    mostReadBooks = mostRead
                )
            }
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

    suspend fun exportAllKnowledge(outputStream: OutputStream): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val books = catalogDao.getAllBooks()
                val allHighlights = highlightDao.getAllHighlights()
                val allBookmarks = bookmarkDao.getAllBookmarks()

                val hlMap = allHighlights.groupBy { it.bookId }
                val bmMap = allBookmarks.groupBy { it.bookId }

                val booksWithData = books.mapNotNull { book ->
                    val hls = hlMap[book.id].orEmpty()
                    val bms = bmMap[book.id].orEmpty()
                    if (hls.isNotEmpty() || bms.isNotEmpty()) {
                        Triple(book, hls, bms)
                    } else null
                }

                val markdown = KnowledgeExporter.formatAllKnowledge(booksWithData)
                KnowledgeExporter.writeMarkdownToStream(outputStream, markdown)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val statsRepo = LocalReadingStatsRepository(db.readingSessionDao(), db.catalogDao())
                val reviewRepo = LocalReviewRepository(db.reviewDao())

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
