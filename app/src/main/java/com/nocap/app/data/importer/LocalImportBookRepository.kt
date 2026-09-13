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
import com.nocap.app.domain.model.DownloadProgress
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSource
import com.nocap.app.domain.repository.ImportBookRepository
import com.nocap.app.domain.repository.ImportException
import com.nocap.app.data.parser.CbzParser
import com.nocap.app.data.parser.DocxParser
import com.nocap.app.data.parser.HtmlSanitizerParser
import com.nocap.app.data.parser.ImageValidator
import com.nocap.app.data.parser.MarkdownParser
import com.nocap.app.data.parser.TxtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
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
    private val publicationManager: ReadiumPublicationManager = ReadiumPublicationManager(context),
    private val remoteDownloader: RemotePublicationDownloader = RemotePublicationDownloader(context),
    private val database: com.nocap.app.core.database.AppDatabase = com.nocap.app.core.database.AppDatabase.getInstance(context)
) : ImportBookRepository {
    companion object { private val importLock = Mutex() }
    private val profile = com.nocap.app.data.sync.Profiles.active.value
    private val profileFiles = com.nocap.app.data.sync.Profiles.files(context, profile)


    override suspend fun importEpub(uri: Uri): Result<String> {
        return importPublication(PublicationSource.LocalUri(uri))
    }

    override suspend fun importPublication(
        source: PublicationSource,
        onProgress: ((DownloadProgress) -> Unit)?
    ): Result<String> = importLock.withLock { withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            var suggestedFilename: String? = null
            var sourceMimeType: String? = null

            when (source) {
                is PublicationSource.LocalUri -> {
                    suggestedFilename = source.displayNameHint ?: queryFilename(source.uri)
                    tempFile = copyUriToTemp(source.uri, suggestedFilename, onProgress)
                }
                is PublicationSource.SharedUri -> {
                    suggestedFilename = queryFilename(source.uri)
                    sourceMimeType = source.mimeType
                    tempFile = copyUriToTemp(source.uri, suggestedFilename, onProgress)
                }
                is PublicationSource.RemoteUrl -> {
                    val downloadRes = remoteDownloader.download(source.url, onProgress).getOrThrow()
                    tempFile = downloadRes.tempFile
                    suggestedFilename = downloadRes.suggestedFilename
                    sourceMimeType = downloadRes.contentType
                }
                is PublicationSource.SharedUrl -> {
                    val downloadRes = remoteDownloader.download(source.url, onProgress).getOrThrow()
                    tempFile = downloadRes.tempFile
                    suggestedFilename = downloadRes.suggestedFilename
                    sourceMimeType = downloadRes.contentType
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext Result.failure(ImportException.FileNotFound("Tệp trống hoặc không thể đọc"))
            }

            // 1. Calculate SHA-256
            val sha256 = calculateSha256(tempFile)

            // 2. Cross-source duplicate detection
            val existingBook = catalogDao.getBookByHash(sha256)
                ?: downloadDao.getDownloadByHash(sha256)?.let { catalogDao.getBookById(it.bookId) }

            if (existingBook != null) {
                tempFile.delete()
                return@withContext Result.failure(
                    ImportException.DuplicateBook(
                        existingBookId = existingBook.id,
                        existingTitle = existingBook.title
                    )
                )
            }

            // 3. Sniff format
            val format = FormatSniffer.sniff(tempFile)
                ?: FormatSniffer.sniffFromExtension(suggestedFilename ?: "")
                ?: FormatSniffer.sniffFromMimeType(sourceMimeType)

            if (format == null) {
                tempFile.delete()
                return@withContext Result.failure(
                    ImportException.UnsupportedFormat("Định dạng tệp không được hỗ trợ.")
                )
            }

            var title = (suggestedFilename ?: "imported_${System.currentTimeMillis()}")
                .substringBeforeLast(".")
                .ifBlank { "Tài liệu chưa đặt tên" }
            var author = "Tác giả chưa xác định"
            var description = ""

            // 4. Validate & extract metadata by format
            when (format) {
                PublicationFormat.EPUB, PublicationFormat.PDF -> {
                    val publicationResult = publicationManager.openPublication(tempFile)
                    if (publicationResult.isFailure) {
                        tempFile.delete()
                        return@withContext Result.failure(
                            if (format == PublicationFormat.PDF) {
                                ImportException.CorruptPdf("Tệp PDF bị lỗi hoặc không thể phân tích cú pháp")
                            } else {
                                ImportException.InvalidEpub("Tệp không phải là định dạng EPUB hợp lệ hoặc đã bị lỗi")
                            }
                        )
                    }
                    val publication = publicationResult.getOrThrow()
                    try {
                    val rawTitle = publication.metadata.title
                    if (!rawTitle.isNullOrBlank()) title = rawTitle
                    val rawAuthor = publication.metadata.authors.firstOrNull()?.name?.ifBlank { null }
                    if (rawAuthor != null) author = rawAuthor
                    description = publication.metadata.description ?: ""
                    } finally { publicationManager.closePublication(publication) }
                }
                PublicationFormat.TXT -> {
                    try {
                        val parsed = TxtParser.validateAndParse(tempFile, suggestedFilename)
                        title = parsed.title
                    } catch (e: Exception) {
                        tempFile.delete()
                        return@withContext Result.failure(ImportException.InvalidEpub("Tệp TXT không hợp lệ hoặc bị lỗi: ${e.message}"))
                    }
                }
                PublicationFormat.MARKDOWN -> {
                    try {
                        val parsed = MarkdownParser.validateAndParse(tempFile, suggestedFilename)
                        title = parsed.title
                    } catch (e: Exception) {
                        tempFile.delete()
                        return@withContext Result.failure(ImportException.InvalidEpub("Tệp Markdown không hợp lệ hoặc bị lỗi: ${e.message}"))
                    }
                }
                PublicationFormat.HTML -> {
                    try {
                        val parsed = HtmlSanitizerParser.validateAndParse(tempFile, suggestedFilename)
                        title = parsed.title
                    } catch (e: Exception) {
                        tempFile.delete()
                        return@withContext Result.failure(ImportException.InvalidEpub("Tệp HTML không hợp lệ hoặc bị lỗi: ${e.message}"))
                    }
                }
                PublicationFormat.DOCX -> {
                    try {
                        val docxMediaDir = File(context.cacheDir, "docx_media").apply { if (!exists()) mkdirs() }
                        val parsed = DocxParser.parse(tempFile, docxMediaDir, suggestedFilename)
                        title = parsed.title
                        if (!parsed.author.isNullOrBlank()) author = parsed.author
                    } catch (e: Exception) {
                        tempFile.delete()
                        return@withContext Result.failure(ImportException.InvalidEpub("Tệp DOCX không hợp lệ hoặc bị lỗi: ${e.message}"))
                    }
                }
                PublicationFormat.JPEG, PublicationFormat.PNG, PublicationFormat.WEBP -> {
                    try {
                        ImageValidator.validateImageBounds(tempFile)
                    } catch (e: Exception) {
                        tempFile.delete()
                        return@withContext Result.failure(ImportException.InvalidEpub("Tệp ảnh không hợp lệ hoặc bị lỗi: ${e.message}"))
                    }
                }
                PublicationFormat.CBZ -> {
                    try {
                        val pages = CbzParser.validateAndListPages(tempFile)
                        if (pages.isEmpty()) {
                            tempFile.delete()
                            return@withContext Result.failure(ImportException.InvalidEpub("Tệp CBZ không chứa trang ảnh hợp lệ."))
                        }
                    } catch (e: Exception) {
                        tempFile.delete()
                        return@withContext Result.failure(ImportException.InvalidEpub("Tệp CBZ không hợp lệ hoặc bị lỗi: ${e.message}"))
                    }
                }
            }

            // 5. Atomic move to permanent directory
            val importedDir = File(profileFiles, "imported").apply { if (!exists()) mkdirs() }
            val bookId = "imported_${sha256.take(12)}"
            val ext = FormatSniffer.extensionFor(format)
            val finalFile = File(importedDir, "$bookId.$ext")

            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!tempFile.renameTo(finalFile)) {
                tempFile.inputStream().use { input ->
                    finalFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
            }

            // Cover handling for Image and CBZ
            var customCoverPath: String? = null
            if (format.isSingleImage) {
                customCoverPath = finalFile.absolutePath
            } else if (format == PublicationFormat.CBZ) {
                val coversDir = File(profileFiles, "covers").apply { if (!exists()) mkdirs() }
                val coverFile = File(coversDir, "$bookId.jpg")
                if (CbzParser.extractFirstPageThumbnail(finalFile, coverFile)) {
                    customCoverPath = coverFile.absolutePath
                }
            }

            // 6. Ensure "imported" category exists
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

            val sourceUrl = when (source) {
                is PublicationSource.RemoteUrl -> source.url
                is PublicationSource.SharedUrl -> source.url
                else -> null
            }

            // 7. Save to Room database
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
                updatedAt = System.currentTimeMillis(),
                format = format,
                mediaType = FormatSniffer.mimeTypeFor(format),
                sourceType = source.sourceType,
                sourceUrl = sourceUrl,
                isInInbox = true,
                inboxAddedAt = System.currentTimeMillis(),
                isPinned = false,
                isArchived = false,
                readingStatus = com.nocap.app.domain.model.DocumentReadingStatus.UNREAD,
                userTitleOverride = null,
                userAuthorOverride = null,
                customCoverPath = customCoverPath,
                lastOpenedAt = null,
                addedAt = System.currentTimeMillis(),
                originalFilename = suggestedFilename
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

            currentCoroutineContext().ensureActive()
            database.withTransaction {
                catalogDao.insertBook(catalogEntity)
                downloadDao.upsertDownload(downloadEntity)
            }

            Result.success(bookId)
        } catch (e: Exception) {
            tempFile?.delete()
            if (e is CancellationException) throw e
            if (e is ImportException) {
                Result.failure(e)
            } else {
                Result.failure(ImportException.GeneralError("Lỗi khi nhập sách: ${e.localizedMessage ?: "Không xác định"}"))
            }
        }
    } }

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

    private suspend fun copyUriToTemp(
        uri: Uri,
        suggestedFilename: String?,
        onProgress: ((DownloadProgress) -> Unit)?
    ): File {
        val tempFile = File(context.cacheDir, "import_temp_${UUID.randomUUID()}.tmp")
        val inputStream = if (uri.scheme == "file") {
            val path = uri.path ?: throw ImportException.StorageError("Đường dẫn tệp không hợp lệ")
            FileInputStream(File(path))
        } else {
            context.contentResolver.openInputStream(uri)
                ?: throw ImportException.StorageError("Không thể mở tệp từ nguồn được chọn")
        }

        val totalBytes = if (uri.scheme == "file") {
            uri.path?.let { File(it).length() } ?: -1L
        } else {
            runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }.getOrDefault(-1L)
        }

        var copiedBytes = 0L
        val buffer = ByteArray(8192)

        try {
        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    currentCoroutineContext().ensureActive()
                    copiedBytes += bytesRead
                    if (copiedBytes > RemotePublicationDownloader.MAX_FILE_SIZE_BYTES) {
                        output.flush()
                        tempFile.delete()
                        throw ImportException.FileSizeLimitExceeded()
                    }
                    output.write(buffer, 0, bytesRead)

                    onProgress?.invoke(
                        DownloadProgress(
                            bytesRead = copiedBytes,
                            totalBytes = if (totalBytes > 0) totalBytes else -1L
                        )
                    )
                }
                output.flush()
            }
        }
        } catch (error: Throwable) {
            tempFile.delete()
            throw error
        }
        return tempFile
    }

    private suspend fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                currentCoroutineContext().ensureActive()
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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
