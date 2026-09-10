package com.nocap.app

import com.nocap.app.data.cloud.CloudArchive
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CloudArchiveTest {
    @Test fun restoredPathsCannotReferenceAnotherAccountOrTraverse() {
        val root=Files.createTempDirectory("restore-path-test").toFile()
        try {
            val old="/data/user/0/app/files/profiles/account-a/"
            assertEquals(File(root,"books/a.epub").canonicalPath,
                CloudArchive.restoredPath("downloaded_books","local_file_path",old+"books/a.epub",old,root))
            for (path in listOf("/data/user/0/app/files/profiles/account-b/private.epub", "file:///private", old+"../account-b/private.epub")) {
                assertThrows(RuntimeException::class.java) { CloudArchive.restoredPath("downloaded_books","local_file_path",path,old,root) }
                assertThrows(RuntimeException::class.java) { CloudArchive.restoredPath("catalog_books","custom_cover_path",path,old,root) }
            }
            assertThrows(RuntimeException::class.java) { CloudArchive.restoredPath("catalog_books","cover_url","file:///private",old,root) }
            assertEquals("https://example.test/cover",CloudArchive.restoredPath("catalog_books","cover_url","https://example.test/cover",old,root))
            assertEquals("normal note",CloudArchive.restoredPath("highlights","note","normal note",old,root))
        } finally { root.deleteRecursively() }
    }
    @Test fun unsafeDocumentIdsCannotBecomeFilePaths() {
        for(id in listOf("../other", "a/b", "a\\b", "/absolute", "a..b", "a\u0000", "a".repeat(256)))
            assertFalse(com.nocap.app.core.util.DocumentIds.isSafe(id))
        for(id in listOf("gutenberg-11", "import_123", "abc.def", "a".repeat(255)))
            assertTrue(com.nocap.app.core.util.DocumentIds.isSafe(id))
    }
    private fun archive(root: File, name: String, content: String): File = File(root,"test.zip").also { file ->
        ZipOutputStream(file.outputStream()).use { zip -> zip.putNextEntry(ZipEntry(name));zip.write(content.toByteArray());zip.closeEntry() }
    }
    @Test fun validArchivePreservesFiles() {
        val root=Files.createTempDirectory("cloud-test").toFile()
        try { val zip=archive(root,"files/imported/book.txt","book contents");val out=File(root,"out");CloudArchive.extract(zip,out);assertEquals("book contents",File(out,"files/imported/book.txt").readText()) } finally { root.deleteRecursively() }
    }
    @Test fun traversalAndUnexpectedEntriesAreRejected() {
        val root=Files.createTempDirectory("cloud-test").toFile()
        try { for(name in listOf("files/../../escape.txt","files/../database.json","files/..\\escape.txt","/absolute.txt","credentials.json")) {
            val zip=archive(root,name,"bad");assertThrows(IllegalArgumentException::class.java) { CloudArchive.extract(zip,File(root,"out")) }
        } } finally { root.deleteRecursively() }
    }
    @Test fun expandedSizeLimitRejectsZipBombs() {
        val root=Files.createTempDirectory("cloud-test").toFile()
        try { val zip=archive(root,"files/large.txt","x".repeat(100000));assertThrows(IllegalArgumentException::class.java) { CloudArchive.extract(zip,File(root,"out"),1000) } } finally { root.deleteRecursively() }
    }
}
