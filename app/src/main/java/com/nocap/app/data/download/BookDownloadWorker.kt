package com.nocap.app.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.entity.DownloadedBookEntity
import com.nocap.app.domain.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class BookDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_BOOK_ID = "key_book_id"
        const val KEY_FILE_URL = "key_file_url"
        const val KEY_EXPECTED_VERSION = "key_expected_version"
        const val KEY_EXPECTED_HASH = "key_expected_hash"
        const val KEY_EXPECTED_SIZE = "key_expected_size"
        const val KEY_PROGRESS = "progress"

        private val okHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val bookId = inputData.getString(KEY_BOOK_ID) ?: return@withContext Result.failure()
        val fileUrl = inputData.getString(KEY_FILE_URL) ?: return@withContext Result.failure()
        val expectedVersion = inputData.getLong(KEY_EXPECTED_VERSION, 1L)
        val expectedHash = inputData.getString(KEY_EXPECTED_HASH)
        val expectedSize = inputData.getLong(KEY_EXPECTED_SIZE, 0L)

        val database = AppDatabase.getInstance(applicationContext)
        val downloadDao = database.downloadDao()

        val booksDir = File(applicationContext.filesDir, "books").apply { if (!exists()) mkdirs() }
        val tempDir = File(applicationContext.filesDir, "temp").apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDir, "$bookId.tmp")
        val targetFile = File(booksDir, "$bookId.epub")

        try {
            // 1. Update status to DOWNLOADING in Room
            val initialEntity = DownloadedBookEntity(
                bookId = bookId,
                localFilePath = targetFile.absolutePath,
                downloadStatus = DownloadStatus.DOWNLOADING,
                downloadProgress = 0f,
                downloadedBytes = 0L,
                totalBytes = expectedSize,
                downloadedContentVersion = expectedVersion,
                contentHash = null,
                downloadedAt = null,
                lastError = null
            )
            downloadDao.upsertDownload(initialEntity)

            // If fileUrl is empty or blank, handle gracefully
            if (fileUrl.isBlank()) {
                throw IllegalArgumentException("Download URL is blank for book: $bookId")
            }

            // 2. Execute streaming request
            val request = Request.Builder()
                .url(fileUrl)
                .header("User-Agent", "EbookReaderApp/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP download failed with code: ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Empty response body from server")
            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) contentLength else expectedSize

            val digest = MessageDigest.getInstance("SHA-256")
            var downloadedBytes = 0L
            var lastProgressUpdateTime = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            tempFile.delete()
                            downloadDao.updateProgress(
                                bookId = bookId,
                                status = DownloadStatus.CANCELLED,
                                progress = 0f,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                            return@withContext Result.failure()
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val currentTime = System.currentTimeMillis()
                        if (totalBytes > 0 && (currentTime - lastProgressUpdateTime > 500 || downloadedBytes == totalBytes)) {
                            lastProgressUpdateTime = currentTime
                            val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                            downloadDao.updateProgress(
                                bookId = bookId,
                                status = DownloadStatus.DOWNLOADING,
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                        }
                    }
                    outputStream.flush()
                }
            }

            // 3. Validation: Content-Length check
            if (contentLength > 0 && downloadedBytes != contentLength) {
                throw IllegalStateException("Downloaded size ($downloadedBytes) does not match Content-Length ($contentLength)")
            }

            // 4. Validation: SHA-256 Hash check
            val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!expectedHash.isNullOrBlank()) {
                if (!computedHash.equals(expectedHash.trim(), ignoreCase = true)) {
                    throw IllegalStateException("SHA-256 mismatch: expected $expectedHash, computed $computedHash")
                }
            }

            // 5. Atomic Finalize: Rename temp file to target EPUB
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                // Fallback copy & delete if renameTo fails across file systems
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            // 6. Update Room status = COMPLETED
            val completedEntity = DownloadedBookEntity(
                bookId = bookId,
                localFilePath = targetFile.absolutePath,
                downloadStatus = DownloadStatus.COMPLETED,
                downloadProgress = 1f,
                downloadedBytes = downloadedBytes,
                totalBytes = downloadedBytes,
                downloadedContentVersion = expectedVersion,
                contentHash = computedHash,
                downloadedAt = System.currentTimeMillis(),
                lastError = null
            )
            downloadDao.upsertDownload(completedEntity)

            Result.success()
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            val finalStatus = if (isStopped) DownloadStatus.CANCELLED else DownloadStatus.FAILED
            downloadDao.upsertDownload(
                DownloadedBookEntity(
                    bookId = bookId,
                    localFilePath = targetFile.absolutePath,
                    downloadStatus = finalStatus,
                    downloadProgress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = expectedSize,
                    downloadedContentVersion = expectedVersion,
                    contentHash = null,
                    downloadedAt = null,
                    lastError = e.message ?: "Unknown download error"
                )
            )
            Result.failure()
        }
    }
}
