package com.nocap.app.data.library

import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.DownloadDao
import com.nocap.app.core.database.dao.FavoriteDao
import com.nocap.app.core.database.dao.ProgressDao
import com.nocap.app.core.database.toDomain
import com.nocap.app.core.database.toEntity
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.ReadingProgress
import com.nocap.app.domain.repository.CatalogRepository
import com.nocap.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LocalLibraryRepository(
    private val downloadDao: DownloadDao,
    private val progressDao: ProgressDao,
    private val favoriteDao: FavoriteDao,
    private val catalogDao: CatalogDao,
    private val catalogRepository: CatalogRepository
) : LibraryRepository {

    override fun observeLibraryBooks(): Flow<List<LibraryBook>> {
        return combine(
            downloadDao.observeCompletedDownloads(),
            progressDao.observeRecentlyRead(),
            favoriteDao.observeFavorites()
        ) { downloads, progresses, favorites ->
            val progressMap = progresses.associateBy { it.bookId }
            val favoriteSet = favorites.map { it.bookId }.toSet()

            downloads.mapNotNull { downloadEntity ->
                val bookId = downloadEntity.bookId
                val catalogBook = catalogDao.getBookById(bookId)?.toDomain()
                    ?: catalogRepository.getBookById(bookId)
                    ?: return@mapNotNull null

                val downloadedDomain = downloadEntity.toDomain()
                val progressDomain = progressMap[bookId]?.toDomain()
                val isFav = favoriteSet.contains(bookId)
                val isUpdateAvailable = catalogBook.contentVersion > downloadedDomain.downloadedContentVersion

                LibraryBook(
                    book = catalogBook,
                    downloadedBook = downloadedDomain,
                    readingProgress = progressDomain,
                    isFavorite = isFav,
                    isUpdateAvailable = isUpdateAvailable
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
        // Ensure catalog book exists in database for foreign key constraint
        val existingInDb = catalogDao.getBookById(progress.bookId)
        if (existingInDb == null) {
            val book = catalogRepository.getBookById(progress.bookId)
            if (book != null) {
                val categories = catalogRepository.observeCategories()
                catalogDao.insertBook(book.toEntity())
            }
        }
        progressDao.saveProgress(progress.toEntity())
    }

    override suspend fun deleteReadingProgress(bookId: String) {
        progressDao.deleteProgress(bookId)
    }
}
