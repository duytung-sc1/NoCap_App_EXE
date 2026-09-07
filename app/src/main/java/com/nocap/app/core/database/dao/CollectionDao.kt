package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nocap.app.core.database.entity.BookCollectionCrossRef
import com.nocap.app.core.database.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

data class CollectionWithBookCount(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Int,
    val sortType: String,
    val bookCount: Int
)

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY sort_order ASC, name ASC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("""
        SELECT c.id, c.name, c.created_at AS createdAt, c.updated_at AS updatedAt,
               c.sort_order AS sortOrder, c.sort_type AS sortType,
               COUNT(b.book_id) AS bookCount
        FROM collections c
        LEFT JOIN book_collection_cross_ref b ON c.id = b.collection_id
        GROUP BY c.id
        ORDER BY c.sort_order ASC, c.name ASC
    """)
    fun observeCollectionsWithCount(): Flow<List<CollectionWithBookCount>>

    @Query("SELECT * FROM collections WHERE id = :collectionId")
    suspend fun getCollectionById(collectionId: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getCollectionByName(name: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(crossRef: BookCollectionCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBooksToCollection(crossRefs: List<BookCollectionCrossRef>)

    @Query("DELETE FROM book_collection_cross_ref WHERE book_id = :bookId AND collection_id = :collectionId")
    suspend fun removeBookFromCollection(bookId: String, collectionId: String)

    @Query("""
        SELECT c.* FROM collections c
        INNER JOIN book_collection_cross_ref b ON c.id = b.collection_id
        WHERE b.book_id = :bookId
        ORDER BY c.name ASC
    """)
    fun observeCollectionsForBook(bookId: String): Flow<List<CollectionEntity>>

    @Query("""
        SELECT c.* FROM collections c
        INNER JOIN book_collection_cross_ref b ON c.id = b.collection_id
        WHERE b.book_id = :bookId
        ORDER BY c.name ASC
    """)
    suspend fun getCollectionsForBook(bookId: String): List<CollectionEntity>

    @Query("SELECT book_id FROM book_collection_cross_ref WHERE collection_id = :collectionId ORDER BY added_at DESC")
    fun observeBookIdsInCollection(collectionId: String): Flow<List<String>>

    @Query("SELECT book_id FROM book_collection_cross_ref WHERE collection_id = :collectionId")
    suspend fun getBookIdsInCollection(collectionId: String): List<String>

    @Query("SELECT * FROM book_collection_cross_ref")
    fun observeAllCollectionCrossRefs(): Flow<List<BookCollectionCrossRef>>
}
