package com.nocap.app

import com.nocap.app.data.importer.ManagedDocumentFiles
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ManagedDocumentFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun deletesActualFormatAndAllowsAlreadyMissingFile() {
        val root = temporary.newFolder("profile")
        for (extension in listOf("txt", "pdf", "docx", "cbz", "epub")) {
            val file = File(root, "imported/book.$extension").apply {
                parentFile!!.mkdirs(); writeText("fixture")
            }
            ManagedDocumentFiles.delete(root, file.path)
            assertFalse(file.exists())
            ManagedDocumentFiles.delete(root, file.path)
        }
    }

    @Test fun rejectsOtherProfileTraversalDirectoriesAndUnrelatedFiles() {
        val root = temporary.newFolder("profile")
        val other = temporary.newFolder("other")
        val protected = File(other, "books/private.txt").apply { parentFile!!.mkdirs(); writeText("safe") }
        val directory = File(root, "imported/folder").apply { mkdirs() }
        val paths = listOf(protected.path, File(root, "imported/../../other/books/private.txt").path,
            File(root, "preferences.xml").path, directory.path, "")
        for (path in paths) {
            try { ManagedDocumentFiles.delete(root, path); fail("Unsafe path accepted") }
            catch (_: IllegalArgumentException) { }
        }
        assertEquals("safe", protected.readText())
        assertTrue(directory.exists())
    }

    @Test fun allowsDeletingRestoredBooksFromCloudBackup() {
        val root = temporary.newFolder("profile")
        val restoredFile = File(root, "restored-${java.util.UUID.randomUUID()}/files/imported/book.epub").apply {
            parentFile!!.mkdirs(); writeText("fixture")
        }
        ManagedDocumentFiles.delete(root, restoredFile.path)
        assertFalse(restoredFile.exists())
    }
}
