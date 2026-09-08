package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nocap.app.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId AND is_deleted = 0 ORDER BY created_at DESC")
    fun observeBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getBookmarksForBook(bookId: String): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks SET is_deleted = 1 WHERE id = :bookmarkId")
    suspend fun softDeleteBookmark(bookmarkId: String)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun hardDeleteBookmark(bookmarkId: String)

    @Query("DELETE FROM bookmarks WHERE book_id = :bookId")
    suspend fun deleteBookmarksByBookId(bookId: String)

    @Query("SELECT * FROM bookmarks WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getAllBookmarks(): List<BookmarkEntity>
}
