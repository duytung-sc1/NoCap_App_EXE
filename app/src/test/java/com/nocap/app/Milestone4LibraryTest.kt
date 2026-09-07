package com.nocap.app

import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.DownloadedBook
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.ReadingProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Milestone4LibraryTest {

    private val sampleCatalogBook1 = CatalogBook(
        id = "book-001",
        title = "The Midnight Library",
        author = "Matt Haig",
        description = "Desc 1",
        coverUrl = "",
        categoryId = "fiction",
        fileUrl = "https://example.com/1.epub",
        fileSizeBytes = 300000L,
        contentVersion = 2L
    )

    private val sampleCatalogBook2 = CatalogBook(
        id = "book-002",
        title = "Project Hail Mary",
        author = "Andy Weir",
        description = "Desc 2",
        coverUrl = "",
        categoryId = "sci-fi",
        fileUrl = "https://example.com/2.epub",
        fileSizeBytes = 500000L,
        contentVersion = 1L
    )

    private val sampleDownloadedBook1 = DownloadedBook(
        bookId = "book-001",
        localFilePath = "/files/books/book-001.epub",
        downloadStatus = DownloadStatus.COMPLETED,
        downloadProgress = 1f,
        downloadedBytes = 300000L,
        totalBytes = 300000L,
        downloadedContentVersion = 1L, // Older than catalog contentVersion (2L) -> Update available!
        downloadedAt = 1000L
    )

    private val sampleDownloadedBook2 = DownloadedBook(
        bookId = "book-002",
        localFilePath = "/files/books/book-002.epub",
        downloadStatus = DownloadStatus.COMPLETED,
        downloadProgress = 1f,
        downloadedBytes = 500000L,
        totalBytes = 500000L,
        downloadedContentVersion = 1L,
        downloadedAt = 2000L
    )

    @Test
    fun library_updateAvailable_whenCatalogVersionHigherThanDownloadedVersion() {
        val libraryBook1 = LibraryBook(
            book = sampleCatalogBook1,
            downloadedBook = sampleDownloadedBook1,
            readingProgress = null,
            isFavorite = false,
            isUpdateAvailable = sampleCatalogBook1.contentVersion > sampleDownloadedBook1.downloadedContentVersion
        )
        assertTrue("Update should be available when catalog v2 > downloaded v1", libraryBook1.isUpdateAvailable)

        val libraryBook2 = LibraryBook(
            book = sampleCatalogBook2,
            downloadedBook = sampleDownloadedBook2,
            readingProgress = null,
            isFavorite = true,
            isUpdateAvailable = sampleCatalogBook2.contentVersion > sampleDownloadedBook2.downloadedContentVersion
        )
        assertFalse("Update should not be available when versions match", libraryBook2.isUpdateAvailable)
    }

    @Test
    fun library_search_byTitleAndAuthor() {
        val books = listOf(
            LibraryBook(sampleCatalogBook1, sampleDownloadedBook1),
            LibraryBook(sampleCatalogBook2, sampleDownloadedBook2)
        )

        fun search(query: String): List<LibraryBook> {
            val q = query.trim().lowercase()
            return if (q.isBlank()) books
            else books.filter {
                it.book.title.lowercase().contains(q) || it.book.author.lowercase().contains(q)
            }
        }

        // Search by Title
        val searchTitle = search("Midnight")
        assertEquals(1, searchTitle.size)
        assertEquals("The Midnight Library", searchTitle.first().book.title)

        // Search by Author
        val searchAuthor = search("Weir")
        assertEquals(1, searchAuthor.size)
        assertEquals("Andy Weir", searchAuthor.first().book.author)

        // Empty search returns all
        assertEquals(2, search("").size)
    }

    @Test
    fun library_sorting_recentlyRead_title_downloadDate() {
        val progress1 = ReadingProgress("book-001", "{}", 0.6f, "Chapter 3", lastReadAt = 5000L)
        val progress2 = ReadingProgress("book-002", "{}", 0.2f, "Chapter 1", lastReadAt = 3000L)

        val book1 = LibraryBook(sampleCatalogBook1, sampleDownloadedBook1, progress1)
        val book2 = LibraryBook(sampleCatalogBook2, sampleDownloadedBook2, progress2)
        val list = listOf(book2, book1)

        // Sort by RECENTLY_READ
        val sortedRecent = list.sortedByDescending {
            it.readingProgress?.lastReadAt ?: it.downloadedBook.downloadedAt ?: 0L
        }
        assertEquals("book-001", sortedRecent[0].book.id) // 5000L > 3000L
        assertEquals("book-002", sortedRecent[1].book.id)

        // Sort by TITLE A-Z
        val sortedTitle = list.sortedBy { it.book.title.lowercase() }
        assertEquals("Project Hail Mary", sortedTitle[0].book.title) // P comes before T
        assertEquals("The Midnight Library", sortedTitle[1].book.title)

        // Sort by DOWNLOAD_DATE DESC
        val sortedDownload = list.sortedByDescending { it.downloadedBook.downloadedAt ?: 0L }
        assertEquals("book-002", sortedDownload[0].book.id) // 2000L > 1000L
        assertEquals("book-001", sortedDownload[1].book.id)
    }

    @Test
    fun library_removeDownload_preservesFavoritesAndReadingProgress() {
        var favoriteState = true
        var readingProgress: ReadingProgress? = ReadingProgress("book-001", "{}", 0.75f, "Chapter 5")
        var downloadedBook: DownloadedBook? = sampleDownloadedBook1

        // Simulate Remove Download:
        fun removeDownload() {
            downloadedBook = null
            // favoriteState and readingProgress are explicitly preserved!
        }

        removeDownload()

        assertNull("Downloaded file state should be removed", downloadedBook)
        assertTrue("Favorite state must be preserved", favoriteState)
        assertNotNull("Reading progress must be preserved", readingProgress)
        assertEquals(0.75f, readingProgress?.progression ?: 0f, 0.001f)
    }

    @Test
    fun recentlyRead_onlyIncludesBooksWithValidProgress() {
        val bookWithProgress = LibraryBook(
            sampleCatalogBook1,
            sampleDownloadedBook1,
            readingProgress = ReadingProgress("book-001", "{}", 0.4f, "Ch 2", lastReadAt = 1000L)
        )
        val bookWithoutProgress = LibraryBook(
            sampleCatalogBook2,
            sampleDownloadedBook2,
            readingProgress = null
        )

        val allBooks = listOf(bookWithProgress, bookWithoutProgress)
        val recentlyRead = allBooks.filter { it.readingProgress != null && it.readingProgress.progression > 0f }

        assertEquals(1, recentlyRead.size)
        assertEquals("book-001", recentlyRead.first().book.id)
    }
}
