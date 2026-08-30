package com.ebookreader.app.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ebookreader.app.core.database.dao.CatalogDao
import com.ebookreader.app.core.database.dao.DownloadDao
import com.ebookreader.app.core.database.entity.DownloadedBookEntity
import com.ebookreader.app.core.database.toDomain
import com.ebookreader.app.core.database.toEntity
import com.ebookreader.app.domain.model.DownloadStatus
import com.ebookreader.app.domain.model.DownloadedBook
import com.ebookreader.app.domain.repository.BookDownloadRepository
import com.ebookreader.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class LocalBookDownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val catalogDao: CatalogDao,
    private val catalogRepository: CatalogRepository
) : BookDownloadRepository {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun observeDownload(bookId: String): Flow<DownloadedBook?> {
        return downloadDao.observeDownloadByBookId(bookId).map { it?.toDomain() }
    }

    override fun observeAllDownloads(): Flow<List<DownloadedBook>> {
        return downloadDao.observeAllDownloads().map { list -> list.map { it.toDomain() } }
    }

    override fun observeCompletedDownloads(): Flow<List<DownloadedBook>> {
        return downloadDao.observeCompletedDownloads().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun startDownload(bookId: String) {
        val currentDownload = downloadDao.getDownloadByBookId(bookId)
        if (currentDownload?.downloadStatus == DownloadStatus.DOWNLOADING ||
            currentDownload?.downloadStatus == DownloadStatus.PENDING
        ) {
            // Already active download
            return
        }

        // Fetch catalog book info
        var book = catalogDao.getBookById(bookId)?.toDomain()
        if (book == null) {
            book = catalogRepository.getBookById(bookId)
            if (book != null) {
                // Ensure category and book exist in database for foreign key constraint
                val categories = catalogRepository.observeCategories().first()
                val category = categories.find { it.id == book.categoryId }
                if (category != null) {
                    catalogDao.insertCategories(listOf(category.toEntity()))
                }
                catalogDao.insertBook(book.toEntity())
            }
        }

        val targetFile = File(File(context.filesDir, "books"), "$bookId.epub")

        // Update database with PENDING state
        downloadDao.upsertDownload(
            DownloadedBookEntity(
                bookId = bookId,
                localFilePath = targetFile.absolutePath,
                downloadStatus = DownloadStatus.PENDING,
                downloadProgress = 0f,
                downloadedBytes = 0L,
                totalBytes = book?.fileSizeBytes ?: 0L,
                downloadedContentVersion = book?.contentVersion ?: 1L,
                contentHash = book?.contentHash,
                downloadedAt = null,
                lastError = null
            )
        )

        // Configure WorkManager request
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            BookDownloadWorker.KEY_BOOK_ID to bookId,
            BookDownloadWorker.KEY_FILE_URL to (book?.fileUrl ?: ""),
            BookDownloadWorker.KEY_EXPECTED_VERSION to (book?.contentVersion ?: 1L),
            BookDownloadWorker.KEY_EXPECTED_HASH to (book?.contentHash ?: ""),
            BookDownloadWorker.KEY_EXPECTED_SIZE to (book?.fileSizeBytes ?: 0L)
        )

        val workRequest = OneTimeWorkRequestBuilder<BookDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("download_$bookId")
            .build()

        workManager.enqueueUniqueWork(
            "download_$bookId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun cancelDownload(bookId: String) {
        workManager.cancelUniqueWork("download_$bookId")

        // Clean up temp file
        val tempFile = File(File(context.filesDir, "temp"), "$bookId.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val existing = downloadDao.getDownloadByBookId(bookId)
        if (existing != null && existing.downloadStatus != DownloadStatus.COMPLETED) {
            downloadDao.updateProgress(
                bookId = bookId,
                status = DownloadStatus.CANCELLED,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = existing.totalBytes
            )
        }
    }

    override suspend fun retryDownload(bookId: String) {
        startDownload(bookId)
    }

    override suspend fun deleteDownloadedBook(bookId: String) {
        cancelDownload(bookId)

        val targetFile = File(File(context.filesDir, "books"), "$bookId.epub")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        downloadDao.deleteDownload(bookId)
    }

    override suspend fun isBookDownloaded(bookId: String): Boolean {
        val download = downloadDao.getDownloadByBookId(bookId)
        if (download?.downloadStatus == DownloadStatus.COMPLETED) {
            val file = File(download.localFilePath)
            return file.exists() && file.length() > 0
        }
        return false
    }
}
