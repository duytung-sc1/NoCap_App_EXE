package com.ebookreader.app.presentation.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ebookreader.app.core.database.AppDatabase
import com.ebookreader.app.data.catalog.LocalCatalogRepository
import com.ebookreader.app.data.download.LocalBookDownloadRepository
import com.ebookreader.app.data.favorite.LocalFavoriteRepository
import com.ebookreader.app.data.library.LocalLibraryRepository
import com.ebookreader.app.data.reader.ReadiumPublicationManager
import com.ebookreader.app.domain.model.DownloadStatus
import com.ebookreader.app.domain.model.ReadingProgress
import com.ebookreader.app.domain.model.TocItem
import com.ebookreader.app.domain.repository.BookDownloadRepository
import com.ebookreader.app.domain.repository.CatalogRepository
import com.ebookreader.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.io.File

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Ready(
        val publication: Publication,
        val initialLocator: Locator?,
        val toc: List<TocItem>,
        val bookTitle: String
    ) : ReaderUiState
    data object BookNotDownloaded : ReaderUiState
    data object FileNotFound : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

class ReaderViewModel(
    private val bookId: String,
    private val downloadRepository: BookDownloadRepository,
    private val libraryRepository: LibraryRepository,
    private val catalogRepository: CatalogRepository,
    private val publicationManager: ReadiumPublicationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentPublication: Publication? = null
    private var lastSavedTime = 0L

    init {
        loadPublication()
    }

    fun loadPublication() {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading

            val downloaded = downloadRepository.observeDownload(bookId).first()
            if (downloaded == null || downloaded.downloadStatus != DownloadStatus.COMPLETED) {
                _uiState.value = ReaderUiState.BookNotDownloaded
                return@launch
            }

            val file = File(downloaded.localFilePath)
            if (!file.exists() || file.length() == 0L) {
                _uiState.value = ReaderUiState.FileNotFound
                return@launch
            }

            val catalogBook = catalogRepository.getBookById(bookId)
            val title = catalogBook?.title ?: "Ebook"

            val savedProgress = libraryRepository.observeBookProgress(bookId).first()
            val initialLocator = publicationManager.deserializeLocator(savedProgress?.locatorJson)

            val openResult = publicationManager.openPublication(file)
            openResult.onSuccess { publication ->
                currentPublication?.let { publicationManager.closePublication(it) }
                currentPublication = publication
                val toc = publicationManager.extractTableOfContents(publication)
                _uiState.value = ReaderUiState.Ready(
                    publication = publication,
                    initialLocator = initialLocator,
                    toc = toc,
                    bookTitle = title
                )
            }.onFailure { error ->
                _uiState.value = ReaderUiState.Error(error.message ?: "Failed to open book")
            }
        }
    }

    fun onLocationChanged(locator: Locator) {
        val now = System.currentTimeMillis()
        if (now - lastSavedTime < 500) return
        lastSavedTime = now

        viewModelScope.launch {
            val progression = (locator.locations.progression ?: 0.0).toFloat().coerceIn(0f, 1f)
            val chapterTitle = locator.title
            val locatorJson = publicationManager.serializeLocator(locator)

            val progress = ReadingProgress(
                bookId = bookId,
                locatorJson = locatorJson,
                progression = progression,
                chapterTitle = chapterTitle,
                lastReadAt = now
            )
            libraryRepository.saveReadingProgress(progress)
        }
    }

    fun saveCurrentLocationImmediately(locator: Locator?) {
        if (locator == null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val progression = (locator.locations.progression ?: 0.0).toFloat().coerceIn(0f, 1f)
            val chapterTitle = locator.title
            val locatorJson = publicationManager.serializeLocator(locator)

            val progress = ReadingProgress(
                bookId = bookId,
                locatorJson = locatorJson,
                progression = progression,
                chapterTitle = chapterTitle,
                lastReadAt = now
            )
            libraryRepository.saveReadingProgress(progress)
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentPublication?.let { publicationManager.closePublication(it) }
        currentPublication = null
    }

    companion object {
        fun provideFactory(
            bookId: String,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val catalogRepo = LocalCatalogRepository(db.progressDao())
                val downloadRepo = LocalBookDownloadRepository(context, db.downloadDao(), db.catalogDao(), catalogRepo)
                val favRepo = LocalFavoriteRepository(db.favoriteDao(), db.catalogDao(), catalogRepo)
                val libraryRepo = LocalLibraryRepository(db.downloadDao(), db.progressDao(), db.favoriteDao(), db.catalogDao(), catalogRepo)
                val publicationManager = ReadiumPublicationManager(context.applicationContext)

                return ReaderViewModel(
                    bookId = bookId,
                    downloadRepository = downloadRepo,
                    libraryRepository = libraryRepo,
                    catalogRepository = catalogRepo,
                    publicationManager = publicationManager
                ) as T
            }
        }
    }
}
