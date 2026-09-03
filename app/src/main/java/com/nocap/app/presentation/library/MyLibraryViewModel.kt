package com.nocap.app.presentation.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.catalog.LocalCatalogRepository
import com.nocap.app.data.download.LocalBookDownloadRepository
import com.nocap.app.data.favorite.LocalFavoriteRepository
import com.nocap.app.data.importer.LocalImportBookRepository
import com.nocap.app.data.library.LocalLibraryRepository
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.repository.BookDownloadRepository
import com.nocap.app.domain.repository.FavoriteRepository
import com.nocap.app.domain.repository.ImportBookRepository
import com.nocap.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

sealed interface LibraryEvent {
    data class ImportSuccess(val bookTitle: String) : LibraryEvent
    data class ShowMessage(val message: String) : LibraryEvent
}

data class MyLibraryUiState(
    val books: List<LibraryBook> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedSort: LibrarySort = LibrarySort.RECENTLY_READ,
    val isLoading: Boolean = true,
    val isImporting: Boolean = false
)

class MyLibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: BookDownloadRepository,
    private val favoriteRepository: FavoriteRepository,
    private val importRepository: ImportBookRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    private val _selectedSort = MutableStateFlow(LibrarySort.RECENTLY_READ)
    private val _isImporting = MutableStateFlow(false)

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    val uiState: StateFlow<MyLibraryUiState> = combine(
        libraryRepository.observeLibraryBooks(),
        _searchQuery,
        _selectedFilter,
        _selectedSort,
        _isImporting
    ) { libraryBooks, query, filter, sort, isImporting ->
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
                it.downloadedBook.downloadStatus == com.nocap.app.domain.model.DownloadStatus.COMPLETED
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
            isLoading = false,
            isImporting = isImporting
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

    fun onDeleteImportedBook(bookId: String) {
        viewModelScope.launch {
            val result = importRepository.deleteImportedBook(bookId)
            if (result.isSuccess) {
                _events.emit(LibraryEvent.ShowMessage("Đã xóa sách khỏi thư viện"))
            } else {
                _events.emit(LibraryEvent.ShowMessage("Không thể xóa sách"))
            }
        }
    }

    fun onImportEpub(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = importRepository.importEpub(uri)
            _isImporting.value = false

            result.onSuccess {
                _events.emit(LibraryEvent.ShowMessage("Nhập sách thành công!"))
            }.onFailure { error ->
                _events.emit(LibraryEvent.ShowMessage(error.message ?: "Lỗi khi nhập sách"))
            }
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
        fun provideFactory(context: Context): ViewModelProvider.Factory =
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
                    val importRepo = LocalImportBookRepository(
                        context = context,
                        catalogDao = db.catalogDao(),
                        downloadDao = db.downloadDao(),
                        progressDao = db.progressDao(),
                        bookmarkDao = db.bookmarkDao(),
                        favoriteDao = db.favoriteDao()
                    )
                    return MyLibraryViewModel(libraryRepo, downloadRepo, favRepo, importRepo) as T
                }
            }
    }
}
