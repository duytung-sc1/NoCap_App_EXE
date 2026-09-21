package com.nocap.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.database.dao.ProgressDao
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.data.catalog.CloudAnnouncements
import com.nocap.app.data.catalog.CloudCatalog
import com.nocap.app.domain.model.Announcement
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.Category
import com.nocap.app.domain.model.HighlightWithBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ContinueReadingItem(
    val bookId: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val formatName: String,
    val progressPercent: Int,
    val lastReadAt: Long
)

data class RecentDocumentItem(
    val bookId: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val formatName: String
)

data class HomeUiState(
    val continueReading: List<ContinueReadingItem> = emptyList(),
    val recentDocuments: List<RecentDocumentItem> = emptyList(),
    val recentMemory: List<HighlightWithBook> = emptyList(),
    val featuredBooks: List<CatalogBook> = emptyList(),
    val categories: List<Category> = emptyList(),
    val newBooks: List<CatalogBook> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val dismissedAnnouncementIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val progressDao: ProgressDao,
    private val catalogDao: CatalogDao,
    private val highlightDao: HighlightDao
) : ViewModel() {

    private val dismissed = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HomeUiState> = combine(
        progressDao.observeRecentlyRead(),
        catalogDao.observeAllBooks(),
        highlightDao.observeAllHighlights(),
        CloudCatalog.books,
        CloudCatalog.categories,
        CloudAnnouncements.announcements,
        dismissed
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val progressList  = args[0] as List<com.nocap.app.core.database.entity.ReadingProgressEntity>
        @Suppress("UNCHECKED_CAST")
        val allBooks      = args[1] as List<CatalogBookEntity>
        @Suppress("UNCHECKED_CAST")
        val highlights    = args[2] as List<com.nocap.app.core.database.entity.HighlightEntity>
        @Suppress("UNCHECKED_CAST")
        val cloudBooks    = args[3] as List<CatalogBook>
        @Suppress("UNCHECKED_CAST")
        val categories    = args[4] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val rawAnn        = args[5] as List<Announcement>
        @Suppress("UNCHECKED_CAST")
        val dismissedIds  = args[6] as Set<String>

        val bookMap      = allBooks.associateBy { it.id }
        val cloudBookMap = cloudBooks.associateBy { it.id }

        // 1. Continue Reading
        val continueReadingItems = progressList.mapNotNull { progress ->
            val localBook  = bookMap[progress.bookId]
            val cloudBook  = cloudBookMap[progress.bookId]
            val title      = localBook?.userTitleOverride ?: localBook?.title ?: cloudBook?.title ?: return@mapNotNull null
            val author     = localBook?.userAuthorOverride ?: localBook?.author ?: cloudBook?.author ?: ""
            val coverUrl   = localBook?.customCoverPath ?: localBook?.coverUrl ?: cloudBook?.coverUrl
            val formatName = localBook?.format?.name ?: cloudBook?.format?.name ?: "EPUB"
            val percent    = (progress.progression * 100).toInt().coerceIn(0, 100)
            ContinueReadingItem(
                bookId = progress.bookId, title = title, author = author,
                coverUrl = coverUrl, formatName = formatName,
                progressPercent = percent, lastReadAt = progress.lastReadAt
            )
        }.take(5)

        // 2. Recent Documents
        val recentDocItems = allBooks
            .sortedWith(compareByDescending<CatalogBookEntity> { it.lastOpenedAt ?: 0L }.thenByDescending { it.addedAt })
            .map { book ->
                RecentDocumentItem(
                    bookId = book.id,
                    title  = book.userTitleOverride ?: book.title,
                    author = book.userAuthorOverride ?: book.author,
                    coverUrl = book.customCoverPath ?: book.coverUrl,
                    formatName = book.format.name
                )
            }.take(6)

        // 3. Recent Memory
        val recentMemoryItems = highlights.mapNotNull { hl ->
            val book = bookMap[hl.bookId] ?: return@mapNotNull null
            HighlightWithBook(highlight = hl, book = book)
        }.take(3)

        // 4. Curated explore feed
        val featured   = cloudBooks.filter { it.isFeatured }
        val newArrivals = cloudBooks.filter { it.isNew }

        // 5. Active announcements (not dismissed)
        val visibleAnn = rawAnn.filter { it.id !in dismissedIds }

        HomeUiState(
            continueReading = continueReadingItems,
            recentDocuments = recentDocItems,
            recentMemory    = recentMemoryItems,
            featuredBooks   = featured,
            categories      = categories,
            newBooks        = newArrivals,
            announcements   = visibleAnn,
            dismissedAnnouncementIds = dismissedIds,
            isLoading       = false
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun dismissAnnouncement(id: String) {
        dismissed.update { it + id }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                return HomeViewModel(
                    progressDao  = db.progressDao(),
                    catalogDao   = db.catalogDao(),
                    highlightDao = db.highlightDao()
                ) as T
            }
        }
    }
}
