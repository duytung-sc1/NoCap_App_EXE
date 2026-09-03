package com.nocap.app.domain.repository

import android.net.Uri

sealed class ImportException(message: String) : Exception(message) {
    class DuplicateBook(message: String = "Sách này đã có trong thư viện") : ImportException(message)
    class InvalidEpub(message: String = "Tệp không phải là định dạng EPUB hợp lệ hoặc đã bị lỗi") : ImportException(message)
    class StorageError(message: String = "Không thể lưu tệp vào bộ nhớ ứng dụng") : ImportException(message)
    class GeneralError(message: String) : ImportException(message)
}

interface ImportBookRepository {
    /**
     * Imports an EPUB file from a content URI via SAF.
     * Validates EPUB structure using Readium 3.3.0, extracts metadata,
     * copies to app-private storage, and persists in Room.
     *
     * @return Result with the imported bookId
     */
    suspend fun importEpub(uri: Uri): Result<String>

    /**
     * Completely removes an imported book, its local file, and all associated bookmarks and progress.
     */
    suspend fun deleteImportedBook(bookId: String): Result<Unit>
}
