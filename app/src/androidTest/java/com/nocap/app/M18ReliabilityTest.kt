package com.nocap.app

import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.importer.LocalImportBookRepository
import com.nocap.app.data.search.LocalKnowledgeSearchRepository
import com.nocap.app.domain.model.KnowledgeItemType
import com.nocap.app.domain.model.PublicationSource
import com.nocap.app.domain.repository.ImportException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.system.measureTimeMillis

/** Generated fixtures and an isolated in-memory DB; never touches an account or backend. */
@RunWith(AndroidJUnit4::class)
class M18ReliabilityTest {
    @Test fun removeDownloadUsesStoredPathAndRejectsForeignFiles() = runBlocking(Dispatchers.IO) {
        isolated { db, repo, root ->
            val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
                override fun getFilesDir() = File(root, "files")
            }
            val downloads = com.nocap.app.data.download.LocalBookDownloadRepository(context,
                db.downloadDao(), db.catalogDao(), com.nocap.app.data.catalog.LocalCatalogRepository())
            val source = File(root, "remove.txt").apply { writeText("Remove download fixture") }
            val id = repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(source))).getOrThrow()
            val saved = db.downloadDao().getDownloadByBookId(id)!!
            val outside = File(root, "other-profile.txt").apply { writeText("Protected") }
            db.downloadDao().upsertDownload(saved.copy(localFilePath = outside.path))
            try { downloads.deleteDownloadedBook(id); fail("Foreign path accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals("Protected", outside.readText())
            assertNotNull(db.downloadDao().getDownloadByBookId(id))
            db.downloadDao().upsertDownload(saved)
            downloads.deleteDownloadedBook(id)
            assertFalse(File(saved.localFilePath).exists())
            assertNull(db.downloadDao().getDownloadByBookId(id))
            assertNotNull(db.catalogDao().getBookById(id))
            try { downloads.cancelDownload("../../outside"); fail("Traversal accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun concurrentReaderBookmarksDedupeWithoutCrossingBooksOrTombstones() = runBlocking(Dispatchers.IO) {
        isolated { db, repo, root ->
            val first = File(root, "bookmarks.txt").apply { writeText("Bookmark concurrency fixture") }
            val bookId = repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(first))).getOrThrow()
            val locator = com.nocap.app.domain.model.TextLocator(blockIndex = 4, scrollOffsetPx = 650).toJson()
            fun bookmark(id: String = bookId) = com.nocap.app.core.database.entity.BookmarkEntity(
                id = UUID.randomUUID().toString(), bookId = id, locatorJson = locator, chapterTitle = "Fixture")
            coroutineScope { (1..20).map { async { db.bookmarkDao().insertReaderBookmarkIfAbsent(bookmark()) } }.awaitAll() }
            val saved = db.bookmarkDao().getBookmarksForBook(bookId).single()
            assertEquals(650, com.nocap.app.domain.model.TextLocator.fromJson(saved.locatorJson)!!.scrollOffsetPx)
            val second = File(root, "other.txt").apply { writeText("Other document") }
            val secondId = repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(second))).getOrThrow()
            db.bookmarkDao().insertReaderBookmarkIfAbsent(bookmark(secondId))
            assertEquals(1, db.bookmarkDao().getBookmarksForBook(secondId).size)
            db.bookmarkDao().softDeleteBookmark(saved.id)
            db.bookmarkDao().insertReaderBookmarkIfAbsent(bookmark())
            assertEquals(1, db.bookmarkDao().getBookmarksForBook(bookId).size)
            assertNotEquals(saved.id, db.bookmarkDao().getBookmarksForBook(bookId).single().id)
        }
    }
    private suspend fun isolated(test: suspend (AppDatabase, LocalImportBookRepository, File) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val root = File(base.cacheDir, "m18-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
        }
        val db = Room.inMemoryDatabaseBuilder(base, AppDatabase::class.java).build()
        try {
            val repo = LocalImportBookRepository(context, db.catalogDao(), db.downloadDao(), db.progressDao(),
                db.bookmarkDao(), db.favoriteDao(), database = db)
            test(db, repo, root)
        } finally { db.close(); check(root.parentFile == base.cacheDir); root.deleteRecursively() }
    }

    @Test fun interruptedAndMalformedImportsRemainRecoverable() = runBlocking(Dispatchers.IO) {
        isolated { db, repo, root ->
            val text = File(root, "Đọc 日本語 한글 #1.txt").apply {
                writeText((1..4000).joinToString("\n\n") { "Paragraph $it " + "reading ".repeat(30) })
            }
            try {
                repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(text))) {
                    throw CancellationException("controlled interruption")
                }
                fail("Cancellation must propagate")
            } catch (_: CancellationException) { }
            assertTrue(File(root, "cache").listFiles().orEmpty().isEmpty())
            assertTrue(db.catalogDao().getAllBooks().isEmpty())
            val failed = repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(text))) {
                throw java.io.IOException("controlled provider failure")
            }
            assertTrue(failed.isFailure)
            assertTrue(File(root, "cache").listFiles().orEmpty().isEmpty())
            for ((name, content) in listOf("empty.txt" to "", "broken.pdf" to "%PDF-broken", "broken.png" to "not an image")) {
                val invalid = File(root, name).apply { writeText(content) }
                assertTrue(name, repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(invalid))).isFailure)
            }
            assertTrue(repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(File(root, "missing.txt")))).isFailure)
            assertTrue(db.catalogDao().getAllBooks().isEmpty())
            val results = coroutineScope { listOf(async { repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(text))) },
                async { repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(text))) }).awaitAll() }
            assertEquals(results.map { it.exceptionOrNull()?.message }.toString(), 1, results.count { it.isSuccess })
            assertEquals(1, results.count { it.exceptionOrNull() is ImportException.DuplicateBook })
            val book = db.catalogDao().getAllBooks().single()
            assertEquals(text.name, book.originalFilename)
            assertEquals(text.readText(), File(db.downloadDao().getDownloadByBookId(book.id)!!.localFilePath).readText())
        }
    }

    @Test fun failedDownloadInsertRollsBackMetadata() = runBlocking(Dispatchers.IO) {
        isolated { db, repo, root ->
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER m18_fail BEFORE INSERT ON downloaded_books BEGIN SELECT RAISE(ABORT, 'controlled failure'); END")
            val file = File(root, "rollback.txt").apply { writeText("A real document for atomic import testing.") }
            assertTrue(repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(file))).isFailure)
            assertTrue(db.catalogDao().getAllBooks().isEmpty())
            assertTrue("Failed import must not leave a permanent document", File(root, "files/imported").listFiles().orEmpty().isEmpty())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER m18_fail")
            assertTrue(repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(file))).isSuccess)
            assertEquals(1, db.catalogDao().getAllBooks().size)
        }
    }

    @Test fun importedDeletionRejectsForeignPathAndPreservesMetadata() = runBlocking(Dispatchers.IO) {
        isolated { db, repo, root ->
            val source = File(root, "delete.txt").apply { writeText("Document deletion fixture") }
            val id = repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(source))).getOrThrow()
            val download = db.downloadDao().getDownloadByBookId(id)!!
            val foreign = File(root, "private.txt").apply { writeText("Must remain safe") }
            db.downloadDao().upsertDownload(download.copy(localFilePath = foreign.path))
            assertTrue(repo.deleteImportedBook(id).isFailure)
            assertEquals("Must remain safe", foreign.readText())
            assertNotNull(db.catalogDao().getBookById(id))
            assertTrue(File(download.localFilePath).exists())
            db.downloadDao().upsertDownload(download)
            assertTrue(repo.deleteImportedBook(id).isSuccess)
            assertFalse(File(download.localFilePath).exists())
            assertNull(db.catalogDao().getBookById(id))
        }
    }

    @Test fun largeLibrarySearchBaseline() = runBlocking(Dispatchers.IO) {
        isolated { db, repo, root ->
            val file = File(root, "seed.txt").apply { writeText("Temporary local stress document.") }
            val id = repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(file))).getOrThrow()
            val template = db.catalogDao().getBookById(id)!!
            db.withTransaction {
                db.catalogDao().insertBooks((1..3000).map { template.copy(id = "stress-$it", title = "Knowledge $it", contentHash = null) })
            }
            val search = LocalKnowledgeSearchRepository(db.catalogDao(), db.highlightDao(), db.bookmarkDao(), db.downloadDao())
            val millis = measureTimeMillis {
                assertEquals(3000, search.search("Knowledge", KnowledgeItemType.DOCUMENT, null).size)
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
                putString("stream", "M18 isolated 3000-document search: ${millis}ms\n")
            })
        }
    }

    @Test fun updatingCategoryWithExistingBooksDoesNotDeleteOrFail() = runBlocking(Dispatchers.IO) {
        isolated { db, repo, root ->
            val file = File(root, "first.txt").apply { writeText("First retained book") }
            val id = repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(file))).getOrThrow()
            val category = db.catalogDao().getCategoryById("imported")!!
            db.catalogDao().insertCategories(listOf(category.copy(name = "Updated category")))
            val second = File(root, "second.txt").apply { writeText("Second retained book") }
            assertTrue(repo.importPublication(PublicationSource.LocalUri(Uri.fromFile(second))).isSuccess)
            assertNotNull(db.catalogDao().getBookById(id))
            assertEquals(2, db.catalogDao().getAllBooks().size)
            assertEquals("Updated category", db.catalogDao().getCategoryById("imported")!!.name)
        }
    }
}
