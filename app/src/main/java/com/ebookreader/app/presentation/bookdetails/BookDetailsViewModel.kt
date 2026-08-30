package com.ebookreader.app.presentation.bookdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ebookreader.app.core.database.AppDatabase
import com.ebookreader.app.data.catalog.LocalCatalogRepository
import com.ebookreader.app.data.download.LocalBookDownloadRepository
import com.ebookreader.app.data.favorite.LocalFavoriteRepository
import com.ebookreader.app.domain.model.CatalogBook
import com.ebookreader.app.domain.model.DownloadStatus
import com.ebookreader.app.domain.model.DownloadedBook
import com.ebookreader.app.domain.model.ReadingProgress
import com.ebookreader.app.domain.repository.BookDownloadRepository
import com.ebookreader.app.domain.repository.CatalogRepository
import com.ebookreader.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookPrimaryAction {
    data object Download : BookPrimaryAction
    data class Downloading(val progress: Float) : BookPrimaryAction
    data object Read : BookPrimaryAction
    data class Update(val newVersion: Long) : BookPrimaryAction
    data class Retry(val error: String?) : BookPrimaryAction
}

data class BookDetailsUiState(
    val book: CatalogBook? = null,
    val isFavorite: Boolean = false,
    val downloadedBook: DownloadedBook? = null,
    val primaryAction: BookPrimaryAction = BookPrimaryAction.Download,
    val readingProgress: ReadingProgress? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class BookDetailsViewModel(
    private val bookId: String,
    private val catalogRepository: CatalogRepository,
    private val favoriteRepository: FavoriteRepository,
    private val downloadRepository: BookDownloadRepository
) : ViewModel() {

    private val _book = MutableStateFlow<CatalogBook?>(null)

    val uiState: StateFlow<BookDetailsUiState> = combine(
        _book,
        favoriteRepository.observeIsFavorite(bookId),
        downloadRepository.observeDownload(bookId)
    ) { book, isFavorite, downloadedBook ->
        val primaryAction = computePrimaryAction(book, downloadedBook)
        BookDetailsUiState(
            book = book,
            isFavorite = isFavorite,
            downloadedBook = downloadedBook,
            primaryAction = primaryAction,
            readingProgress = null,
            isLoading = book == null
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BookDetailsUiState(isLoading = true)
    )

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val loaded = catalogRepository.getBookById(bookId)
            _book.value = loaded
        }
    }

    private fun computePrimaryAction(
        book: CatalogBook?,
        download: DownloadedBook?
    ): BookPrimaryAction {
        if (download == null || download.downloadStatus == DownloadStatus.CANCELLED) {
            return BookPrimaryAction.Download
        }
        return when (download.downloadStatus) {
            DownloadStatus.PENDING -> BookPrimaryAction.Downloading(0f)
            DownloadStatus.DOWNLOADING -> BookPrimaryAction.Downloading(download.downloadProgress)
            DownloadStatus.FAILED -> BookPrimaryAction.Retry(download.lastError)
            DownloadStatus.COMPLETED -> {
                if (book != null && book.contentVersion > download.downloadedContentVersion) {
                    BookPrimaryAction.Update(book.contentVersion)
                } else {
                    BookPrimaryAction.Read
                }
            }
            DownloadStatus.CANCELLED -> BookPrimaryAction.Download
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(bookId)
        }
    }

    fun onStartDownload() {
        viewModelScope.launch {
            downloadRepository.startDownload(bookId)
        }
    }

    fun onCancelDownload() {
        viewModelScope.launch {
            downloadRepository.cancelDownload(bookId)
        }
    }

    fun onRetryDownload() {
        viewModelScope.launch {
            downloadRepository.retryDownload(bookId)
        }
    }

    fun onDeleteDownload() {
        viewModelScope.launch {
            downloadRepository.deleteDownloadedBook(bookId)
        }
    }

    companion object {
        fun provideFactory(
            bookId: String,
            context: android.content.Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val catalogRepo = LocalCatalogRepository()
                val favRepo = LocalFavoriteRepository(db.favoriteDao(), db.catalogDao(), catalogRepo)
                val downloadRepo = LocalBookDownloadRepository(context, db.downloadDao(), db.catalogDao(), catalogRepo)
                return BookDetailsViewModel(bookId, catalogRepo, favRepo, downloadRepo) as T
            }
        }
    }
}
