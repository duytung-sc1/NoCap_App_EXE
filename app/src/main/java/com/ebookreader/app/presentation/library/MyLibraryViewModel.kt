package com.ebookreader.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ebookreader.app.core.database.AppDatabase
import com.ebookreader.app.data.catalog.LocalCatalogRepository
import com.ebookreader.app.data.download.LocalBookDownloadRepository
import com.ebookreader.app.data.favorite.LocalFavoriteRepository
import com.ebookreader.app.data.library.LocalLibraryRepository
import com.ebookreader.app.domain.model.LibraryBook
import com.ebookreader.app.domain.repository.BookDownloadRepository
import com.ebookreader.app.domain.repository.FavoriteRepository
import com.ebookreader.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter(val displayName: String) {
    ALL("Tất cả"),
    DOWNLOADED("Đã tải"),
    READING("Đang đọc")
}

enum class LibrarySort(val displayName: String) {
    RECENTLY_READ("Gần đây"),
    TITLE("Tên sách (A-Z)"),
    DOWNLOAD_DATE("Ngày tải")
}

data class MyLibraryUiState(
    val books: List<LibraryBook> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedSort: LibrarySort = LibrarySort.RECENTLY_READ,
    val isLoading: Boolean = true
)

class MyLibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: BookDownloadRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    private val _selectedSort = MutableStateFlow(LibrarySort.RECENTLY_READ)

    val uiState: StateFlow<MyLibraryUiState> = combine(
        libraryRepository.observeLibraryBooks(),
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { libraryBooks, query, filter, sort ->
        // 1. Search filter
        val searchFiltered = if (query.isBlank()) {
            libraryBooks
        } else {
            val q = query.trim().lowercase()
            libraryBooks.filter {
                it.book.title.lowercase().contains(q) || it.book.author.lowercase().contains(q)
            }
        }

        // 2. Category / status filter
        val statusFiltered = when (filter) {
            LibraryFilter.ALL -> searchFiltered
            LibraryFilter.DOWNLOADED -> searchFiltered.filter {
                it.downloadedBook.downloadStatus == com.ebookreader.app.domain.model.DownloadStatus.COMPLETED
            }
            LibraryFilter.READING -> searchFiltered.filter {
                it.readingProgress != null && it.readingProgress.progression > 0f
            }
        }

        // 3. Sort
        val sortedBooks = when (sort) {
            LibrarySort.RECENTLY_READ -> statusFiltered.sortedByDescending {
                it.readingProgress?.lastReadAt ?: it.downloadedBook.downloadedAt ?: 0L
            }
            LibrarySort.TITLE -> statusFiltered.sortedBy { it.book.title.lowercase() }
            LibrarySort.DOWNLOAD_DATE -> statusFiltered.sortedByDescending {
                it.downloadedBook.downloadedAt ?: 0L
            }
        }

        MyLibraryUiState(
            books = sortedBooks,
            searchQuery = query,
            selectedFilter = filter,
            selectedSort = sort,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MyLibraryUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onFilterChange(filter: LibraryFilter) {
        _selectedFilter.update { filter }
    }

    fun onSortChange(sort: LibrarySort) {
        _selectedSort.update { sort }
    }

    fun onRemoveDownload(bookId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadedBook(bookId)
        }
    }

    fun onUpdateBook(bookId: String) {
        viewModelScope.launch {
            downloadRepository.startDownload(bookId)
        }
    }

    fun onToggleFavorite(bookId: String) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(bookId)
        }
    }

    companion object {
        fun provideFactory(context: android.content.Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val catalogRepo = LocalCatalogRepository(db.progressDao())
                    val libraryRepo = LocalLibraryRepository(
                        downloadDao = db.downloadDao(),
                        progressDao = db.progressDao(),
                        favoriteDao = db.favoriteDao(),
                        catalogDao = db.catalogDao(),
                        catalogRepository = catalogRepo
                    )
                    val downloadRepo = LocalBookDownloadRepository(
                        context = context,
                        downloadDao = db.downloadDao(),
                        catalogDao = db.catalogDao(),
                        catalogRepository = catalogRepo
                    )
                    val favRepo = LocalFavoriteRepository(
                        favoriteDao = db.favoriteDao(),
                        catalogDao = db.catalogDao(),
                        catalogRepository = catalogRepo
                    )
                    return MyLibraryViewModel(libraryRepo, downloadRepo, favRepo) as T
                }
            }
    }
}
