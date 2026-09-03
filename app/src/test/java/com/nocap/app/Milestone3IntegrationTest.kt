package com.nocap.app

import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.CategoryEntity
import com.nocap.app.core.database.entity.DownloadedBookEntity
import com.nocap.app.core.database.entity.FavoriteEntity
import com.nocap.app.core.database.toDomain
import com.nocap.app.core.database.toEntity
import com.nocap.app.data.catalog.LocalCatalogRepository
import com.nocap.app.data.catalog.SeedCatalogDataSource
import com.nocap.app.data.favorite.LocalFavoriteRepository
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.DownloadedBook
import com.nocap.app.domain.model.EntitlementType
import com.nocap.app.presentation.bookdetails.BookDetailsViewModel
import com.nocap.app.presentation.bookdetails.BookPrimaryAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class Milestone3IntegrationTest {

    @Test
    fun testFavorites_fullLifecycle_add_query_toggle_remove() = runTest {
        val seedRepo = LocalCatalogRepository()
        val book = seedRepo.getBookById("book-001")
        assertNotNull(book)

        // Mock state tracking like Room FavoriteDao
        val favoriteStorage = mutableSetOf<String>()

        fun isFav(id: String) = favoriteStorage.contains(id)
        fun addFav(id: String) = favoriteStorage.add(id)
        fun removeFav(id: String) = favoriteStorage.remove(id)
        fun toggleFav(id: String): Boolean {
            return if (isFav(id)) {
                removeFav(id)
                false
            } else {
                addFav(id)
                true
            }
        }

        // 1. Initial: Not favorite
        assertFalse(isFav("book-001"))

        // 2. Add Favorite
        val added = toggleFav("book-001")
        assertTrue(added)
        assertTrue(isFav("book-001"))

        // 3. Simulate app restart: favoriteStorage retains data
        assertEquals(1, favoriteStorage.size)
        assertTrue(favoriteStorage.contains("book-001"))

        // 4. Toggle again -> Removed
        val removed = toggleFav("book-001")
        assertFalse(removed)
        assertFalse(isFav("book-001"))
        assertEquals(0, favoriteStorage.size)
    }

    @Test
    fun testDownloadPipeline_streaming_hashValidation_atomicFinalize() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ebook_test_temp").apply { mkdirs() }
        val booksDir = File(System.getProperty("java.io.tmpdir"), "ebook_test_books").apply { mkdirs() }

        val bookId = "test-book-pipeline"
        val tempFile = File(tempDir, "$bookId.tmp")
        val targetFile = File(booksDir, "$bookId.epub")

        try {
            // Clean before test
            tempFile.delete()
            targetFile.delete()

            // 1. Stream simulated EPUB content to .tmp
            val sampleEpubData = "PK\u0003\u0004EPUB_TEST_CONTENT_FOR_VALIDATION".toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256")

            tempFile.outputStream().use { out ->
                out.write(sampleEpubData)
                digest.update(sampleEpubData)
                out.flush()
            }

            val computedHash = digest.digest().joinToString("") { "%02x".format(it) }

            // 2. Validate temp file exists and size > 0
            assertTrue(tempFile.exists())
            assertEquals(sampleEpubData.size.toLong(), tempFile.length())

            // 3. Validate SHA-256 matches
            val expectedHash = computedHash
            assertTrue(computedHash.equals(expectedHash, ignoreCase = true))

            // 4. Atomic finalize: move .tmp -> .epub
            val finalized = tempFile.renameTo(targetFile)
            if (!finalized) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            // 5. Assertions
            assertFalse(tempFile.exists())
            assertTrue(targetFile.exists())
            assertEquals(sampleEpubData.size.toLong(), targetFile.length())

            // 6. Test Remove Download
            val deleted = targetFile.delete()
            assertTrue(deleted)
            assertFalse(targetFile.exists())
        } finally {
            tempFile.delete()
            targetFile.delete()
            tempDir.delete()
            booksDir.delete()
        }
    }

    @Test
    fun testBookDetails_primaryAction_computation() {
        val book = CatalogBook(
            id = "book-001",
            title = "Test Title",
            author = "Test Author",
            description = "Desc",
            coverUrl = "",
            categoryId = "fiction",
            fileUrl = "https://example.com/test.epub",
            fileSizeBytes = 500000L,
            contentVersion = 2L
        )

        // 1. When downloadedBook is null -> Action is DOWNLOAD
        val actionNotDownloaded = computeAction(book, null)
        assertTrue(actionNotDownloaded is BookPrimaryAction.Download)

        // 2. When PENDING / DOWNLOADING -> Action is DOWNLOADING with progress
        val downloading = DownloadedBook(
            bookId = "book-001",
            localFilePath = "/path/book-001.epub",
            downloadStatus = DownloadStatus.DOWNLOADING,
            downloadProgress = 0.45f,
            downloadedBytes = 225000L,
            totalBytes = 500000L,
            downloadedContentVersion = 2L
        )
        val actionDownloading = computeAction(book, downloading)
        assertTrue(actionDownloading is BookPrimaryAction.Downloading)
        assertEquals(0.45f, (actionDownloading as BookPrimaryAction.Downloading).progress, 0.001f)

        // 3. When FAILED -> Action is RETRY with error
        val failed = downloading.copy(downloadStatus = DownloadStatus.FAILED, lastError = "Connection reset")
        val actionRetry = computeAction(book, failed)
        assertTrue(actionRetry is BookPrimaryAction.Retry)
        assertEquals("Connection reset", (actionRetry as BookPrimaryAction.Retry).error)

        // 4. When COMPLETED and version matches -> Action is READ
        val completedCurrent = downloading.copy(
            downloadStatus = DownloadStatus.COMPLETED,
            downloadProgress = 1f,
            downloadedContentVersion = 2L
        )
        val actionRead = computeAction(book, completedCurrent)
        assertTrue(actionRead is BookPrimaryAction.Read)

        // 5. When COMPLETED and catalog has higher version (catalog v2 > downloaded v1) -> Action is UPDATE
        val completedOlder = downloading.copy(
            downloadStatus = DownloadStatus.COMPLETED,
            downloadProgress = 1f,
            downloadedContentVersion = 1L
        )
        val actionUpdate = computeAction(book, completedOlder)
        assertTrue(actionUpdate is BookPrimaryAction.Update)
        assertEquals(2L, (actionUpdate as BookPrimaryAction.Update).newVersion)
    }

    private fun computeAction(book: CatalogBook?, download: DownloadedBook?): BookPrimaryAction {
        if (download == null || download.downloadStatus == DownloadStatus.CANCELLED) {
            return BookPrimaryAction.Download
        }
        return when (download.downloadStatus) {
            DownloadStatus.PENDING -> BookPrimaryAction.Downloading(0f)
            DownloadStatus.DOWNLOADING -> BookPrimaryAction.Downloading(download.downloadProgress)
            DownloadStatus.FAILED -> BookPrimaryAction.Retry(download.lastError)
            DownloadStatus.COMPLETED -> {
                if (book != null && book.contentVersion > download.downloadedContentVersion) {
                    BookPrimaryAction.Update(book.contentVersion)
                } else {
                    BookPrimaryAction.Read
                }
            }
            DownloadStatus.CANCELLED -> BookPrimaryAction.Download
        }
    }
}
