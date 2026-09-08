package com.nocap.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nocap.app.core.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun `test MIGRATION_1_2 executes correct alter table statements`() {
        val executedSqls = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSqls.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        AppDatabase.MIGRATION_1_2.migrate(dbProxy)

        assertEquals(4, executedSqls.size)
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN format TEXT NOT NULL DEFAULT 'EPUB'") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN media_type TEXT NOT NULL DEFAULT 'application/epub+zip'") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN source_type TEXT NOT NULL DEFAULT 'LOCAL_FILE'") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN source_url TEXT DEFAULT NULL") })
    }

    @Test
    fun `test MIGRATION_2_3 executes correct create table and index statements`() {
        val executedSqls = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSqls.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)

        AppDatabase.MIGRATION_2_3.migrate(dbProxy)

        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS collections") })
        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS book_collection_cross_ref") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_book_collection_cross_ref_book_id") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_book_collection_cross_ref_collection_id") })
        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS highlights") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_highlights_book_id") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_highlights_created_at") })
        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS per_book_preferences") })
        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS custom_fonts") })
    }

    @Test
    fun `test MIGRATION_3_4 executes correct alter table, create table, and index statements`() {
        val executedSqls = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSqls.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)

        AppDatabase.MIGRATION_3_4.migrate(dbProxy)

        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN is_in_inbox INTEGER NOT NULL DEFAULT 0") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN inbox_added_at INTEGER DEFAULT NULL") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN reading_status TEXT NOT NULL DEFAULT 'UNREAD'") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN user_title_override TEXT DEFAULT NULL") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN user_author_override TEXT DEFAULT NULL") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN custom_cover_path TEXT DEFAULT NULL") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN last_opened_at INTEGER DEFAULT NULL") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN added_at INTEGER NOT NULL DEFAULT 0") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN original_filename TEXT DEFAULT NULL") })
        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS tags") })
        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS book_tag_cross_ref") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_book_id") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_tag_id") })
    }

    @Test
    fun `test real SQLite v3 to v4 migration preserves all M10 data and leaves existing books OUT of inbox`() {
        val conn = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")

        // 1. Create full v3 schema in SQLite
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE categories (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    display_order INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE catalog_books (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    author TEXT NOT NULL,
                    description TEXT NOT NULL,
                    cover_url TEXT NOT NULL,
                    category_id TEXT NOT NULL,
                    file_url TEXT NOT NULL,
                    file_size_bytes INTEGER NOT NULL,
                    content_version INTEGER NOT NULL,
                    content_hash TEXT,
                    is_featured INTEGER NOT NULL,
                    is_new INTEGER NOT NULL,
                    is_premium INTEGER NOT NULL,
                    rating REAL NOT NULL,
                    published_date TEXT,
                    updated_at INTEGER NOT NULL,
                    format TEXT NOT NULL DEFAULT 'EPUB',
                    media_type TEXT NOT NULL DEFAULT 'application/epub+zip',
                    source_type TEXT NOT NULL DEFAULT 'LOCAL_FILE',
                    source_url TEXT DEFAULT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE downloaded_books (
                    book_id TEXT NOT NULL PRIMARY KEY,
                    local_file_path TEXT NOT NULL,
                    downloaded_at INTEGER NOT NULL,
                    file_size_bytes INTEGER NOT NULL,
                    download_status TEXT NOT NULL,
                    download_progress REAL NOT NULL,
                    failure_reason TEXT
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE reading_progress (
                    book_id TEXT NOT NULL PRIMARY KEY,
                    last_locator TEXT NOT NULL,
                    progression REAL NOT NULL,
                    last_read_at INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE bookmarks (
                    id TEXT NOT NULL PRIMARY KEY,
                    book_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    locator TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE favorite_books (
                    book_id TEXT NOT NULL PRIMARY KEY,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE collections (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE book_collection_cross_ref (
                    book_id TEXT NOT NULL,
                    collection_id TEXT NOT NULL,
                    added_at INTEGER NOT NULL,
                    PRIMARY KEY(book_id, collection_id)
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE highlights (
                    id TEXT NOT NULL PRIMARY KEY,
                    book_id TEXT NOT NULL,
                    locator TEXT NOT NULL,
                    text_snippet TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    note TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE per_book_preferences (
                    book_id TEXT NOT NULL PRIMARY KEY,
                    font_family TEXT,
                    font_size_multiplier REAL,
                    line_height REAL,
                    text_align TEXT,
                    background_color INTEGER,
                    text_color INTEGER,
                    scroll_mode INTEGER,
                    page_animation TEXT,
                    is_pinned_to_book INTEGER NOT NULL DEFAULT 1,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE custom_fonts (
                    id TEXT NOT NULL PRIMARY KEY,
                    font_family TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    file_size INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())

            // 2. Seed M10 existing data
            // Pre-existing EPUB book (with reading progress 45%, favorite, collection, bookmarks, highlights, per-book preferences)
            stmt.execute("""
                INSERT INTO catalog_books VALUES (
                    'm10_epub_1', 'Clean Architecture', 'Robert C. Martin', 'Architecture guide',
                    '', 'imported', '/files/clean_arch.epub', 1024000, 1, 'hash_epub_1',
                    0, 0, 0, 5.0, '2020', 1680000000000, 'EPUB', 'application/epub+zip', 'LOCAL_FILE', NULL
                )
            """.trimIndent())
            stmt.execute("INSERT INTO downloaded_books VALUES ('m10_epub_1', '/files/clean_arch.epub', 1680000100000, 1024000, 'COMPLETED', 1.0, NULL)")
            stmt.execute("INSERT INTO reading_progress VALUES ('m10_epub_1', '{\"chapter\": 3}', 0.45, 1700000000000)")
            stmt.execute("INSERT INTO favorite_books VALUES ('m10_epub_1', 1680000200000)")
            stmt.execute("INSERT INTO collections VALUES ('col_engineering', 'Software Engineering', 1680000300000)")
            stmt.execute("INSERT INTO book_collection_cross_ref VALUES ('m10_epub_1', 'col_engineering', 1680000400000)")
            stmt.execute("INSERT INTO bookmarks VALUES ('bm_1', 'm10_epub_1', 'Chapter 3 Bookmark', '{\"chapter\": 3}', 1680000500000)")
            stmt.execute("INSERT INTO highlights VALUES ('hl_1', 'm10_epub_1', '{\"chapter\": 3}', 'SOLID principles', -256, 'Important rule', 1680000600000, 1680000600000)")
            stmt.execute("INSERT INTO per_book_preferences VALUES ('m10_epub_1', 'Literata', 1.2, 1.5, 'JUSTIFY', -1, -16777216, 0, 'SLIDE', 1, 1680000700000)")

            // Pre-existing PDF book (100% completed)
            stmt.execute("""
                INSERT INTO catalog_books VALUES (
                    'm10_pdf_2', 'Deep Learning Report', 'Ian Goodfellow', 'Research paper',
                    '', 'imported', '/files/dl_report.pdf', 2048000, 1, 'hash_pdf_2',
                    0, 0, 0, 4.8, '2023', 1690000000000, 'PDF', 'application/pdf', 'LOCAL_FILE', NULL
                )
            """.trimIndent())
            stmt.execute("INSERT INTO downloaded_books VALUES ('m10_pdf_2', '/files/dl_report.pdf', 1690000100000, 2048000, 'COMPLETED', 1.0, NULL)")
            stmt.execute("INSERT INTO reading_progress VALUES ('m10_pdf_2', '{\"page\": 50}', 1.0, 1700000500000)")
            stmt.execute("INSERT INTO book_collection_cross_ref VALUES ('m10_pdf_2', 'col_engineering', 1690000400000)")

            // Pre-existing Catalog book without download
            stmt.execute("""
                INSERT INTO catalog_books VALUES (
                    'm10_catalog_3', 'Cloud Computing', 'Author Three', 'Cloud overview',
                    '', 'cat_cloud', '', 500000, 1, 'hash_cat_3',
                    0, 0, 0, 4.0, '2021', 1670000000000, 'EPUB', 'application/epub+zip', 'CATALOG', NULL
                )
            """.trimIndent())
        }

        // 3. Run MIGRATION_3_4
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                conn.createStatement().use { it.execute(args[0] as String) }
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_3_4.migrate(dbProxy)

        // 4. Verify pre-existing documents DO NOT enter Inbox (is_in_inbox == 0)
        conn.createStatement().use { stmt ->
            // m10_epub_1
            val rsEpub = stmt.executeQuery("SELECT id, title, is_in_inbox, inbox_added_at, is_pinned, is_archived, reading_status, last_opened_at, added_at FROM catalog_books WHERE id = 'm10_epub_1'")
            assertTrue(rsEpub.next())
            assertEquals("m10_epub_1", rsEpub.getString("id"))
            assertEquals("Clean Architecture", rsEpub.getString("title"))
            assertEquals(0, rsEpub.getInt("is_in_inbox")) // MUST BE OUT OF INBOX
            org.junit.Assert.assertNull(rsEpub.getObject("inbox_added_at"))
            assertEquals(0, rsEpub.getInt("is_pinned"))
            assertEquals(0, rsEpub.getInt("is_archived"))
            assertEquals("READING", rsEpub.getString("reading_status")) // backfilled from progression 0.45
            assertEquals(1700000000000L, rsEpub.getLong("last_opened_at")) // backfilled from reading_progress
            assertEquals(1680000100000L, rsEpub.getLong("added_at")) // backfilled from downloaded_books
            rsEpub.close()

            // m10_pdf_2
            val rsPdf = stmt.executeQuery("SELECT id, title, is_in_inbox, inbox_added_at, is_pinned, is_archived, reading_status, last_opened_at, added_at FROM catalog_books WHERE id = 'm10_pdf_2'")
            assertTrue(rsPdf.next())
            assertEquals("m10_pdf_2", rsPdf.getString("id"))
            assertEquals("Deep Learning Report", rsPdf.getString("title"))
            assertEquals(0, rsPdf.getInt("is_in_inbox")) // MUST BE OUT OF INBOX
            org.junit.Assert.assertNull(rsPdf.getObject("inbox_added_at"))
            assertEquals(0, rsPdf.getInt("is_pinned"))
            assertEquals(0, rsPdf.getInt("is_archived"))
            assertEquals("COMPLETED", rsPdf.getString("reading_status")) // backfilled from progression 1.0 >= 0.98
            assertEquals(1700000500000L, rsPdf.getLong("last_opened_at"))
            assertEquals(1690000100000L, rsPdf.getLong("added_at"))
            rsPdf.close()

            // m10_catalog_3
            val rsCat = stmt.executeQuery("SELECT id, title, is_in_inbox, inbox_added_at, is_pinned, is_archived, reading_status, last_opened_at, added_at FROM catalog_books WHERE id = 'm10_catalog_3'")
            assertTrue(rsCat.next())
            assertEquals("m10_catalog_3", rsCat.getString("id"))
            assertEquals(0, rsCat.getInt("is_in_inbox")) // MUST BE OUT OF INBOX
            assertEquals("UNREAD", rsCat.getString("reading_status")) // default UNREAD
            assertEquals(1670000000000L, rsCat.getLong("added_at")) // backfilled from updated_at
            org.junit.Assert.assertNull(rsCat.getObject("last_opened_at"))
            rsCat.close()
        }

        // 5. Verify all pre-existing relations & annotations are 100% preserved
        conn.createStatement().use { stmt ->
            // Reading progress preserved
            val rsProgress = stmt.executeQuery("SELECT progression FROM reading_progress WHERE book_id = 'm10_epub_1'")
            assertTrue(rsProgress.next())
            assertEquals(0.45f, rsProgress.getFloat("progression"), 0.001f)
            rsProgress.close()

            // Favorite preserved
            val rsFav = stmt.executeQuery("SELECT count(*) FROM favorite_books WHERE book_id = 'm10_epub_1'")
            assertTrue(rsFav.next())
            assertEquals(1, rsFav.getInt(1))
            rsFav.close()

            // Collection relation preserved
            val rsCol = stmt.executeQuery("SELECT count(*) FROM book_collection_cross_ref WHERE book_id = 'm10_epub_1' AND collection_id = 'col_engineering'")
            assertTrue(rsCol.next())
            assertEquals(1, rsCol.getInt(1))
            rsCol.close()

            // Bookmarks preserved
            val rsBm = stmt.executeQuery("SELECT title FROM bookmarks WHERE book_id = 'm10_epub_1'")
            assertTrue(rsBm.next())
            assertEquals("Chapter 3 Bookmark", rsBm.getString("title"))
            rsBm.close()

            // Highlights preserved
            val rsHl = stmt.executeQuery("SELECT text_snippet, note FROM highlights WHERE book_id = 'm10_epub_1'")
            assertTrue(rsHl.next())
            assertEquals("SOLID principles", rsHl.getString("text_snippet"))
            assertEquals("Important rule", rsHl.getString("note"))
            rsHl.close()

            // Per-book preferences preserved
            val rsPref = stmt.executeQuery("SELECT font_family, font_size_multiplier FROM per_book_preferences WHERE book_id = 'm10_epub_1'")
            assertTrue(rsPref.next())
            assertEquals("Literata", rsPref.getString("font_family"))
            assertEquals(1.2f, rsPref.getFloat("font_size_multiplier"), 0.001f)
            rsPref.close()
        }

        // 6. Simulate a NEW M11 import via normal application repository logic
        val now = System.currentTimeMillis()
        conn.createStatement().use { stmt ->
            stmt.execute("""
                INSERT INTO catalog_books (
                    id, title, author, description, cover_url, category_id, file_url,
                    file_size_bytes, content_version, content_hash, is_featured, is_new,
                    is_premium, rating, published_date, updated_at, format, media_type,
                    source_type, source_url, is_in_inbox, inbox_added_at, is_pinned,
                    is_archived, reading_status, user_title_override, user_author_override,
                    custom_cover_path, last_opened_at, added_at, original_filename
                ) VALUES (
                    'm11_new_import', 'Kotlin Coroutines in Action', 'JetBrains', 'Coroutines guide',
                    '', 'imported', '/files/coroutines.epub', 204800, 1, 'hash_m11',
                    0, 0, 0, 5.0, '2026', $now, 'EPUB', 'application/epub+zip',
                    'LOCAL_FILE', NULL, 1, $now, 0, 0, 'UNREAD', NULL, NULL, NULL, NULL, $now, 'coroutines.epub'
                )
            """.trimIndent())

            val rsNew = stmt.executeQuery("SELECT is_in_inbox, inbox_added_at, reading_status FROM catalog_books WHERE id = 'm11_new_import'")
            assertTrue(rsNew.next())
            assertEquals(1, rsNew.getInt("is_in_inbox")) // NEW document enters Inbox
            assertEquals(now, rsNew.getLong("inbox_added_at"))
            assertEquals("UNREAD", rsNew.getString("reading_status"))
            rsNew.close()

            // Verify pre-existing documents STILL remain OUT of Inbox
            val rsOld = stmt.executeQuery("SELECT count(*) FROM catalog_books WHERE is_in_inbox = 1")
            assertTrue(rsOld.next())
            assertEquals(1, rsOld.getInt(1)) // ONLY the new document is in Inbox!
            rsOld.close()
        }

        conn.close()
    }

    @Test
    fun `test MIGRATION_4_5 executes correct create table and index statements`() {
        val executedSqls = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSqls.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        assertEquals(4, AppDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AppDatabase.MIGRATION_4_5.endVersion)

        AppDatabase.MIGRATION_4_5.migrate(dbProxy)

        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS review_items") })
        assertTrue(executedSqls.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_review_items_annotation_id") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_review_items_book_id") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_review_items_next_review_at") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_review_items_is_enabled") })

        assertTrue(executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS reading_sessions") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_reading_sessions_book_id") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_reading_sessions_started_at") })
    }

    @Test
    fun `test real SQLite v4 to v5 migration preserves existing data and enables review and session tables`() {
        val conn = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")

        // 1. Create full v4 schema in SQLite
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE catalog_books (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    author TEXT NOT NULL,
                    description TEXT NOT NULL,
                    cover_url TEXT NOT NULL,
                    category_id TEXT NOT NULL,
                    file_url TEXT NOT NULL,
                    file_size_bytes INTEGER NOT NULL,
                    content_version INTEGER NOT NULL,
                    content_hash TEXT,
                    is_featured INTEGER NOT NULL,
                    is_new INTEGER NOT NULL,
                    is_premium INTEGER NOT NULL,
                    rating REAL NOT NULL,
                    published_date TEXT,
                    updated_at INTEGER NOT NULL,
                    format TEXT NOT NULL DEFAULT 'EPUB',
                    media_type TEXT NOT NULL DEFAULT 'application/epub+zip',
                    source_type TEXT NOT NULL DEFAULT 'LOCAL_FILE',
                    source_url TEXT DEFAULT NULL,
                    is_in_inbox INTEGER NOT NULL DEFAULT 0,
                    inbox_added_at INTEGER DEFAULT NULL,
                    is_pinned INTEGER NOT NULL DEFAULT 0,
                    is_archived INTEGER NOT NULL DEFAULT 0,
                    reading_status TEXT NOT NULL DEFAULT 'UNREAD',
                    user_title_override TEXT DEFAULT NULL,
                    user_author_override TEXT DEFAULT NULL,
                    custom_cover_path TEXT DEFAULT NULL,
                    last_opened_at INTEGER DEFAULT NULL,
                    added_at INTEGER NOT NULL DEFAULT 0,
                    original_filename TEXT DEFAULT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE highlights (
                    id TEXT NOT NULL PRIMARY KEY,
                    book_id TEXT NOT NULL,
                    locator_json TEXT NOT NULL,
                    text TEXT NOT NULL,
                    color TEXT NOT NULL,
                    note TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())

            // 2. Insert sample data into v4 tables
            val now = System.currentTimeMillis()
            stmt.execute("""
                INSERT INTO catalog_books (
                    id, title, author, description, cover_url, category_id, file_url,
                    file_size_bytes, content_version, content_hash, is_featured, is_new,
                    is_premium, rating, published_date, updated_at, format, media_type,
                    source_type, source_url, is_in_inbox, inbox_added_at, is_pinned,
                    is_archived, reading_status, user_title_override, user_author_override,
                    custom_cover_path, last_opened_at, added_at, original_filename
                ) VALUES (
                    'book_m12', 'Clean Architecture', 'Robert C. Martin', 'Design handbook',
                    '', 'c1', '/files/clean_arch.pdf', 1048576, 1, 'hash123',
                    1, 0, 0, 4.8, '2023', $now, 'PDF', 'application/pdf',
                    'LOCAL_FILE', NULL, 0, NULL, 1, 0, 'READING', 'Clean Arch Override', NULL,
                    NULL, $now, $now, 'clean_arch.pdf'
                )
            """.trimIndent())

            stmt.execute("""
                INSERT INTO highlights (id, book_id, locator_json, text, color, note, created_at, updated_at)
                VALUES ('hl_1', 'book_m12', '{"href":"page_1"}', 'Dependencies must point inward', 'YELLOW', 'Key takeaway', $now, $now)
            """.trimIndent())
        }

        // 3. Migrate v4 -> v5
        val supportDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                conn.createStatement().use { s -> s.execute(args[0] as String) }
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_4_5.migrate(supportDb)

        // 4. Verify existing data preserved
        conn.createStatement().use { stmt ->
            val rsBook = stmt.executeQuery("SELECT title, user_title_override, is_pinned FROM catalog_books WHERE id = 'book_m12'")
            assertTrue(rsBook.next())
            assertEquals("Clean Architecture", rsBook.getString("title"))
            assertEquals("Clean Arch Override", rsBook.getString("user_title_override"))
            assertEquals(1, rsBook.getInt("is_pinned"))
            rsBook.close()

            val rsHl = stmt.executeQuery("SELECT text, note FROM highlights WHERE id = 'hl_1'")
            assertTrue(rsHl.next())
            assertEquals("Dependencies must point inward", rsHl.getString("text"))
            assertEquals("Key takeaway", rsHl.getString("note"))
            rsHl.close()

            // 5. Test review_items table in v5
            val now = System.currentTimeMillis()
            stmt.execute("""
                INSERT INTO review_items (
                    id, annotation_id, book_id, is_enabled, next_review_at,
                    last_reviewed_at, review_count, interval_days, ease_factor, created_at, updated_at
                ) VALUES (
                    'rev_1', 'hl_1', 'book_m12', 1, $now, NULL, 0, 1, 2.5, $now, $now
                )
            """.trimIndent())

            val rsRev = stmt.executeQuery("SELECT annotation_id, interval_days, ease_factor FROM review_items WHERE id = 'rev_1'")
            assertTrue(rsRev.next())
            assertEquals("hl_1", rsRev.getString("annotation_id"))
            assertEquals(1, rsRev.getInt("interval_days"))
            assertEquals(2.5, rsRev.getDouble("ease_factor"), 0.001)
            rsRev.close()

            // 6. Test reading_sessions table in v5
            stmt.execute("""
                INSERT INTO reading_sessions (
                    id, book_id, started_at, ended_at, duration_ms, start_progress, end_progress, format
                ) VALUES (
                    'sess_1', 'book_m12', $now, ${now + 60000}, 60000, 0.1, 0.25, 'PDF'
                )
            """.trimIndent())

            val rsSess = stmt.executeQuery("SELECT duration_ms, format FROM reading_sessions WHERE id = 'sess_1'")
            assertTrue(rsSess.next())
            assertEquals(60000, rsSess.getLong("duration_ms"))
            assertEquals("PDF", rsSess.getString("format"))
            rsSess.close()
        }

        conn.close()
    }
}

