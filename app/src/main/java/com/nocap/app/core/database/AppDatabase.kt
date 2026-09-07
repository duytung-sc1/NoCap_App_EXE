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
import com.nocap.app.core.database.entity.BookCollectionCrossRef
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
        CustomFontEntity::class
    ],
    version = 3,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ebook_reader.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}
