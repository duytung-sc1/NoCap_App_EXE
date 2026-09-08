package com.nocap.app.presentation.library

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.dao.CollectionWithBookCount
import com.nocap.app.core.database.dao.TagWithBookCount
import com.nocap.app.core.database.entity.BookCollectionCrossRef
import com.nocap.app.core.database.entity.CollectionEntity
import com.nocap.app.core.datastore.LibraryPreferencesDataStore
import com.nocap.app.core.datastore.LibrarySmartView
import com.nocap.app.core.datastore.LibrarySort
import com.nocap.app.data.catalog.LocalCatalogRepository
import com.nocap.app.data.collection.LocalCollectionRepository
import com.nocap.app.data.download.LocalBookDownloadRepository
import com.nocap.app.data.favorite.LocalFavoriteRepository
import com.nocap.app.data.importer.LocalImportBookRepository
import com.nocap.app.data.library.LocalLibraryRepository
import com.nocap.app.data.tag.LocalTagRepository
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.DownloadProgress
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSource
import com.nocap.app.domain.model.PublicationSourceType
import com.nocap.app.domain.model.Tag
import com.nocap.app.domain.repository.BookDownloadRepository
import com.nocap.app.domain.repository.CollectionRepository
import com.nocap.app.domain.repository.FavoriteRepository
import com.nocap.app.domain.repository.ImportBookRepository
import com.nocap.app.domain.repository.ImportException
import com.nocap.app.domain.repository.LibraryRepository
import com.nocap.app.domain.repository.TagRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
import java.io.File
import java.io.FileOutputStream

enum class LibraryTab(val displayName: String) {
    ALL("Tất cả"),
    INBOX("Chưa phân loại"),
    COLLECTIONS("Bộ sưu tập"),
    TAGS("Thẻ")
}

sealed interface LibraryEvent {
    data class ImportSuccess(val bookTitle: String) : LibraryEvent
    data class ShowMessage(val message: String) : LibraryEvent
    data class DuplicateFound(val bookId: String, val title: String) : LibraryEvent
}

data class ImportState(
    val isImporting: Boolean = false,
    val progress: DownloadProgress? = null
)

data class MyLibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val selectedSmartView: LibrarySmartView = LibrarySmartView.ALL,
    val selectedSort: LibrarySort = LibrarySort.RECENTLY_OPENED,
    val searchQuery: String = "",
    val books: List<LibraryBook> = emptyList(),
    val inboxCount: Int = 0,
    val collections: List<CollectionWithBookCount> = emptyList(),
    val tags: List<TagWithBookCount> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedBookIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val importProgress: DownloadProgress? = null
)

class MyLibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: BookDownloadRepository,
    private val favoriteRepository: FavoriteRepository,
    private val importRepository: ImportBookRepository,
    private val collectionRepository: CollectionRepository,
    private val tagRepository: TagRepository,
    private val preferencesDataStore: LibraryPreferencesDataStore? = null
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.ALL)
    private val _selectedSmartView = MutableStateFlow(LibrarySmartView.ALL)
    private val _selectedSort = MutableStateFlow(LibrarySort.RECENTLY_OPENED)
    private val _searchQuery = MutableStateFlow("")
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedBookIds = MutableStateFlow<Set<String>>(emptySet())
    private val _importState = MutableStateFlow(ImportState())
    private var importJob: Job? = null

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    init {
        preferencesDataStore?.let { dataStore ->
            viewModelScope.launch {
                dataStore.libraryPreferences.collect { prefs ->
                    _selectedSort.value = prefs.sort
                    _selectedSmartView.value = prefs.smartView
                }
            }
        }
    }

    val collections: StateFlow<List<CollectionWithBookCount>> = collectionRepository.observeCollections()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val tags: StateFlow<List<TagWithBookCount>> = tagRepository.observeTagsWithCount()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private val allLibraryBooks: Flow<List<LibraryBook>> = libraryRepository.observeLibraryBooks()

    val inboxCount: Flow<Int> = allLibraryBooks.combine(MutableStateFlow(Unit)) { books, _ ->
        books.count { it.book.isInInbox && !it.book.isArchived }
    }

    private val filteredBooks: Flow<List<LibraryBook>> = combine(
        allLibraryBooks,
        _selectedTab,
        _selectedSmartView,
        _selectedSort,
        _searchQuery
    ) { libraryBooks, tab, smartView, sort, query ->
        // 1. Tab filtering
        val tabFiltered = when (tab) {
            LibraryTab.INBOX -> libraryBooks.filter { it.book.isInInbox && !it.book.isArchived }
            LibraryTab.ALL -> {
                if (smartView == LibrarySmartView.ARCHIVED) {
                    libraryBooks.filter { it.book.isArchived }
                } else {
                    libraryBooks.filter { !it.book.isArchived }
                }
            }
            LibraryTab.COLLECTIONS, LibraryTab.TAGS -> libraryBooks
        }

        // 2. Smart View filtering (applied in ALL tab)
        val smartViewFiltered = if (tab == LibraryTab.ALL) {
            when (smartView) {
                LibrarySmartView.ALL -> tabFiltered
                LibrarySmartView.PINNED -> tabFiltered.filter { it.book.isPinned }
                LibrarySmartView.FAVORITES -> tabFiltered.filter { it.isFavorite }
                LibrarySmartView.RECENTLY_OPENED -> tabFiltered.filter { it.book.lastOpenedAt != null }
                LibrarySmartView.RECENTLY_ADDED -> tabFiltered.filter { it.book.addedAt > 0L || it.downloadedBook.downloadedAt != null }
                LibrarySmartView.UNREAD -> tabFiltered.filter { it.book.readingStatus == DocumentReadingStatus.UNREAD }
                LibrarySmartView.READING -> tabFiltered.filter { it.book.readingStatus == DocumentReadingStatus.READING }
                LibrarySmartView.COMPLETED -> tabFiltered.filter { it.book.readingStatus == DocumentReadingStatus.COMPLETED }
                LibrarySmartView.ARCHIVED -> tabFiltered // already filtered above
                LibrarySmartView.EPUB -> tabFiltered.filter { it.book.format == PublicationFormat.EPUB }
                LibrarySmartView.PDF -> tabFiltered.filter { it.book.format == PublicationFormat.PDF }
                LibrarySmartView.TXT -> tabFiltered.filter { it.book.format == PublicationFormat.TXT }
                LibrarySmartView.MARKDOWN -> tabFiltered.filter { it.book.format == PublicationFormat.MARKDOWN }
                LibrarySmartView.HTML -> tabFiltered.filter { it.book.format == PublicationFormat.HTML }
                LibrarySmartView.DOCX -> tabFiltered.filter { it.book.format == PublicationFormat.DOCX }
                LibrarySmartView.IMAGE -> tabFiltered.filter { it.book.format.isSingleImage }
                LibrarySmartView.CBZ -> tabFiltered.filter { it.book.format == PublicationFormat.CBZ }
                LibrarySmartView.FROM_WEB -> tabFiltered.filter {
                    it.book.sourceType == PublicationSourceType.REMOTE_URL || it.book.sourceType == PublicationSourceType.SHARED_URL
                }
            }
        } else {
            tabFiltered
        }

        // 3. Search query filtering
        val searchFiltered = if (query.isBlank()) {
            smartViewFiltered
        } else {
            val q = query.trim().lowercase()
            smartViewFiltered.filter { book ->
                book.book.displayTitle.lowercase().contains(q) ||
                book.book.displayAuthor.lowercase().contains(q) ||
                (book.book.originalFilename?.lowercase()?.contains(q) == true) ||
                book.tags.any { tag -> tag.name.lowercase().contains(q) } ||
                book.collections.any { col -> col.lowercase().contains(q) }
            }
        }

        // 4. Sorting
        val sorted = when (sort) {
            LibrarySort.RECENTLY_OPENED -> searchFiltered.sortedByDescending { it.book.lastOpenedAt ?: it.readingProgress?.lastReadAt ?: 0L }
            LibrarySort.RECENTLY_ADDED -> searchFiltered.sortedByDescending { it.book.addedAt.takeIf { t -> t > 0L } ?: it.downloadedBook.downloadedAt ?: 0L }
            LibrarySort.TITLE_ASC -> searchFiltered.sortedBy { it.book.displayTitle.lowercase() }
            LibrarySort.TITLE_DESC -> searchFiltered.sortedByDescending { it.book.displayTitle.lowercase() }
            LibrarySort.AUTHOR -> searchFiltered.sortedBy { it.book.displayAuthor.lowercase() }
            LibrarySort.PROGRESS -> searchFiltered.sortedByDescending { it.readingProgress?.progression ?: 0f }
        }

        // In ALL tab (when not filtering by Pinned), surfaced pinned items first
        if (tab == LibraryTab.ALL && smartView != LibrarySmartView.PINNED && smartView != LibrarySmartView.ARCHIVED) {
            sorted.sortedByDescending { it.book.isPinned }
        } else {
            sorted
        }
    }

    val uiState: StateFlow<MyLibraryUiState> = combine(
        filteredBooks,
        collections,
        tags,
        _selectedTab,
        _selectedSmartView,
        _selectedSort,
        _searchQuery
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val books = args[0] as List<LibraryBook>
        @Suppress("UNCHECKED_CAST")
        val collectionList = args[1] as List<CollectionWithBookCount>
        @Suppress("UNCHECKED_CAST")
        val tagList = args[2] as List<TagWithBookCount>
        val tab = args[3] as LibraryTab
        val smartView = args[4] as LibrarySmartView
        val sort = args[5] as LibrarySort
        val query = args[6] as String

        Triple(books, collectionList, tagList) to Triple(tab, smartView, sort to query)
    }.combine(
        combine(
            _isSelectionMode,
            _selectedBookIds,
            _importState,
            inboxCount
        ) { isSelection, selectedIds, importState, inboxes ->
            Triple(isSelection, selectedIds, importState to inboxes)
        }
    ) { (mainData, filterData), (isSelection, selectedIds, importAndInbox) ->
        val (books, collectionList, tagList) = mainData
        val (tab, smartView, sortAndQuery) = filterData
        val (sort, query) = sortAndQuery
        val (importState, inboxes) = importAndInbox

        MyLibraryUiState(
            selectedTab = tab,
            selectedSmartView = smartView,
            selectedSort = sort,
            searchQuery = query,
            books = books,
            inboxCount = inboxes,
            collections = collectionList,
            tags = tagList,
            isSelectionMode = isSelection,
            selectedBookIds = selectedIds,
            isLoading = false,
            isImporting = importState.isImporting,
            importProgress = importState.progress
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MyLibraryUiState(isLoading = true)
    )

    fun onTabChange(tab: LibraryTab) {
        _selectedTab.update { tab }
    }

    fun onSmartViewChange(smartView: LibrarySmartView) {
        _selectedSmartView.update { smartView }
        viewModelScope.launch {
            preferencesDataStore?.updateSmartView(smartView)
        }
    }

    fun onSortChange(sort: LibrarySort) {
        _selectedSort.update { sort }
        viewModelScope.launch {
            preferencesDataStore?.updateSort(sort)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    // Selection Mode & Bulk Actions
    fun enterSelectionMode(initialBookId: String? = null) {
        _isSelectionMode.value = true
        _selectedBookIds.value = if (initialBookId != null) setOf(initialBookId) else emptySet()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedBookIds.value = emptySet()
    }

    fun toggleBookSelection(bookId: String) {
        _selectedBookIds.update { current ->
            if (current.contains(bookId)) {
                val next = current - bookId
                if (next.isEmpty()) {
                    _isSelectionMode.value = false
                }
                next
            } else {
                current + bookId
            }
        }
    }

    fun selectAll() {
        val currentBooks = uiState.value.books
        _selectedBookIds.value = currentBooks.map { it.book.id }.toSet()
    }

    fun bulkAddTag(tagId: String) {
        val ids = _selectedBookIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = tagRepository.bulkAddTag(ids, tagId)
            if (result.isSuccess) {
                _events.emit(LibraryEvent.ShowMessage("Đã gắn thẻ cho ${ids.size} tài liệu"))
                exitSelectionMode()
            } else {
                _events.emit(LibraryEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Lỗi gắn thẻ"))
            }
        }
    }

    fun bulkAddCollection(collectionId: String) {
        val ids = _selectedBookIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { bookId ->
                collectionRepository.addBookToCollection(bookId, collectionId)
            }
            _events.emit(LibraryEvent.ShowMessage("Đã thêm ${ids.size} tài liệu vào bộ sưu tập"))
            exitSelectionMode()
        }
    }

    fun bulkSetArchived(isArchived: Boolean) {
        val ids = _selectedBookIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            libraryRepository.bulkArchive(ids, isArchived)
            val msg = if (isArchived) "Đã lưu trữ ${ids.size} tài liệu" else "Đã bỏ lưu trữ ${ids.size} tài liệu"
            _events.emit(LibraryEvent.ShowMessage(msg))
            exitSelectionMode()
        }
    }

    fun bulkSetPinned(isPinned: Boolean) {
        val ids = _selectedBookIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            libraryRepository.bulkPin(ids, isPinned)
            val msg = if (isPinned) "Đã ghim ${ids.size} tài liệu" else "Đã bỏ ghim ${ids.size} tài liệu"
            _events.emit(LibraryEvent.ShowMessage(msg))
            exitSelectionMode()
        }
    }

    fun bulkSetReadingStatus(status: DocumentReadingStatus) {
        val ids = _selectedBookIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            libraryRepository.bulkSetReadingStatus(ids, status)
            _events.emit(LibraryEvent.ShowMessage("Đã cập nhật trạng thái: ${status.displayName}"))
            exitSelectionMode()
        }
    }

    // Individual Document Actions
    fun onSetInboxState(bookId: String, inInbox: Boolean) {
        viewModelScope.launch {
            libraryRepository.setInboxState(bookId, inInbox)
            val msg = if (inInbox) "Đã chuyển vào Chưa phân loại" else "Đã hoàn thành tổ chức"
            _events.emit(LibraryEvent.ShowMessage(msg))
        }
    }

    fun onTogglePin(bookId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            val next = !currentPinned
            libraryRepository.setPinnedState(bookId, next)
            val msg = if (next) "Đã ghim tài liệu" else "Đã bỏ ghim tài liệu"
            _events.emit(LibraryEvent.ShowMessage(msg))
        }
    }

    fun onToggleArchive(bookId: String, currentArchived: Boolean) {
        viewModelScope.launch {
            val next = !currentArchived
            libraryRepository.setArchivedState(bookId, next)
            val msg = if (next) "Đã lưu trữ tài liệu" else "Đã khôi phục tài liệu"
            _events.emit(LibraryEvent.ShowMessage(msg))
        }
    }

    fun onSetReadingStatus(bookId: String, status: DocumentReadingStatus) {
        viewModelScope.launch {
            libraryRepository.setReadingStatus(bookId, status)
            _events.emit(LibraryEvent.ShowMessage("Đã đánh dấu: ${status.displayName}"))
        }
    }

    fun onUpdateMetadata(bookId: String, titleOverride: String?, authorOverride: String?) {
        viewModelScope.launch {
            val cleanTitle = titleOverride?.trim()?.takeIf { it.isNotBlank() }
            val cleanAuthor = authorOverride?.trim()?.takeIf { it.isNotBlank() }
            libraryRepository.setMetadataOverrides(bookId, cleanTitle, cleanAuthor)
            _events.emit(LibraryEvent.ShowMessage("Đã cập nhật thông tin tài liệu"))
        }
    }

    fun onSetCustomCover(bookId: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val cr = context.contentResolver
                // 1. Verify size <= 10MB
                val pfd = cr.openFileDescriptor(uri, "r")
                val fileSize = pfd?.statSize ?: 0L
                pfd?.close()
                if (fileSize > 10 * 1024 * 1024L) {
                    _events.emit(LibraryEvent.ShowMessage("Ảnh bìa không được vượt quá 10MB"))
                    return@launch
                }

                // 2. Validate image decodability
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    _events.emit(LibraryEvent.ShowMessage("Tệp ảnh không hợp lệ"))
                    return@launch
                }

                // 3. Copy to private covers/ directory
                val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
                val coverFile = File(coversDir, "${bookId}_cover.jpg")
                cr.openInputStream(uri)?.use { input ->
                    FileOutputStream(coverFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 4. Update repository
                libraryRepository.setCustomCover(bookId, coverFile.absolutePath)
                _events.emit(LibraryEvent.ShowMessage("Đã cập nhật ảnh bìa tùy chỉnh"))
            } catch (e: Exception) {
                _events.emit(LibraryEvent.ShowMessage("Lỗi lưu ảnh bìa: ${e.message}"))
            }
        }
    }

    fun onRemoveCustomCover(bookId: String, context: Context) {
        viewModelScope.launch {
            val coversDir = File(context.filesDir, "covers")
            val coverFile = File(coversDir, "${bookId}_cover.jpg")
            if (coverFile.exists()) coverFile.delete()
            libraryRepository.setCustomCover(bookId, null)
            _events.emit(LibraryEvent.ShowMessage("Đã xóa ảnh bìa tùy chỉnh"))
        }
    }

    // Tag Management
    fun createTag(name: String, onComplete: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            val result = tagRepository.createTag(name)
            if (result.isSuccess) {
                _events.emit(LibraryEvent.ShowMessage("Đã tạo thẻ: $name"))
            } else {
                _events.emit(LibraryEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Lỗi tạo thẻ"))
            }
            onComplete(result)
        }
    }

    fun renameTag(tagId: String, newName: String, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = tagRepository.renameTag(tagId, newName)
            if (result.isSuccess) {
                _events.emit(LibraryEvent.ShowMessage("Đã đổi tên thẻ"))
            } else {
                _events.emit(LibraryEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Lỗi đổi tên thẻ"))
            }
            onComplete(result)
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            tagRepository.deleteTag(tagId)
            _events.emit(LibraryEvent.ShowMessage("Đã xóa thẻ"))
        }
    }

    fun getTagsForBook(bookId: String): Flow<List<Tag>> = tagRepository.observeTagsForBook(bookId)

    fun updateBookTags(bookId: String, selectedTagIds: List<String>) {
        viewModelScope.launch {
            tagRepository.setTagsForBook(bookId, selectedTagIds)
            _events.emit(LibraryEvent.ShowMessage("Đã cập nhật thẻ cho tài liệu"))
        }
    }

    fun observeBooksForTag(tagId: String): Flow<List<LibraryBook>> {
        return allLibraryBooks.combine(MutableStateFlow(tagId)) { books, id ->
            books.filter { book -> book.tags.any { it.id == id } }
        }
    }

    // Collection Management
    fun createCollection(name: String, onComplete: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            val result = collectionRepository.createCollection(name)
            if (result.isSuccess) {
                _events.emit(LibraryEvent.ShowMessage("Đã tạo bộ sưu tập: $name"))
            } else {
                _events.emit(LibraryEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Lỗi tạo bộ sưu tập"))
            }
            onComplete(result)
        }
    }

    fun renameCollection(id: String, newName: String, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = collectionRepository.renameCollection(id, newName)
            if (result.isSuccess) {
                _events.emit(LibraryEvent.ShowMessage("Đã đổi tên thành: $newName"))
            } else {
                _events.emit(LibraryEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Lỗi đổi tên"))
            }
            onComplete(result)
        }
    }

    fun deleteCollection(id: String) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(id)
            _events.emit(LibraryEvent.ShowMessage("Đã xóa bộ sưu tập"))
        }
    }

    fun observeBooksInCollection(collectionId: String): Flow<List<LibraryBook>> {
        return combine(
            collectionRepository.observeBookIdsInCollection(collectionId),
            allLibraryBooks
        ) { bookIds, libraryBooks ->
            val idSet = bookIds.toSet()
            libraryBooks.filter { idSet.contains(it.book.id) }
        }
    }

    fun getCollectionsForBook(bookId: String): Flow<List<CollectionEntity>> {
        return collectionRepository.observeCollectionsForBook(bookId)
    }

    fun updateBookCollections(bookId: String, selectedCollectionIds: Set<String>) {
        viewModelScope.launch {
            collectionRepository.updateBookCollections(bookId, selectedCollectionIds)
            _events.emit(LibraryEvent.ShowMessage("Đã cập nhật bộ sưu tập"))
        }
    }

    fun removeBookFromCollection(bookId: String, collectionId: String) {
        viewModelScope.launch {
            collectionRepository.removeBookFromCollection(bookId, collectionId)
            _events.emit(LibraryEvent.ShowMessage("Đã xóa khỏi bộ sưu tập"))
        }
    }

    // Import / Delete / Favorite / Updates
    fun onRemoveDownload(bookId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadedBook(bookId)
        }
    }

    fun onDeleteImportedBook(bookId: String) {
        viewModelScope.launch {
            val result = importRepository.deleteImportedBook(bookId)
            if (result.isSuccess) {
                _events.emit(LibraryEvent.ShowMessage("Đã xóa tài liệu khỏi thư viện"))
            } else {
                _events.emit(LibraryEvent.ShowMessage("Không thể xóa tài liệu"))
            }
        }
    }

    fun onImportEpub(uri: Uri) {
        onImportPublication(PublicationSource.LocalUri(uri))
    }

    fun onImportPublication(source: PublicationSource) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _importState.value = ImportState(isImporting = true, progress = null)

            val result = importRepository.importPublication(source) { progress ->
                _importState.value = ImportState(isImporting = true, progress = progress)
            }

            _importState.value = ImportState(isImporting = false, progress = null)

            result.onSuccess {
                _events.emit(LibraryEvent.ShowMessage("Nhập tài liệu thành công!"))
            }.onFailure { error ->
                when (error) {
                    is ImportException.DuplicateBook -> {
                        _events.emit(LibraryEvent.DuplicateFound(error.existingBookId, error.existingTitle))
                    }
                    is ImportException.DownloadCancelled -> {
                        _events.emit(LibraryEvent.ShowMessage("Đã hủy tải tài liệu"))
                    }
                    else -> {
                        _events.emit(LibraryEvent.ShowMessage(error.message ?: "Lỗi khi nhập tài liệu"))
                    }
                }
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _importState.value = ImportState(isImporting = false, progress = null)
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
                    val catalogRepo = LocalCatalogRepository(db.progressDao(), db.catalogDao())
                    val libraryRepo = LocalLibraryRepository(
                        downloadDao = db.downloadDao(),
                        progressDao = db.progressDao(),
                        favoriteDao = db.favoriteDao(),
                        catalogDao = db.catalogDao(),
                        catalogRepository = catalogRepo,
                        tagDao = db.tagDao(),
                        collectionDao = db.collectionDao()
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
                    val collectionRepo = LocalCollectionRepository(
                        collectionDao = db.collectionDao()
                    )
                    val tagRepo = LocalTagRepository(
                        tagDao = db.tagDao()
                    )
                    val prefsDataStore = LibraryPreferencesDataStore(context)
                    return MyLibraryViewModel(
                        libraryRepo,
                        downloadRepo,
                        favRepo,
                        importRepo,
                        collectionRepo,
                        tagRepo,
                        prefsDataStore
                    ) as T
                }
            }
    }
}
