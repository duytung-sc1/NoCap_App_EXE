package com.nocap.app.presentation.memory.annotations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.dao.BookmarkDao
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.util.VietnameseUtils
import com.nocap.app.data.review.LocalReviewRepository
import com.nocap.app.domain.model.BookmarkWithBook
import com.nocap.app.domain.model.HighlightWithBook
import com.nocap.app.domain.repository.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AnnotationTab(val label: String) {
    ALL("Tất cả"),
    HIGHLIGHTS("Đoạn trích"),
    NOTES("Ghi chú"),
    BOOKMARKS("Đánh dấu"),
    IN_REVIEW("Đang ôn tập")
}

data class GlobalAnnotationsUiState(
    val isLoading: Boolean = true,
    val selectedTab: AnnotationTab = AnnotationTab.ALL,
    val searchQuery: String = "",
    val highlights: List<HighlightWithBook> = emptyList(),
    val bookmarks: List<BookmarkWithBook> = emptyList()
)

class GlobalAnnotationsViewModel(
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    private val catalogDao: CatalogDao,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalAnnotationsUiState())
    val uiState: StateFlow<GlobalAnnotationsUiState> = _uiState.asStateFlow()

    init {
        loadAnnotations()
    }

    fun loadAnnotations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val books = withContext(Dispatchers.IO) { catalogDao.getAllBooks().associateBy { it.id } }
            val allHighlights = withContext(Dispatchers.IO) { highlightDao.getAllHighlights() }
            val allBookmarks = withContext(Dispatchers.IO) { bookmarkDao.getAllBookmarks() }
            val reviewItems = withContext(Dispatchers.IO) { reviewRepository.getDueItemsWithDetails(Long.MAX_VALUE) }
            val reviewAnnotationIds = reviewItems.filter { it.reviewItem.isEnabled }.map { it.reviewItem.annotationId }.toSet()

            val hlWithBooks = allHighlights.mapNotNull { hl ->
                books[hl.bookId]?.let { book ->
                    HighlightWithBook(
                        highlight = hl,
                        book = book,
                        isUnderReview = reviewAnnotationIds.contains(hl.id)
                    )
                }
            }

            val bmWithBooks = allBookmarks.filter { !it.isDeleted }.mapNotNull { bm ->
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
                    highlights = hlWithBooks,
                    bookmarks = bmWithBooks
                )
            }
        }
    }

    fun selectTab(tab: AnnotationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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
            loadAnnotations()
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                highlightDao.deleteHighlight(id)
            }
            loadAnnotations()
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                bookmarkDao.softDeleteBookmark(id)
            }
            loadAnnotations()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val reviewRepo = LocalReviewRepository(db.reviewDao())
                return GlobalAnnotationsViewModel(
                    highlightDao = db.highlightDao(),
                    bookmarkDao = db.bookmarkDao(),
                    catalogDao = db.catalogDao(),
                    reviewRepository = reviewRepo
                ) as T
            }
        }
    }
}
