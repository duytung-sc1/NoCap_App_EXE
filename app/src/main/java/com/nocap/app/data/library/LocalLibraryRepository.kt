package com.nocap.app.data.library

import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.CollectionDao
import com.nocap.app.core.database.dao.DownloadDao
import com.nocap.app.core.database.dao.FavoriteDao
import com.nocap.app.core.database.dao.ProgressDao
import com.nocap.app.core.database.dao.TagDao
import com.nocap.app.core.database.toDomain
import com.nocap.app.core.database.toEntity
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.ReadingProgress
import com.nocap.app.domain.repository.CatalogRepository
import com.nocap.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class LocalLibraryRepository(
    private val downloadDao: DownloadDao,
    private val progressDao: ProgressDao,
    private val favoriteDao: FavoriteDao,
    private val catalogDao: CatalogDao,
    private val catalogRepository: CatalogRepository,
    private val tagDao: TagDao? = null,
    private val collectionDao: CollectionDao? = null
) : LibraryRepository {

    override fun observeLibraryBooks(): Flow<List<LibraryBook>> {
        val tagCrossRefsFlow = tagDao?.observeAllTagCrossRefs() ?: flowOf(emptyList())
        val allTagsFlow = tagDao?.observeTags() ?: flowOf(emptyList())
        val colCrossRefsFlow = collectionDao?.observeAllCollectionCrossRefs() ?: flowOf(emptyList())
        val allColsFlow = collectionDao?.observeCollections() ?: flowOf(emptyList())

        return combine(
            downloadDao.observeCompletedDownloads(),
            progressDao.observeRecentlyRead(),
            favoriteDao.observeFavorites(),
            tagCrossRefsFlow,
            allTagsFlow
        ) { downloads, progresses, favorites, tagRefs, allTags ->
            Triple(downloads, progresses, favorites) to (tagRefs to allTags)
        }.combine(
            combine(colCrossRefsFlow, allColsFlow, catalogDao.observeAllBooks()) { colRefs, allCols, allCatalogBooks ->
                Triple(colRefs, allCols, allCatalogBooks)
            }
        ) { (base, tagData), (colRefs, allCols, allCatalogBooks) ->
            val (downloads, progresses, favorites) = base
            val (tagRefs, allTags) = tagData

            val progressMap = progresses.associateBy { it.bookId }
            val favoriteSet = favorites.map { it.bookId }.toSet()
            val tagMap = allTags.associateBy { it.id }
            val tagsByBookId = tagRefs.groupBy { it.bookId }.mapValues { entry ->
                entry.value.mapNotNull { tagMap[it.tagId]?.toDomain() }
            }
            val colMap = allCols.associateBy { it.id }
            val colsByBookId = colRefs.groupBy { it.bookId }.mapValues { entry ->
                entry.value.mapNotNull { colMap[it.collectionId]?.name }
            }
            val catalogMap = allCatalogBooks.associateBy { it.id }

            downloads.mapNotNull { downloadEntity ->
                val bookId = downloadEntity.bookId
                val catalogBook = catalogMap[bookId]?.toDomain()
                    ?: catalogDao.getBookById(bookId)?.toDomain()
                    ?: catalogRepository.getBookById(bookId)
                    ?: return@mapNotNull null

                val downloadedDomain = downloadEntity.toDomain()
                val progressDomain = progressMap[bookId]?.toDomain()
                val isFav = favoriteSet.contains(bookId)
                val isUpdateAvailable = catalogBook.contentVersion > downloadedDomain.downloadedContentVersion
                val bookTags = tagsByBookId[bookId] ?: emptyList()
                val bookCols = colsByBookId[bookId] ?: emptyList()

                LibraryBook(
                    book = catalogBook,
                    downloadedBook = downloadedDomain,
                    readingProgress = progressDomain,
                    isFavorite = isFav,
                    isUpdateAvailable = isUpdateAvailable,
                    tags = bookTags,
                    collections = bookCols
                )
            }
        }
    }

    override fun observeRecentlyReadBooks(): Flow<List<LibraryBook>> {
        return combine(
            progressDao.observeRecentlyRead(),
            downloadDao.observeCompletedDownloads(),
            favoriteDao.observeFavorites()
        ) { progresses, downloads, favorites ->
            val downloadMap = downloads.associateBy { it.bookId }
            val favoriteSet = favorites.map { it.bookId }.toSet()

            progresses.mapNotNull { progressEntity ->
                val bookId = progressEntity.bookId
                val catalogBook = catalogDao.getBookById(bookId)?.toDomain()
                    ?: catalogRepository.getBookById(bookId)
                    ?: return@mapNotNull null

                val downloadedEntity = downloadMap[bookId]
                val downloadedDomain = downloadedEntity?.toDomain()
                    ?: com.nocap.app.domain.model.DownloadedBook(
                        bookId = bookId,
                        localFilePath = "",
                        downloadStatus = com.nocap.app.domain.model.DownloadStatus.COMPLETED,
                        downloadProgress = 1f,
                        downloadedBytes = catalogBook.fileSizeBytes,
                        totalBytes = catalogBook.fileSizeBytes,
                        downloadedContentVersion = catalogBook.contentVersion
                    )

                val isFav = favoriteSet.contains(bookId)
                val isUpdateAvailable = catalogBook.contentVersion > downloadedDomain.downloadedContentVersion

                LibraryBook(
                    book = catalogBook,
                    downloadedBook = downloadedDomain,
                    readingProgress = progressEntity.toDomain(),
                    isFavorite = isFav,
                    isUpdateAvailable = isUpdateAvailable
                )
            }
        }
    }

    override fun observeBookProgress(bookId: String): Flow<ReadingProgress?> {
        return progressDao.observeProgress(bookId).map { it?.toDomain() }
    }

    override suspend fun saveReadingProgress(progress: ReadingProgress) {
        val existingInDb = catalogDao.getBookById(progress.bookId)
        if (existingInDb == null) {
            val book = catalogRepository.getBookById(progress.bookId)
            if (book != null) {
                catalogRepository.observeCategories()
                catalogDao.insertBook(book.toEntity())
            }
        }
        progressDao.saveProgress(progress.toEntity())
    }

    override suspend fun deleteReadingProgress(bookId: String) {
        progressDao.deleteProgress(bookId)
    }

    override suspend fun setInboxState(bookId: String, inInbox: Boolean) {
        catalogDao.updateInboxState(bookId, inInbox)
    }

    override suspend fun setPinnedState(bookId: String, isPinned: Boolean) {
        catalogDao.updatePinnedState(bookId, isPinned)
    }

    override suspend fun setArchivedState(bookId: String, isArchived: Boolean) {
        catalogDao.updateArchivedState(bookId, isArchived)
    }

    override suspend fun setReadingStatus(bookId: String, status: DocumentReadingStatus) {
        catalogDao.updateReadingStatus(bookId, status)
    }

    override suspend fun setMetadataOverrides(bookId: String, titleOverride: String?, authorOverride: String?) {
        catalogDao.updateMetadataOverrides(bookId, titleOverride, authorOverride)
    }

    override suspend fun setCustomCover(bookId: String, coverPath: String?) {
        catalogDao.updateCustomCover(bookId, coverPath)
    }

    override suspend fun updateLastOpenedAt(bookId: String, timestamp: Long) {
        catalogDao.updateLastOpenedAt(bookId, timestamp)
    }

    override suspend fun bulkArchive(bookIds: List<String>, isArchived: Boolean) {
        catalogDao.updateBulkArchive(bookIds, isArchived)
    }

    override suspend fun bulkPin(bookIds: List<String>, isPinned: Boolean) {
        catalogDao.updateBulkPin(bookIds, isPinned)
    }

    override suspend fun bulkSetReadingStatus(bookIds: List<String>, status: DocumentReadingStatus) {
        catalogDao.updateBulkReadingStatus(bookIds, status)
    }
}
