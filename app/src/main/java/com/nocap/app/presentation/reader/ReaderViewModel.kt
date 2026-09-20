package com.nocap.app.presentation.reader

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.entity.CustomFontEntity
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.core.database.entity.PerBookPreferencesEntity
import com.nocap.app.core.datastore.ReaderFontFamily
import com.nocap.app.core.datastore.ReaderOrientation
import com.nocap.app.core.datastore.ReaderPreferences
import com.nocap.app.core.datastore.ReaderPreferencesDataStore
import com.nocap.app.core.datastore.ReaderTextAlignment
import com.nocap.app.core.datastore.ReaderTheme
import com.nocap.app.data.annotation.LocalAnnotationRepository
import com.nocap.app.data.bookmark.LocalBookmarkRepository
import com.nocap.app.data.catalog.LocalCatalogRepository
import com.nocap.app.data.download.LocalBookDownloadRepository
import com.nocap.app.data.favorite.LocalFavoriteRepository
import com.nocap.app.data.font.LocalCustomFontRepository
import com.nocap.app.data.importer.FormatSniffer
import com.nocap.app.data.library.LocalLibraryRepository
import com.nocap.app.data.preferences.LocalPerBookPreferencesRepository
import com.nocap.app.data.reader.ReadiumPublicationManager
import com.nocap.app.domain.model.Bookmark
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.ReadingProgress
import com.nocap.app.domain.model.TocItem
import com.nocap.app.domain.repository.AnnotationRepository
import com.nocap.app.domain.repository.BookDownloadRepository
import com.nocap.app.domain.repository.BookmarkRepository
import com.nocap.app.domain.repository.CatalogRepository
import com.nocap.app.domain.repository.CustomFontRepository
import com.nocap.app.domain.repository.LibraryRepository
import com.nocap.app.domain.repository.PerBookPreferencesRepository
import com.nocap.app.data.review.LocalReviewRepository
import com.nocap.app.domain.repository.ReviewRepository
import com.nocap.app.domain.session.ReadingSessionManager
import kotlinx.coroutines.Job

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.isSearchable
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.Try
import java.io.File
import java.util.UUID
import kotlin.math.abs

internal fun isSameReadiumBookmarkPosition(savedLocatorJson: String, currentLocatorJson: String): Boolean {
    return runCatching {
        val saved = JSONObject(savedLocatorJson)
        val current = JSONObject(currentLocatorJson)
        if (saved.optString("href") != current.optString("href")) return@runCatching false

        val savedLocations = saved.optJSONObject("locations") ?: return@runCatching saved.toString() == current.toString()
        val currentLocations = current.optJSONObject("locations") ?: return@runCatching saved.toString() == current.toString()

        val savedFragments = savedLocations.optJSONArray("fragments")
        val currentFragments = currentLocations.optJSONArray("fragments")
        if (savedFragments != null && currentFragments != null && savedFragments.toString() == currentFragments.toString()) {
            return@runCatching true
        }

        val savedPosition = savedLocations.optInt("position", -1)
        val currentPosition = currentLocations.optInt("position", -1)
        if (savedPosition >= 0 && currentPosition >= 0 && savedPosition == currentPosition) {
            return@runCatching true
        }

        val savedProgression = savedLocations.optDouble("progression", Double.NaN)
        val currentProgression = currentLocations.optDouble("progression", Double.NaN)
        !savedProgression.isNaN() && !currentProgression.isNaN() &&
            abs(savedProgression - currentProgression) <= 0.0025
    }.getOrDefault(false)
}

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Ready(
        val publication: Publication,
        val initialLocator: Locator?,
        val toc: List<TocItem>,
        val bookTitle: String,
        val format: PublicationFormat = PublicationFormat.EPUB
    ) : ReaderUiState
    data class CustomFormatReady(
        val book: CatalogBook,
        val file: File,
        val format: PublicationFormat
    ) : ReaderUiState
    data object BookNotDownloaded : ReaderUiState
    data object FileNotFound : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

data class SearchResultItem(
    val locator: Locator,
    val chapterTitle: String?,
    val snippet: String,
    val progression: Float?
)

fun ReaderPreferences.toReadiumPreferences(): EpubPreferences {
    val readiumTheme = when (theme) {
        ReaderTheme.LIGHT -> Theme.LIGHT
        ReaderTheme.DARK -> Theme.DARK
        ReaderTheme.SEPIA -> Theme.SEPIA
    }

    val readiumFontFamily = when (fontFamily) {
        ReaderFontFamily.SERIF -> FontFamily.SERIF
        ReaderFontFamily.SANS_SERIF -> FontFamily.SANS_SERIF
        ReaderFontFamily.LORA -> FontFamily("Lora")
        ReaderFontFamily.ROBOTO -> FontFamily("Roboto")
        ReaderFontFamily.CUSTOM -> customFontName?.let { FontFamily(it) }
        else -> null
    }

    val readiumTextAlign = when (textAlignment) {
        ReaderTextAlignment.START -> TextAlign.START
        ReaderTextAlignment.JUSTIFY -> TextAlign.JUSTIFY
    }

    return EpubPreferences(
        fontSize = fontSizeMultiplier.toDouble(),
        fontFamily = readiumFontFamily,
        lineHeight = lineHeightMultiplier.toDouble(),
        textAlign = readiumTextAlign,
        theme = readiumTheme,
        scroll = isScrollMode,
        // Reader choices must win over publisher CSS; otherwise many EPUBs keep their
        // own white background, font and alignment and make these controls appear broken.
        publisherStyles = false
    )
}

class ReaderViewModel(
    private val bookId: String,
    private val downloadRepository: BookDownloadRepository,
    private val libraryRepository: LibraryRepository,
    private val catalogRepository: CatalogRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val preferencesDataStore: ReaderPreferencesDataStore,
    private val publicationManager: ReadiumPublicationManager,
    private val annotationRepository: AnnotationRepository,
    private val perBookPreferencesRepository: PerBookPreferencesRepository,
    private val customFontRepository: CustomFontRepository,
    private val reviewRepository: ReviewRepository? = null,
    private val readingSessionManager: ReadingSessionManager? = null,
    private val initialLocatorJson: String? = null
) : ViewModel() {

    private var activeSessionId: String? = null


    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val globalPreferences = preferencesDataStore.readerPreferences
    private val perBookPreferences = perBookPreferencesRepository.observePreferences(bookId)

    val isUsingBookOverride: StateFlow<Boolean> = perBookPreferences.combine(globalPreferences) { perBook, _ ->
        perBook?.useBookOverride == true
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val preferences: StateFlow<ReaderPreferences> = combine(
        globalPreferences,
        perBookPreferences
    ) { global, perBook ->
        if (perBook != null && perBook.useBookOverride) {
            val theme = perBook.theme?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: global.theme
            val fontFamily = perBook.fontFamily?.let { runCatching { ReaderFontFamily.valueOf(it) }.getOrNull() } ?: global.fontFamily
            val fontSize = perBook.fontSize ?: global.fontSizeMultiplier
            val lineHeight = perBook.lineHeight ?: global.lineHeightMultiplier
            val textAlignment = perBook.textAlignment?.let { runCatching { ReaderTextAlignment.valueOf(it) }.getOrNull() } ?: global.textAlignment
            val isScrollMode = perBook.scrollMode ?: global.isScrollMode

            global.copy(
                theme = theme,
                fontFamily = fontFamily,
                fontSizeMultiplier = fontSize,
                lineHeightMultiplier = lineHeight,
                textAlignment = textAlignment,
                isScrollMode = isScrollMode
            )
        } else {
            global
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderPreferences()
    )

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepository.observeBookmarks(bookId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val highlights: StateFlow<List<HighlightEntity>> = annotationRepository.observeHighlights(bookId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customFonts: StateFlow<List<CustomFontEntity>> = customFontRepository.observeFonts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentLocator = MutableStateFlow<Locator?>(null)
    val currentLocator: StateFlow<Locator?> = _currentLocator.asStateFlow()

    val isCurrentBookmarked: StateFlow<Boolean> = combine(_currentLocator, bookmarks) { locator, list ->
        if (locator == null) false
        else {
            val currentJson = publicationManager.serializeLocator(locator)
            list.any { isSameReadiumBookmarkPosition(it.locatorJson, currentJson) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // In-book Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearchSupported = MutableStateFlow(false)
    val isSearchSupported: StateFlow<Boolean> = _isSearchSupported.asStateFlow()

    private var searchJob: Job? = null
    private var currentPublication: Publication? = null
    private var lastSavedTime = 0L
    private var loadJob: Job? = null
    private val progressWriteLock = kotlinx.coroutines.sync.Mutex()
    private var lastPersistedTime = Long.MIN_VALUE

    private suspend fun persistProgress(progress: ReadingProgress) {
        progressWriteLock.lock()
        try {
            if (progress.lastReadAt >= lastPersistedTime) {
                libraryRepository.saveReadingProgress(progress)
                lastPersistedTime = progress.lastReadAt
            }
        } finally { progressWriteLock.unlock() }
    }

    init {
        loadPublication()
    }

    fun loadPublication() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
            _uiState.value = ReaderUiState.Loading

            var downloaded = downloadRepository.observeDownload(bookId).first()
            if (downloaded == null || downloaded.downloadStatus != DownloadStatus.COMPLETED) {
                try {
                    downloadRepository.startDownload(bookId)
                    downloaded = kotlinx.coroutines.withTimeout(180_000) {
                        downloadRepository.observeDownload(bookId).first { it?.downloadStatus == DownloadStatus.COMPLETED || it?.downloadStatus == DownloadStatus.FAILED }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                    _uiState.value = ReaderUiState.Error(e.message ?: "Chưa tải được tài liệu")
                    return@launch
                }
            }
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
            val title = catalogBook?.title ?: file.nameWithoutExtension.ifBlank { "Sách điện tử" }
            val format = catalogBook?.format ?: FormatSniffer.sniff(file) ?: PublicationFormat.EPUB

            if (format != PublicationFormat.EPUB && format != PublicationFormat.PDF) {
                if (catalogBook != null) {
                    _uiState.value = ReaderUiState.CustomFormatReady(
                        book = catalogBook,
                        file = file,
                        format = format
                    )
                    return@launch
                }
            }

            val savedProgress = libraryRepository.observeBookProgress(bookId).first()
            val locatorJsonToUse = initialLocatorJson ?: savedProgress?.locatorJson

            val openResult = publicationManager.openPublication(file)
            openResult.onSuccess { publication ->
                currentPublication?.let { publicationManager.closePublication(it) }
                currentPublication = publication
                val initialLocator = publicationManager.deserializeLocator(locatorJsonToUse, publication)
                val toc = publicationManager.extractTableOfContents(publication)
                val format = catalogBook?.format ?: FormatSniffer.sniff(file) ?: PublicationFormat.EPUB
                _isSearchSupported.value = publication.isSearchable
                libraryRepository.updateLastOpenedAt(bookId, System.currentTimeMillis())

                // Start reading session
                val startProgress = (initialLocator?.locations?.progression ?: savedProgress?.progression?.toDouble() ?: 0.0).toFloat()
                viewModelScope.launch {
                    activeSessionId = readingSessionManager?.startSession(
                        bookId = bookId,
                        format = format.name,
                        startProgress = startProgress
                    )
                }

                _uiState.value = ReaderUiState.Ready(
                    publication = publication,
                    initialLocator = initialLocator,
                    toc = toc,
                    bookTitle = title,
                    format = format
                )
            }.onFailure { error ->

                _uiState.value = ReaderUiState.Error(error.message ?: "Không thể mở sách")
            }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = ReaderUiState.Error("Chưa mở được tài liệu. Hãy quay lại thư viện và thử lại; dữ liệu trên máy vẫn được giữ.")
            }
        }
    }

    fun onLocationChanged(locator: Locator) {
        _currentLocator.value = locator
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
            persistProgress(progress)

            val currentBook = catalogRepository.getBookById(bookId)
            if (currentBook != null) {
                if (currentBook.readingStatus == com.nocap.app.domain.model.DocumentReadingStatus.UNREAD && progression > 0f) {
                    libraryRepository.setReadingStatus(bookId, com.nocap.app.domain.model.DocumentReadingStatus.READING)
                } else if (currentBook.readingStatus == com.nocap.app.domain.model.DocumentReadingStatus.READING && progression >= 0.98f) {
                    libraryRepository.setReadingStatus(bookId, com.nocap.app.domain.model.DocumentReadingStatus.COMPLETED)
                }
            }
        }
    }

    fun endActiveSession(progression: Float? = null) {
        val sId = activeSessionId ?: return
        activeSessionId = null
        val finalProg = progression ?: (_currentLocator.value?.locations?.progression ?: 0.0).toFloat()
        readingSessionManager?.endSessionAsync(sId, finalProg)
    }

    fun saveCurrentLocationImmediately(locator: Locator?) {
        val target = locator ?: _currentLocator.value
        val progression = (target?.locations?.progression ?: 0.0).toFloat().coerceIn(0f, 1f)
        endActiveSession(progression)
        if (target == null) return
        val now = System.currentTimeMillis()
        ReadingSessionManager.processScope.launch {
            val chapterTitle = target.title
            val locatorJson = publicationManager.serializeLocator(target)

            val progress = ReadingProgress(
                bookId = bookId,
                locatorJson = locatorJson,
                progression = progression,
                chapterTitle = chapterTitle,
                lastReadAt = now
            )
            persistProgress(progress)
        }
    }

    fun toggleBookmark() {
        val locator = _currentLocator.value ?: return
        viewModelScope.launch {
            val currentJson = publicationManager.serializeLocator(locator)
            val existing = bookmarks.value.filter { isSameReadiumBookmarkPosition(it.locatorJson, currentJson) }
            if (existing.isNotEmpty()) {
                // Older builds could create several bookmarks for one position because
                // the icon never recognized its own locator. One toggle removes that
                // duplicate group and restores the expected off state.
                existing.forEach { bookmarkRepository.removeBookmark(it.id) }
            } else {
                val bookmark = Bookmark(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    locatorJson = currentJson,
                    chapterTitle = locator.title ?: "Dấu trang",
                    snippet = locator.text.highlight ?: locator.text.after ?: locator.title,
                    createdAt = System.currentTimeMillis()
                )
                bookmarkRepository.addBookmark(bookmark)
            }
        }
    }

    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(bookmarkId)
        }
    }

    // Search inside EPUB
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.trim().isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        val pub = currentPublication
        if (pub == null || !pub.isSearchable) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce input
            _isSearching.value = true
            _searchResults.value = emptyList()

            try {
                val iterator = pub.search(query.trim())
                if (iterator == null) {
                    _isSearching.value = false
                    return@launch
                }

                val collected = mutableListOf<SearchResultItem>()
                try {
                while (collected.size < 60) {
                    val res = iterator.next()
                    if (res is Try.Success) {
                        val collection = res.value
                        if (collection == null || collection.locators.isEmpty()) break
                        for (loc in collection.locators) {
                            val snippet = loc.text.highlight
                                ?: listOfNotNull(loc.text.before, loc.text.after).joinToString(" ").ifBlank { loc.title.orEmpty() }
                            collected.add(
                                SearchResultItem(
                                    locator = loc,
                                    chapterTitle = loc.title,
                                    snippet = snippet.trim(),
                                    progression = loc.locations.progression?.toFloat()
                                )
                            )
                        }
                        _searchResults.value = collected.toList()
                    } else {
                        break
                    }
                }
                } finally { iterator.close() }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    // Highlights & Notes
    fun addHighlight(
        locator: Locator,
        colorHex: String = "YELLOW",
        note: String? = null,
        addToReview: Boolean = false,
        onComplete: (HighlightEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val text = locator.text.highlight ?: locator.text.before ?: locator.title ?: ""
            val locatorJson = publicationManager.serializeLocator(locator)
            val result = annotationRepository.addHighlight(
                bookId = bookId,
                locatorJson = locatorJson,
                text = text.trim(),
                color = colorHex,
                note = note?.trim()?.ifBlank { null }
            )
            result.onSuccess { hl ->
                if (addToReview || !note.isNullOrBlank()) {
                    reviewRepository?.addToReview(hl.id, bookId)
                }
                onComplete(hl)
            }
        }
    }

    fun addHighlight(
        text: String,
        colorHex: String = "YELLOW",
        locatorJson: String,
        note: String? = null,
        addToReview: Boolean = false,
        onComplete: (HighlightEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = annotationRepository.addHighlight(
                bookId = bookId,
                locatorJson = locatorJson,
                text = text.trim(),
                color = colorHex,
                note = note?.trim()?.ifBlank { null }
            )
            result.onSuccess { hl ->
                if (addToReview || !note.isNullOrBlank()) {
                    reviewRepository?.addToReview(hl.id, bookId)
                }
                onComplete(hl)
            }
        }
    }

    fun toggleReview(annotationId: String) {
        viewModelScope.launch {
            val existing = reviewRepository?.getReviewItem(annotationId)
            if (existing != null) {
                reviewRepository.removeFromReview(annotationId)
            } else {
                reviewRepository?.addToReview(annotationId, bookId)
            }
        }
    }

    suspend fun extractPdfPageText(pageIndex: Int): com.nocap.app.data.parser.PdfTextExtractor.PageTextResult? {
        val downloaded = downloadRepository.observeDownload(bookId).first() ?: return null
        val file = File(downloaded.localFilePath)
        if (!file.exists()) return null
        return com.nocap.app.data.parser.PdfTextExtractor.extractPageText(file, pageIndex)
    }



    fun updateHighlightColor(id: String, colorHex: String) {
        viewModelScope.launch {
            annotationRepository.updateHighlightColor(id, colorHex)
        }
    }

    fun updateHighlightNote(id: String, note: String?) {
        viewModelScope.launch {
            annotationRepository.updateNote(id, note?.trim()?.ifBlank { null })
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch {
            annotationRepository.deleteHighlight(id)
        }
    }

    // Per-Book Reader Settings
    fun toggleUseBookOverride(enabled: Boolean) {
        viewModelScope.launch {
            val currentPref = preferences.value
            if (enabled) {
                val override = PerBookPreferencesEntity(
                    bookId = bookId,
                    theme = currentPref.theme.name,
                    fontFamily = currentPref.fontFamily.name,
                    fontSize = currentPref.fontSizeMultiplier,
                    lineHeight = currentPref.lineHeightMultiplier,
                    textAlignment = currentPref.textAlignment.name,
                    scrollMode = currentPref.isScrollMode,
                    useBookOverride = true
                )
                perBookPreferencesRepository.savePreferences(override)
            } else {
                val existing = perBookPreferences.first()
                if (existing != null) {
                    perBookPreferencesRepository.savePreferences(existing.copy(useBookOverride = false))
                }
            }
        }
    }

    fun resetBookOverride() {
        viewModelScope.launch {
            perBookPreferencesRepository.resetToDefaults(bookId)
        }
    }

    fun updateTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            if (isUsingBookOverride.value) {
                val existing = perBookPreferences.first() ?: PerBookPreferencesEntity(bookId = bookId)
                perBookPreferencesRepository.savePreferences(existing.copy(theme = theme.name, useBookOverride = true))
            } else {
                preferencesDataStore.updateTheme(theme)
            }
        }
    }

    fun updateFontFamily(fontFamily: ReaderFontFamily, customFontName: String? = null) {
        viewModelScope.launch {
            if (isUsingBookOverride.value) {
                val existing = perBookPreferences.first() ?: PerBookPreferencesEntity(bookId = bookId)
                perBookPreferencesRepository.savePreferences(
                    existing.copy(
                        fontFamily = fontFamily.name,
                        useBookOverride = true
                    )
                )
            } else {
                preferencesDataStore.updateFontFamily(fontFamily, customFontName)
            }
        }
    }

    fun updateFontSize(multiplier: Float) {
        viewModelScope.launch {
            if (isUsingBookOverride.value) {
                val existing = perBookPreferences.first() ?: PerBookPreferencesEntity(bookId = bookId)
                perBookPreferencesRepository.savePreferences(
                    existing.copy(fontSize = multiplier.coerceIn(0.8f, 2.0f), useBookOverride = true)
                )
            } else {
                preferencesDataStore.updateFontSize(multiplier)
            }
        }
    }

    fun updateLineHeight(multiplier: Float) {
        viewModelScope.launch {
            if (isUsingBookOverride.value) {
                val existing = perBookPreferences.first() ?: PerBookPreferencesEntity(bookId = bookId)
                perBookPreferencesRepository.savePreferences(
                    existing.copy(lineHeight = multiplier.coerceIn(1.2f, 2.0f), useBookOverride = true)
                )
            } else {
                preferencesDataStore.updateLineHeight(multiplier)
            }
        }
    }

    fun updateTextAlignment(alignment: ReaderTextAlignment) {
        viewModelScope.launch {
            if (isUsingBookOverride.value) {
                val existing = perBookPreferences.first() ?: PerBookPreferencesEntity(bookId = bookId)
                perBookPreferencesRepository.savePreferences(
                    existing.copy(textAlignment = alignment.name, useBookOverride = true)
                )
            } else {
                preferencesDataStore.updateTextAlignment(alignment)
            }
        }
    }

    fun updateScrollMode(isScrollMode: Boolean) {
        viewModelScope.launch {
            if (isUsingBookOverride.value) {
                val existing = perBookPreferences.first() ?: PerBookPreferencesEntity(bookId = bookId)
                perBookPreferencesRepository.savePreferences(
                    existing.copy(scrollMode = isScrollMode, useBookOverride = true)
                )
            } else {
                preferencesDataStore.updateScrollMode(isScrollMode)
            }
        }
    }

    // Global Reader Controls
    fun updateVolumeButtonsTurnPages(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateVolumeButtonsTurnPages(enabled) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateKeepScreenOn(enabled) }
    }

    fun updateFullscreen(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateFullscreen(enabled) }
    }

    fun updateOrientation(orientation: ReaderOrientation) {
        viewModelScope.launch { preferencesDataStore.updateOrientation(orientation) }
    }

    fun updateBrightness(followSystem: Boolean, customBrightness: Float = 0.5f) {
        viewModelScope.launch { preferencesDataStore.updateBrightness(followSystem, customBrightness) }
    }

    fun updateChromeOptions(showProgress: Boolean, showPercentage: Boolean, showClock: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateChromeOptions(showProgress, showPercentage, showClock) }
    }

    // Custom Fonts
    fun importCustomFont(uri: Uri, onResult: (Result<CustomFontEntity>) -> Unit = {}) {
        viewModelScope.launch {
            val result = customFontRepository.importFont(uri)
            onResult(result)
        }
    }

    fun deleteCustomFont(fontId: String) {
        viewModelScope.launch {
            customFontRepository.deleteFont(fontId)
        }
    }

    override fun onCleared() {
        saveCurrentLocationImmediately(_currentLocator.value)
        super.onCleared()
        searchJob?.cancel()
        currentPublication?.let { publicationManager.closePublication(it) }
        currentPublication = null
        endActiveSession()
    }

    companion object {
        fun provideFactory(
            bookId: String,
            context: Context,
            initialLocatorJson: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val catalogRepo = LocalCatalogRepository(db.progressDao(), db.catalogDao())
                val downloadRepo = LocalBookDownloadRepository(context, db.downloadDao(), db.catalogDao(), catalogRepo)
                val libraryRepo = LocalLibraryRepository(db.downloadDao(), db.progressDao(), db.favoriteDao(), db.catalogDao(), catalogRepo)
                val bookmarkRepo = LocalBookmarkRepository(db.bookmarkDao(), db.catalogDao(), catalogRepo)
                val prefStore = ReaderPreferencesDataStore(context.applicationContext)
                val publicationManager = ReadiumPublicationManager(context.applicationContext)
                val annotationRepo = LocalAnnotationRepository(db.highlightDao())
                val perBookRepo = LocalPerBookPreferencesRepository(db.perBookPreferencesDao())
                val customFontRepo = LocalCustomFontRepository(context.applicationContext, db.customFontDao())
                val reviewRepo = LocalReviewRepository(db.reviewDao())
                val sessionManager = ReadingSessionManager.getInstance(context.applicationContext)

                return ReaderViewModel(
                    bookId = bookId,
                    downloadRepository = downloadRepo,
                    libraryRepository = libraryRepo,
                    catalogRepository = catalogRepo,
                    bookmarkRepository = bookmarkRepo,
                    preferencesDataStore = prefStore,
                    publicationManager = publicationManager,
                    annotationRepository = annotationRepo,
                    perBookPreferencesRepository = perBookRepo,
                    customFontRepository = customFontRepo,
                    reviewRepository = reviewRepo,
                    readingSessionManager = sessionManager,
                    initialLocatorJson = initialLocatorJson
                ) as T
            }
        }
    }
}

