package com.nocap.app

import com.nocap.app.data.download.DownloadIntegrity
import com.nocap.app.data.parser.CbzParser
import com.nocap.app.data.parser.DocumentCacheKey
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ReaderDownloadRegressionTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun emptyAndTruncatedDownloadsCannotBeCompleted() {
        for ((actual, declared) in listOf(0L to 0L, 0L to -1L, 2L to 3L)) {
            try { DownloadIntegrity.verifySize(actual, declared); fail("Invalid download accepted") }
            catch (_: IllegalStateException) { }
        }
        DownloadIntegrity.verifySize(3, 3)
        DownloadIntegrity.verifySize(3, -1)
    }
    @Test fun cacheSeparatesProfilesEntriesAndContentRevisions() {
        val first = temp.newFile("profile-a.cbz").apply { writeText("first") }
        val other = temp.newFile("profile-b.cbz").apply { writeText("first") }
        val key = DocumentCacheKey.forFile(first, "1.png", "v1")
        assertEquals(key, DocumentCacheKey.forFile(first, "1.png", "v1"))
        assertNotEquals(key, DocumentCacheKey.forFile(other, "1.png", "v1"))
        assertNotEquals(key, DocumentCacheKey.forFile(first, "2.png", "v1"))
        assertNotEquals(key, DocumentCacheKey.forFile(first, "1.png", "v2"))
        first.appendText("updated")
        assertNotEquals(key, DocumentCacheKey.forFile(first, "1.png", "v1"))
    }
    @Test fun failedExtractionDoesNotPoisonExistingCacheOrLeaveTemporaryFiles() {
        val archive = temp.newFile("pages.cbz")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("empty.png")); zip.closeEntry()
            zip.putNextEntry(ZipEntry("page.png")); zip.write("image bytes".toByteArray()); zip.closeEntry()
        }
        val cache = temp.newFolder("cache")
        val target = File(cache, "page.png").apply { writeText("previous") }
        try { CbzParser.extractPageToFile(archive, "empty.png", target); fail("Empty page accepted") }
        catch (_: IllegalStateException) { }
        assertEquals("previous", target.readText())
        assertEquals(1, cache.listFiles()!!.size)
        CbzParser.extractPageToFile(archive, "page.png", target)
        assertEquals("image bytes", target.readText())
        assertEquals(1, cache.listFiles()!!.size)
    }
}
