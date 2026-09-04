package com.nocap.app.domain.repository

import android.net.Uri

import com.nocap.app.domain.model.DownloadProgress
import com.nocap.app.domain.model.PublicationSource

sealed class ImportException(message: String) : Exception(message) {
    data class DuplicateBook(
        val existingBookId: String,
        val existingTitle: String,
        override val message: String = "Sách \"$existingTitle\" đã có trong thư viện"
    ) : ImportException(message)

    class InvalidUrl(message: String = "Liên kết không hợp lệ") : ImportException(message)
    class InsecureScheme(message: String = "Chỉ hỗ trợ liên kết HTTPS an toàn") : ImportException(message)
    class FileNotFound(message: String = "Không tìm thấy tệp hoặc tệp rỗng") : ImportException(message)
    class UnsupportedFormat(message: String = "Định dạng tệp chưa được hỗ trợ") : ImportException(message)
    class CorruptPdf(message: String = "Tệp PDF bị hỏng hoặc không đúng định dạng") : ImportException(message)
    class CorruptEpub(message: String = "Tệp EPUB bị hỏng hoặc không đúng định dạng") : ImportException(message)
    class InvalidEpub(message: String = "Tệp không phải là định dạng EPUB hợp lệ hoặc đã bị lỗi") : ImportException(message)
    class FileSizeLimitExceeded(message: String = "Tệp vượt quá giới hạn dung lượng cho phép (tối đa 250 MB)") : ImportException(message)
    class DownloadFailed(message: String = "Không thể tải tệp từ liên kết") : ImportException(message)
    class DownloadCancelled(message: String = "Quá trình tải đã bị hủy") : ImportException(message)
    class StorageError(message: String = "Không thể lưu tệp vào bộ nhớ ứng dụng") : ImportException(message)
    class GeneralError(message: String) : ImportException(message)
}

interface ImportBookRepository {
    /**
     * Legacy M7 method for backward compatibility.
     * Imports an EPUB file from a content URI via SAF.
     */
    suspend fun importEpub(uri: Uri): Result<String>

    /**
     * Unified M9A publication import from any supported PublicationSource
     * (LocalUri, RemoteUrl, SharedUri, SharedUrl).
     */
    suspend fun importPublication(
        source: PublicationSource,
        onProgress: ((DownloadProgress) -> Unit)? = null
    ): Result<String>

    /**
     * Completely removes an imported book, its local file, and all associated bookmarks and progress.
     */
    suspend fun deleteImportedBook(bookId: String): Result<Unit>
}
