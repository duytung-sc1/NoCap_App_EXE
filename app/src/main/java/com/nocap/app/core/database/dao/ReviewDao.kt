package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.core.database.entity.ReviewItemEntity
import kotlinx.coroutines.flow.Flow

data class ReviewItemWithDetails(
    @Embedded val reviewItem: ReviewItemEntity,
    @Relation(
        parentColumn = "annotation_id",
        entityColumn = "id"
    )
    val highlight: HighlightEntity?,
    @Relation(
        parentColumn = "book_id",
        entityColumn = "id"
    )
    val book: CatalogBookEntity?
)

@Dao
interface ReviewDao {

    @Query("SELECT * FROM review_items WHERE next_review_at <= :cutoffTime AND is_enabled = 1 ORDER BY next_review_at ASC")
    fun observeDueItems(cutoffTime: Long): Flow<List<ReviewItemEntity>>

    @Transaction
    @Query("SELECT * FROM review_items WHERE next_review_at <= :cutoffTime AND is_enabled = 1 ORDER BY next_review_at ASC")
    fun observeDueItemsWithDetails(cutoffTime: Long): Flow<List<ReviewItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM review_items WHERE next_review_at <= :cutoffTime AND is_enabled = 1 ORDER BY next_review_at ASC")
    suspend fun getDueItemsWithDetails(cutoffTime: Long): List<ReviewItemWithDetails>

    @Query("SELECT COUNT(*) FROM review_items WHERE next_review_at <= :cutoffTime AND is_enabled = 1")
    fun observeDueItemsCount(cutoffTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_items WHERE next_review_at <= :cutoffTime AND is_enabled = 1")
    suspend fun getDueItemsCount(cutoffTime: Long): Int

    @Query("SELECT * FROM review_items WHERE annotation_id = :annotationId LIMIT 1")
    suspend fun getReviewItemByAnnotationId(annotationId: String): ReviewItemEntity?

    @Query("SELECT * FROM review_items WHERE annotation_id = :annotationId LIMIT 1")
    fun observeReviewItemByAnnotationId(annotationId: String): Flow<ReviewItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewItem(item: ReviewItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReviewItems(items: List<ReviewItemEntity>)

    @Update
    suspend fun updateReviewItem(item: ReviewItemEntity)

    @Query("DELETE FROM review_items WHERE annotation_id = :annotationId")
    suspend fun deleteByAnnotationId(annotationId: String)

    @Query("DELETE FROM review_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM review_items ORDER BY next_review_at ASC")
    fun observeAllReviewItems(): Flow<List<ReviewItemEntity>>

    @Query("SELECT annotation_id FROM review_items")
    suspend fun getAllReviewAnnotationIds(): List<String>

    @Query("SELECT COUNT(*) FROM review_items WHERE next_review_at <= :now AND is_enabled = 1")
    suspend fun getDueCount(now: Long): Int

    @Query("SELECT COUNT(*) FROM review_items WHERE next_review_at > :now AND is_enabled = 1 AND interval_days < 21")
    suspend fun getLearningCount(now: Long): Int

    @Query("SELECT COUNT(*) FROM review_items WHERE next_review_at > :now AND is_enabled = 1 AND interval_days >= 21")
    suspend fun getMasteredCount(now: Long): Int
}
