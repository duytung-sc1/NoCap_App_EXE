package com.ebookreader.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ebookreader.app.core.database.entity.CatalogBookEntity
import com.ebookreader.app.core.database.entity.CategoryEntity
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
}
