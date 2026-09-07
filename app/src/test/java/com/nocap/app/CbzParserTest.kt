package com.nocap.app

import com.nocap.app.data.parser.CbzParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CbzParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSyntheticCbz(file: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
    }

    @Test
    fun `test natural order sorting of comic pages`() {
        val file = tempFolder.newFile("comic.cbz")
        // Add pages out of order
        val entries = mapOf(
            "page_10.png" to ByteArray(16),
            "page_1.png" to ByteArray(16),
            "page_2.png" to ByteArray(16),
            "page_20.jpg" to ByteArray(16),
            "page_3.webp" to ByteArray(16)
        )
        createSyntheticCbz(file, entries)

        val pages = CbzParser.validateAndListPages(file)
        assertEquals(5, pages.size)
        assertEquals("page_1.png", pages[0].fileName)
        assertEquals("page_2.png", pages[1].fileName)
        assertEquals("page_3.webp", pages[2].fileName)
        assertEquals("page_10.png", pages[3].fileName)
        assertEquals("page_20.jpg", pages[4].fileName)
    }

    @Test
    fun `test filters out macos metadata and non-image files`() {
        val file = tempFolder.newFile("mixed.cbz")
        val entries = mapOf(
            "__MACOSX/._01.jpg" to ByteArray(16),
            ".DS_Store" to ByteArray(16),
            "readme.txt" to ByteArray(16),
            "cover.jpg" to ByteArray(16),
            "chapter1/page1.png" to ByteArray(16)
        )
        createSyntheticCbz(file, entries)

        val pages = CbzParser.validateAndListPages(file)
        assertEquals(2, pages.size)
        assertEquals("chapter1/page1.png", pages[0].entryName)
        assertEquals("cover.jpg", pages[1].entryName)
    }

    @Test
    fun `test extract page to file and thumbnail extraction`() {
        val file = tempFolder.newFile("story.cbz")
        val pageData = "FakeImageData".toByteArray()
        val entries = mapOf("01.jpg" to pageData)
        createSyntheticCbz(file, entries)

        val targetFile = tempFolder.newFile("thumb.jpg")
        val extracted = CbzParser.extractFirstPageThumbnail(file, targetFile)
        assertTrue(extracted)
        assertEquals("FakeImageData", targetFile.readText())
    }

    @Test
    fun `test reject cbz with no images`() {
        val file = tempFolder.newFile("empty_cbz.cbz")
        createSyntheticCbz(file, mapOf("notes.txt" to "no images".toByteArray()))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            CbzParser.validateAndListPages(file)
        }
        assertTrue(ex.message?.contains("không chứa trang ảnh hợp lệ") == true)
    }
}
