package com.ebookreader.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ebookreader.app.core.database.dao.BookmarkDao
import com.ebookreader.app.core.database.dao.CatalogDao
import com.ebookreader.app.core.database.dao.DownloadDao
import com.ebookreader.app.core.database.dao.FavoriteDao
import com.ebookreader.app.core.database.dao.ProgressDao
import com.ebookreader.app.core.database.entity.BookmarkEntity
import com.ebookreader.app.core.database.entity.CatalogBookEntity
import com.ebookreader.app.core.database.entity.CategoryEntity
import com.ebookreader.app.core.database.entity.DownloadedBookEntity
import com.ebookreader.app.core.database.entity.FavoriteEntity
import com.ebookreader.app.core.database.entity.ReadingProgressEntity

@Database(
    entities = [
        CategoryEntity::class,
        CatalogBookEntity::class,
        DownloadedBookEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun downloadDao(): DownloadDao
    abstract fun progressDao(): ProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ebook_reader.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
