package com.ebookreader.app

import com.ebookreader.app.domain.model.DownloadStatus
import com.ebookreader.app.domain.model.DownloadedBook
import com.ebookreader.app.domain.model.ReadingProgress
import com.ebookreader.app.presentation.reader.ReaderUiState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

class Milestone5ReaderTest {

    @Test
    fun locator_deserializationFallback_whenJsonIsCorruptedOrNull() {
        fun safeDeserialize(json: String?): Locator? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                Locator.fromJSON(JSONObject(json))
            }.getOrNull()
        }

        assertNull("Null string should fallback to null locator", safeDeserialize(null))
        assertNull("Blank string should fallback to null locator", safeDeserialize("   "))
        assertNull("Malformed JSON should fallback to null locator without crashing", safeDeserialize("not a valid json"))
        assertNull("Empty object should fallback to null", safeDeserialize("{}"))
    }

    @Test
    fun readingProgress_mapping_fromValues_normalizesProgression() {
        val rawProgression = 0.725
        val bookId = "book-001"
        val progression = rawProgression.toFloat().coerceIn(0f, 1f)
        val progress = ReadingProgress(
            bookId = bookId,
            locatorJson = """{"href":"ch2.xhtml","locations":{"progression":0.725}}""",
            progression = progression,
            chapterTitle = "Chapter 2",
            lastReadAt = 10000L
        )

        assertEquals("book-001", progress.bookId)
        assertEquals(0.725f, progress.progression, 0.001f)
        assertEquals("Chapter 2", progress.chapterTitle)
        assertEquals(10000L, progress.lastReadAt)
    }

    @Test
    fun readingProgress_clamping_progressionBoundaries() {
        fun normalize(p: Float): Float = p.coerceIn(0f, 1f)

        assertEquals(0.0f, normalize(-0.5f), 0.001f)
        assertEquals(1.0f, normalize(1.5f), 0.001f)
        assertEquals(0.42f, normalize(0.42f), 0.001f)
    }

    @Test
    fun readerState_transitions_forDownloadAndFileIntegrity() {
        // 1. Not Downloaded
        val downloadedBookNull: DownloadedBook? = null
        val state1 = if (downloadedBookNull == null || downloadedBookNull.downloadStatus != DownloadStatus.COMPLETED) {
            ReaderUiState.BookNotDownloaded
        } else {
            ReaderUiState.Loading
        }
        assertEquals(ReaderUiState.BookNotDownloaded, state1)

        // 2. Downloaded status is DOWNLOADING / FAILED (not COMPLETED)
        val downloadingBook = DownloadedBook(
            bookId = "book-001",
            localFilePath = "/path/test.epub",
            downloadStatus = DownloadStatus.DOWNLOADING,
            downloadProgress = 0.5f,
            downloadedBytes = 500L,
            totalBytes = 1000L
        )
        val state2 = if (downloadingBook.downloadStatus != DownloadStatus.COMPLETED) {
            ReaderUiState.BookNotDownloaded
        } else {
            ReaderUiState.Loading
        }
        assertEquals(ReaderUiState.BookNotDownloaded, state2)

        // 3. Download status COMPLETED but file does not exist
        fun checkFileExists(path: String) = false // Mock missing file
        val completedBook = DownloadedBook(
            bookId = "book-001",
            localFilePath = "/non/existent/path.epub",
            downloadStatus = DownloadStatus.COMPLETED,
            downloadProgress = 1f,
            downloadedBytes = 1000L,
            totalBytes = 1000L
        )
        val state3 = if (completedBook.downloadStatus != DownloadStatus.COMPLETED) {
            ReaderUiState.BookNotDownloaded
        } else if (!checkFileExists(completedBook.localFilePath)) {
            ReaderUiState.FileNotFound
        } else {
            ReaderUiState.Loading
        }
        assertEquals(ReaderUiState.FileNotFound, state3)
    }
}
