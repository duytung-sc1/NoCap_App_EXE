package com.nocap.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nocap.app.core.database.dao.BookmarkDao
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.CollectionDao
import com.nocap.app.core.database.dao.CustomFontDao
import com.nocap.app.core.database.dao.DownloadDao
import com.nocap.app.core.database.dao.FavoriteDao
import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.database.dao.PerBookPreferencesDao
import com.nocap.app.core.database.dao.ProgressDao
import com.nocap.app.core.database.dao.TagDao
import com.nocap.app.core.database.entity.BookCollectionCrossRef
import com.nocap.app.core.database.entity.BookTagCrossRef
import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.CategoryEntity
import com.nocap.app.core.database.entity.CollectionEntity
import com.nocap.app.core.database.entity.CustomFontEntity
import com.nocap.app.core.database.entity.DownloadedBookEntity
import com.nocap.app.core.database.entity.FavoriteEntity
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.core.database.entity.PerBookPreferencesEntity
import com.nocap.app.core.database.entity.ReadingProgressEntity
import com.nocap.app.core.database.dao.ReadingSessionDao
import com.nocap.app.core.database.dao.ReviewDao
import com.nocap.app.core.database.entity.ReadingSessionEntity
import com.nocap.app.core.database.entity.ReviewItemEntity
import com.nocap.app.core.database.entity.TagEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        CatalogBookEntity::class,
        DownloadedBookEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        FavoriteEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class,
        HighlightEntity::class,
        PerBookPreferencesEntity::class,
        CustomFontEntity::class,
        TagEntity::class,
        BookTagCrossRef::class,
        ReviewItemEntity::class,
        ReadingSessionEntity::class,
        com.nocap.app.core.database.entity.HighlightNoteVersionEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun downloadDao(): DownloadDao
    abstract fun progressDao(): ProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun collectionDao(): CollectionDao
    abstract fun highlightDao(): HighlightDao
    abstract fun perBookPreferencesDao(): PerBookPreferencesDao
    abstract fun customFontDao(): CustomFontDao
    abstract fun tagDao(): TagDao
    abstract fun reviewDao(): ReviewDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun highlightNoteVersionDao(): com.nocap.app.core.database.dao.HighlightNoteVersionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val profileInstances = mutableMapOf<String, AppDatabase>()

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `highlight_note_versions` (
                        `id` TEXT NOT NULL,
                        `highlight_id` TEXT NOT NULL,
                        `note_text` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`highlight_id`) REFERENCES `highlights`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlight_note_versions_highlight_id` ON `highlight_note_versions` (`highlight_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlight_note_versions_created_at` ON `highlight_note_versions` (`created_at`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                com.nocap.app.data.sync.SyncSchema.install { db.execSQL(it) }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN format TEXT NOT NULL DEFAULT 'EPUB'")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN media_type TEXT NOT NULL DEFAULT 'application/epub+zip'")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN source_type TEXT NOT NULL DEFAULT 'LOCAL_FILE'")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN source_url TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS collections (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sort_order INTEGER NOT NULL DEFAULT 0,
                        sort_type TEXT NOT NULL DEFAULT 'RECENTLY_ADDED'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_collection_cross_ref (
                        book_id TEXT NOT NULL,
                        collection_id TEXT NOT NULL,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY(book_id, collection_id),
                        FOREIGN KEY(book_id) REFERENCES catalog_books(id) ON DELETE CASCADE,
                        FOREIGN KEY(collection_id) REFERENCES collections(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_collection_cross_ref_book_id ON book_collection_cross_ref(book_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_collection_cross_ref_collection_id ON book_collection_cross_ref(collection_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS highlights (
                        id TEXT NOT NULL PRIMARY KEY,
                        book_id TEXT NOT NULL,
                        locator_json TEXT NOT NULL,
                        text TEXT NOT NULL,
                        color TEXT NOT NULL,
                        note TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(book_id) REFERENCES catalog_books(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_highlights_book_id ON highlights(book_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_highlights_created_at ON highlights(created_at)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS per_book_preferences (
                        book_id TEXT NOT NULL PRIMARY KEY,
                        theme TEXT,
                        font_family TEXT,
                        font_size REAL,
                        line_height REAL,
                        text_alignment TEXT,
                        scroll_mode INTEGER,
                        use_book_override INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(book_id) REFERENCES catalog_books(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_fonts (
                        id TEXT NOT NULL PRIMARY KEY,
                        font_family TEXT NOT NULL,
                        file_name TEXT NOT NULL,
                        file_size INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to catalog_books
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN is_in_inbox INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN inbox_added_at INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN reading_status TEXT NOT NULL DEFAULT 'UNREAD'")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN user_title_override TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN user_author_override TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN custom_cover_path TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN last_opened_at INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN added_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_books ADD COLUMN original_filename TEXT DEFAULT NULL")

                // 2. Backfill added_at from downloaded_books or updated_at
                db.execSQL("""
                    UPDATE catalog_books 
                    SET added_at = COALESCE(
                        (SELECT downloaded_at FROM downloaded_books WHERE downloaded_books.book_id = catalog_books.id),
                        updated_at
                    )
                """.trimIndent())
                db.execSQL("UPDATE catalog_books SET added_at = updated_at WHERE added_at = 0")

                // 3. Backfill last_opened_at from reading_progress
                db.execSQL("""
                    UPDATE catalog_books 
                    SET last_opened_at = (SELECT last_read_at FROM reading_progress WHERE reading_progress.book_id = catalog_books.id)
                """.trimIndent())

                // 4. Backfill reading_status based on reading_progress
                db.execSQL("""
                    UPDATE catalog_books 
                    SET reading_status = 'COMPLETED' 
                    WHERE id IN (SELECT book_id FROM reading_progress WHERE progression >= 0.98)
                """.trimIndent())
                db.execSQL("""
                    UPDATE catalog_books 
                    SET reading_status = 'READING' 
                    WHERE id IN (SELECT book_id FROM reading_progress WHERE progression > 0 AND progression < 0.98)
                """.trimIndent())

                // 5. Indices for catalog_books new columns
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_books_is_in_inbox ON catalog_books(is_in_inbox)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_books_is_pinned ON catalog_books(is_pinned)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_books_is_archived ON catalog_books(is_archived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_books_reading_status ON catalog_books(reading_status)")

                // 6. Create tags table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        normalized_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_normalized_name ON tags(normalized_name)")

                // 7. Create book_tag_cross_ref table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_tag_cross_ref (
                        book_id TEXT NOT NULL,
                        tag_id TEXT NOT NULL,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY(book_id, tag_id),
                        FOREIGN KEY(book_id) REFERENCES catalog_books(id) ON DELETE CASCADE,
                        FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_book_id ON book_tag_cross_ref(book_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_tag_id ON book_tag_cross_ref(tag_id)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create review_items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS review_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        annotation_id TEXT NOT NULL,
                        book_id TEXT NOT NULL,
                        is_enabled INTEGER NOT NULL DEFAULT 1,
                        next_review_at INTEGER NOT NULL,
                        last_reviewed_at INTEGER DEFAULT NULL,
                        review_count INTEGER NOT NULL DEFAULT 0,
                        interval_days INTEGER NOT NULL DEFAULT 1,
                        ease_factor REAL NOT NULL DEFAULT 2.5,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(annotation_id) REFERENCES highlights(id) ON DELETE CASCADE,
                        FOREIGN KEY(book_id) REFERENCES catalog_books(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_review_items_annotation_id ON review_items(annotation_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_items_book_id ON review_items(book_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_items_next_review_at ON review_items(next_review_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_items_is_enabled ON review_items(is_enabled)")

                // 2. Create reading_sessions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        book_id TEXT NOT NULL,
                        started_at INTEGER NOT NULL,
                        ended_at INTEGER DEFAULT NULL,
                        duration_ms INTEGER NOT NULL DEFAULT 0,
                        start_progress REAL NOT NULL DEFAULT 0.0,
                        end_progress REAL NOT NULL DEFAULT 0.0,
                        format TEXT NOT NULL,
                        FOREIGN KEY(book_id) REFERENCES catalog_books(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_book_id ON reading_sessions(book_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_started_at ON reading_sessions(started_at)")
            }
        }

        fun getInstance(context: Context, profile: String = com.nocap.app.data.sync.Profiles.active.value): AppDatabase {
            return synchronized(this) {
                profileInstances.getOrPut(profile) { Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    com.nocap.app.data.sync.Profiles.databaseName(profile)
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            com.nocap.app.data.sync.SyncSchema.install { db.execSQL(it) }
                            db.execSQL("UPDATE sync_control SET profile=? WHERE id=1", arrayOf(profile))
                        }
                    }).build() }
            }
        }
    }
}
