package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.CategoryEntity
import com.nocap.app.domain.model.DocumentReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories ORDER BY display_order ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM catalog_books")
    fun observeAllBooks(): Flow<List<CatalogBookEntity>>

    @Query("SELECT * FROM catalog_books WHERE is_featured = 1")
    fun observeFeaturedBooks(): Flow<List<CatalogBookEntity>>

    @Query("SELECT * FROM catalog_books WHERE is_new = 1")
    fun observeNewBooks(): Flow<List<CatalogBookEntity>>

    @Query("SELECT * FROM catalog_books WHERE category_id = :categoryId")
    fun observeBooksByCategory(categoryId: String): Flow<List<CatalogBookEntity>>

    @Query("SELECT * FROM catalog_books WHERE id = :bookId")
    fun observeBookById(bookId: String): Flow<CatalogBookEntity?>

    @Query("SELECT * FROM catalog_books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): CatalogBookEntity?

    @Query("SELECT * FROM catalog_books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<CatalogBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<CatalogBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: CatalogBookEntity)

    @Query("DELETE FROM catalog_books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("SELECT * FROM catalog_books WHERE content_hash = :hash LIMIT 1")
    suspend fun getBookByHash(hash: String): CatalogBookEntity?

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Query("UPDATE catalog_books SET is_in_inbox = :isInInbox, inbox_added_at = CASE WHEN :isInInbox = 1 THEN :inboxAddedAt ELSE inbox_added_at END WHERE id = :bookId")
    suspend fun updateInboxState(bookId: String, isInInbox: Boolean, inboxAddedAt: Long? = System.currentTimeMillis())

    @Query("UPDATE catalog_books SET is_pinned = :isPinned WHERE id = :bookId")
    suspend fun updatePinnedState(bookId: String, isPinned: Boolean)

    @Query("UPDATE catalog_books SET is_archived = :isArchived, is_in_inbox = CASE WHEN :isArchived = 1 THEN 0 ELSE is_in_inbox END WHERE id = :bookId")
    suspend fun updateArchivedState(bookId: String, isArchived: Boolean)

    @Query("UPDATE catalog_books SET reading_status = :status WHERE id = :bookId")
    suspend fun updateReadingStatus(bookId: String, status: DocumentReadingStatus)

    @Query("UPDATE catalog_books SET user_title_override = :titleOverride, user_author_override = :authorOverride WHERE id = :bookId")
    suspend fun updateMetadataOverrides(bookId: String, titleOverride: String?, authorOverride: String?)

    @Query("UPDATE catalog_books SET custom_cover_path = :coverPath WHERE id = :bookId")
    suspend fun updateCustomCover(bookId: String, coverPath: String?)

    @Query("UPDATE catalog_books SET last_opened_at = :timestamp WHERE id = :bookId")
    suspend fun updateLastOpenedAt(bookId: String, timestamp: Long)

    @Query("UPDATE catalog_books SET is_archived = :isArchived, is_in_inbox = CASE WHEN :isArchived = 1 THEN 0 ELSE is_in_inbox END WHERE id IN (:bookIds)")
    suspend fun updateBulkArchive(bookIds: List<String>, isArchived: Boolean)

    @Query("UPDATE catalog_books SET is_pinned = :isPinned WHERE id IN (:bookIds)")
    suspend fun updateBulkPin(bookIds: List<String>, isPinned: Boolean)

    @Query("UPDATE catalog_books SET reading_status = :status WHERE id IN (:bookIds)")
    suspend fun updateBulkReadingStatus(bookIds: List<String>, status: DocumentReadingStatus)
}
