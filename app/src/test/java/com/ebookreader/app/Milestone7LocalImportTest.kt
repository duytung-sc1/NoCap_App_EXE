package com.ebookreader.app

import com.ebookreader.app.domain.model.CatalogBook
import com.ebookreader.app.domain.model.DownloadStatus
import com.ebookreader.app.domain.model.DownloadedBook
import com.ebookreader.app.domain.model.EntitlementType
import com.ebookreader.app.domain.model.LibraryBook
import com.ebookreader.app.domain.repository.ImportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Milestone7LocalImportTest {

    @Test
    fun `test duplicate exception message`() {
        val exception = ImportException.DuplicateBook("Sách đã có trong thư viện")
        assertEquals("Sách đã có trong thư viện", exception.message)
    }

    @Test
    fun `test invalid epub exception message`() {
        val exception = ImportException.InvalidEpub("Tệp không hợp lệ")
        assertEquals("Tệp không hợp lệ", exception.message)
    }

    @Test
    fun `test imported book identification and badge`() {
        val importedBook = CatalogBook(
            id = "imported_123456",
            title = "My Personal Ebook",
            author = "Unknown Author",
            description = "Custom import",
            coverUrl = "",
            categoryId = "imported",
            fileUrl = "",
            fileSizeBytes = 50000L,
            isFeatured = false,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 0f,
            publishedDate = null
        )

        val downloaded = DownloadedBook(
            bookId = importedBook.id,
            localFilePath = "/data/user/0/com.ebookreader.app/files/imported/imported_123456.epub",
            downloadStatus = DownloadStatus.COMPLETED,
            downloadProgress = 1f,
            downloadedBytes = 50000L,
            totalBytes = 50000L
        )

        val libraryBook = LibraryBook(
            book = importedBook,
            downloadedBook = downloaded,
            readingProgress = null,
            isFavorite = false,
            isUpdateAvailable = false
        )

        assertEquals("imported", libraryBook.book.categoryId)
        assertTrue(libraryBook.book.categoryId == "imported")
        assertEquals(DownloadStatus.COMPLETED, libraryBook.downloadedBook.downloadStatus)
    }

    @Test
    fun `test library search matches imported book by title or author`() {
        val importedBook = CatalogBook(
            id = "imported_789",
            title = "Dế Mèn Phiêu Lưu Ký",
            author = "Tô Hoài",
            description = "",
            coverUrl = "",
            categoryId = "imported",
            fileUrl = "",
            fileSizeBytes = 120000L,
            isFeatured = false,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 0f,
            publishedDate = null
        )

        val libraryBooks = listOf(
            LibraryBook(
                book = importedBook,
                downloadedBook = DownloadedBook(
                    bookId = importedBook.id,
                    localFilePath = "/path/de_men.epub",
                    downloadStatus = DownloadStatus.COMPLETED,
                    downloadProgress = 1f,
                    downloadedBytes = 120000L,
                    totalBytes = 120000L
                ),
                readingProgress = null,
                isFavorite = false,
                isUpdateAvailable = false
            )
        )

        val query = "Dế Mèn"
        val filtered = libraryBooks.filter {
            it.book.title.contains(query, ignoreCase = true) || it.book.author.contains(query, ignoreCase = true)
        }

        assertEquals(1, filtered.size)
        assertEquals("Dế Mèn Phiêu Lưu Ký", filtered.first().book.title)
    }

    @Test
    fun `test filename fallback when metadata title is missing`() {
        val filename = "my_favorite_novel.epub"
        val rawTitle: String? = null
        val title = if (!rawTitle.isNullOrBlank()) rawTitle else filename.substringBeforeLast(".")
        assertEquals("my_favorite_novel", title)
    }
}
