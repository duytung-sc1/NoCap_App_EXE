package com.nocap.app.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.nocap.app.core.database.dao.BookmarkDao
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.DownloadDao
import com.nocap.app.core.database.dao.FavoriteDao
import com.nocap.app.core.database.dao.ProgressDao
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.CategoryEntity
import com.nocap.app.core.database.entity.DownloadedBookEntity
import com.nocap.app.data.reader.ReadiumPublicationManager
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.repository.ImportBookRepository
import com.nocap.app.domain.repository.ImportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class LocalImportBookRepository(
    private val context: Context,
    private val catalogDao: CatalogDao,
    private val downloadDao: DownloadDao,
    private val progressDao: ProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val favoriteDao: FavoriteDao,
    private val publicationManager: ReadiumPublicationManager = ReadiumPublicationManager(context)
) : ImportBookRepository {

    override suspend fun importEpub(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val originalFilename = queryFilename(uri) ?: "imported_${System.currentTimeMillis()}.epub"
        val tempFile = File(context.cacheDir, "import_temp_${UUID.randomUUID()}.epub")

        try {
            // 1. Stream copy and compute SHA-256 simultaneously
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(ImportException.StorageError("Không thể mở tệp từ nguồn được chọn"))

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            if (tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext Result.failure(ImportException.InvalidEpub("Tệp EPUB trống"))
            }

            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

            // 2. Check for duplicates
            val existingBook = catalogDao.getBookByHash(sha256)
                ?: downloadDao.getDownloadByHash(sha256)?.let { catalogDao.getBookById(it.bookId) }

            if (existingBook != null) {
                tempFile.delete()
                return@withContext Result.failure(ImportException.DuplicateBook("Sách \"${existingBook.title}\" đã có trong thư viện"))
            }

            // 3. Validate EPUB with Readium
            val publicationResult = publicationManager.openPublication(tempFile)
            if (publicationResult.isFailure) {
                tempFile.delete()
                return@withContext Result.failure(
                    ImportException.InvalidEpub("Tệp không phải là định dạng EPUB hợp lệ hoặc đã bị lỗi")
                )
            }

            val publication = publicationResult.getOrThrow()
            val rawTitle = publication.metadata.title
            val title = if (!rawTitle.isNullOrBlank()) {
                rawTitle
            } else {
                originalFilename.substringBeforeLast(".").ifBlank { "Sách chưa đặt tên" }
            }

            val author = publication.metadata.authors.firstOrNull()?.name?.ifBlank { null }
                ?: "Tác giả chưa xác định"

            val description = publication.metadata.description ?: ""

            publicationManager.closePublication(publication)

            // 4. Move to permanent app-private directory
            val importedDir = File(context.filesDir, "imported").apply { if (!exists()) mkdirs() }
            val bookId = "imported_${sha256.take(12)}"
            val finalFile = File(importedDir, "$bookId.epub")

            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!tempFile.renameTo(finalFile)) {
                // Fallback copy if rename across filesystems fails
                tempFile.inputStream().use { input ->
                    finalFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
            }

            // 5. Ensure "imported" category exists
            val existingCategory = catalogDao.getCategoryById("imported")
            if (existingCategory == null) {
                catalogDao.insertCategories(
                    listOf(
                        CategoryEntity(
                            id = "imported",
                            name = "Sách cá nhân",
                            displayOrder = 99
                        )
                    )
                )
            }

            // 6. Save to Room database
            val catalogEntity = CatalogBookEntity(
                id = bookId,
                title = title,
                author = author,
                description = description,
                coverUrl = "",
                categoryId = "imported",
                fileUrl = "",
                fileSizeBytes = finalFile.length(),
                contentVersion = 1L,
                contentHash = sha256,
                isFeatured = false,
                isNew = false,
                isPremium = false,
                rating = 0f,
                publishedDate = null,
                updatedAt = System.currentTimeMillis()
            )

            val downloadEntity = DownloadedBookEntity(
                bookId = bookId,
                localFilePath = finalFile.absolutePath,
                downloadStatus = DownloadStatus.COMPLETED,
                downloadProgress = 1f,
                downloadedBytes = finalFile.length(),
                totalBytes = finalFile.length(),
                downloadedContentVersion = 1L,
                contentHash = sha256,
                downloadedAt = System.currentTimeMillis()
            )

            catalogDao.insertBook(catalogEntity)
            downloadDao.upsertDownload(downloadEntity)

            Result.success(bookId)
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(ImportException.GeneralError("Lỗi khi nhập sách: ${e.localizedMessage ?: "Không xác định"}"))
        }
    }

    override suspend fun deleteImportedBook(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val download = downloadDao.getDownloadByBookId(bookId)
            if (download != null && download.localFilePath.isNotBlank()) {
                val file = File(download.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }

            bookmarkDao.deleteBookmarksByBookId(bookId)
            progressDao.deleteProgress(bookId)
            favoriteDao.removeFavorite(bookId)
            downloadDao.deleteDownload(bookId)
            catalogDao.deleteBook(bookId)

            Unit
        }
    }

    private fun queryFilename(uri: Uri): String? {
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
}
