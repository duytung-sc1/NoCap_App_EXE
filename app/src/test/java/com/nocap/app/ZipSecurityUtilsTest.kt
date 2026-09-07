package com.nocap.app

import com.nocap.app.core.util.ZipSecurityUtils
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

class ZipSecurityUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test isPathTraversal detects dangerous path variations`() {
        assertTrue(ZipSecurityUtils.isPathTraversal("../evil.txt"))
        assertTrue(ZipSecurityUtils.isPathTraversal("..\\evil.txt"))
        assertTrue(ZipSecurityUtils.isPathTraversal("dir/../../evil.txt"))
        assertTrue(ZipSecurityUtils.isPathTraversal("dir\\..\\evil.txt"))
        assertTrue(ZipSecurityUtils.isPathTraversal("/absolute/path.txt"))
        assertTrue(ZipSecurityUtils.isPathTraversal("\\absolute\\path.txt"))

        assertFalse(ZipSecurityUtils.isPathTraversal("normal/sub/file.txt"))
        assertFalse(ZipSecurityUtils.isPathTraversal("file.txt"))
        assertFalse(ZipSecurityUtils.isPathTraversal("images/001.png"))
    }

    @Test
    fun `test sanitizeEntryName removes leading slashes and normalizes backslashes`() {
        assertEquals("dir/file.txt", ZipSecurityUtils.sanitizeEntryName("\\dir\\file.txt"))
        assertEquals("dir/file.txt", ZipSecurityUtils.sanitizeEntryName("/dir/file.txt"))
        assertEquals("file.txt", ZipSecurityUtils.sanitizeEntryName("file.txt"))
    }

    @Test
    fun `test validateZipStructure succeeds on benign zip file`() {
        val zipFile = tempFolder.newFile("safe.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("chapter1.txt"))
            zos.write("Hello World".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("images/pic.png"))
            zos.write(ByteArray(512))
            zos.closeEntry()
        }

        val result = ZipSecurityUtils.validateZipStructure(zipFile)
        assertEquals(2, result.entryCount)
        assertEquals(11L + 512L, result.totalUncompressedBytes)
    }

    @Test
    fun `test validateZipStructure throws on path traversal entry`() {
        val zipFile = tempFolder.newFile("traversal.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("../evil.sh"))
            zos.write("malicious".toByteArray())
            zos.closeEntry()
        }

        val ex = assertThrows(ZipSecurityUtils.ZipSecurityException::class.java) {
            ZipSecurityUtils.validateZipStructure(zipFile)
        }
        assertTrue(ex.message?.contains("Path Traversal") == true)
    }

    @Test
    fun `test validateZipStructure throws if file does not exist`() {
        val nonExistent = File(tempFolder.root, "does_not_exist.zip")
        assertThrows(ZipSecurityUtils.ZipSecurityException::class.java) {
            ZipSecurityUtils.validateZipStructure(nonExistent)
        }
    }
}
