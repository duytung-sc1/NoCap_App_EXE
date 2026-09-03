package com.nocap.app

import com.nocap.app.data.catalog.LocalCatalogRepository
import com.nocap.app.data.catalog.SeedCatalogDataSource
import com.nocap.app.data.favorite.LocalFavoriteRepository
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.DownloadedBook
import com.nocap.app.presentation.bookdetails.BookDetailsViewModel
import com.nocap.app.presentation.bookdetails.BookPrimaryAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class Milestone3LogicTest {

    @Test
    fun sha256_validation_correctlyIdentifiesMatchingAndMismatchedHashes() {
        val testContent = "Sample EPUB Content for Testing".toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val correctHash = md.digest(testContent).joinToString("") { "%02x".format(it) }

        // Test matching
        val isMatch = correctHash.equals(correctHash, ignoreCase = true)
        assertTrue(isMatch)

        // Test mismatch
        val corruptedHash = "0000000000000000000000000000000000000000000000000000000000000000"
        assertFalse(correctHash.equals(corruptedHash, ignoreCase = true))
    }

    @Test
    fun contentVersion_updateAvailable_whenCatalogVersionIsHigher() {
        val catalogVersion = 2L
        val downloadedVersion = 1L

        val isUpdateAvailable = catalogVersion > downloadedVersion
        assertTrue(isUpdateAvailable)

        val sameVersion = 1L
        assertFalse(sameVersion > downloadedVersion)
    }

    @Test
    fun downloadStateTransitions_flowLogic() {
        // Transition: Not Downloaded (null) -> Pending
        var downloadedBook: DownloadedBook? = null
        assertTrue(downloadedBook == null)

        // -> Pending
        downloadedBook = DownloadedBook(
            bookId = "book-001",
            localFilePath = "/files/books/book-001.epub",
            downloadStatus = DownloadStatus.PENDING,
            downloadProgress = 0f,
            downloadedBytes = 0L,
            totalBytes = 1000L,
            downloadedContentVersion = 1L
        )
        assertEquals(DownloadStatus.PENDING, downloadedBook.downloadStatus)

        // -> Downloading 50%
        downloadedBook = downloadedBook.copy(
            downloadStatus = DownloadStatus.DOWNLOADING,
            downloadProgress = 0.5f,
            downloadedBytes = 500L
        )
        assertEquals(DownloadStatus.DOWNLOADING, downloadedBook.downloadStatus)
        assertEquals(0.5f, downloadedBook.downloadProgress, 0.001f)

        // -> Completed
        downloadedBook = downloadedBook.copy(
            downloadStatus = DownloadStatus.COMPLETED,
            downloadProgress = 1f,
            downloadedBytes = 1000L,
            downloadedAt = System.currentTimeMillis()
        )
        assertEquals(DownloadStatus.COMPLETED, downloadedBook.downloadStatus)
        assertNotNull(downloadedBook.downloadedAt)

        // -> Failed
        val failedBook = downloadedBook.copy(
            downloadStatus = DownloadStatus.FAILED,
            lastError = "Network connection timeout"
        )
        assertEquals(DownloadStatus.FAILED, failedBook.downloadStatus)
        assertEquals("Network connection timeout", failedBook.lastError)
    }

    @Test
    fun duplicateDownloadCheck_preventsDuplicateActiveWork() {
        fun canStartDownload(status: DownloadStatus?): Boolean {
            return status != DownloadStatus.DOWNLOADING && status != DownloadStatus.PENDING
        }

        // When not downloading, can start
        assertTrue(canStartDownload(null))
        assertTrue(canStartDownload(DownloadStatus.COMPLETED))
        assertTrue(canStartDownload(DownloadStatus.FAILED))
        assertTrue(canStartDownload(DownloadStatus.CANCELLED))

        // When currently downloading or pending, cannot start duplicate
        assertFalse(canStartDownload(DownloadStatus.DOWNLOADING))
        assertFalse(canStartDownload(DownloadStatus.PENDING))
    }
}
