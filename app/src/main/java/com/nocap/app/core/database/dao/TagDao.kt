package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nocap.app.core.database.entity.BookTagCrossRef
import com.nocap.app.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

data class TagWithBookCount(
    val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Long,
    val bookCount: Int
)

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("""
        SELECT t.id, t.name, t.normalized_name AS normalizedName, t.created_at AS createdAt,
               COUNT(b.book_id) AS bookCount
        FROM tags t
        LEFT JOIN book_tag_cross_ref b ON t.id = b.tag_id
        GROUP BY t.id
        ORDER BY t.name COLLATE NOCASE ASC
    """)
    fun observeTagsWithCount(): Flow<List<TagWithBookCount>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: String): TagEntity?

    @Query("SELECT * FROM tags WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun getTagByNormalizedName(normalizedName: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToBook(crossRef: BookTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagsToBooks(crossRefs: List<BookTagCrossRef>)

    @Query("DELETE FROM book_tag_cross_ref WHERE book_id = :bookId AND tag_id = :tagId")
    suspend fun removeTagFromBook(bookId: String, tagId: String)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN book_tag_cross_ref b ON t.id = b.tag_id
        WHERE b.book_id = :bookId
        ORDER BY t.name COLLATE NOCASE ASC
    """)
    fun observeTagsForBook(bookId: String): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN book_tag_cross_ref b ON t.id = b.tag_id
        WHERE b.book_id = :bookId
        ORDER BY t.name COLLATE NOCASE ASC
    """)
    suspend fun getTagsForBook(bookId: String): List<TagEntity>

    @Query("SELECT * FROM book_tag_cross_ref")
    fun observeAllTagCrossRefs(): Flow<List<BookTagCrossRef>>

    @Query("DELETE FROM book_tag_cross_ref WHERE book_id = :bookId")
    suspend fun deleteTagsForBook(bookId: String)

    @Transaction
    suspend fun setTagsForBook(bookId: String, tagIds: List<String>) {
        deleteTagsForBook(bookId)
        val crossRefs = tagIds.map { BookTagCrossRef(bookId = bookId, tagId = it) }
        addTagsToBooks(crossRefs)
    }
}
