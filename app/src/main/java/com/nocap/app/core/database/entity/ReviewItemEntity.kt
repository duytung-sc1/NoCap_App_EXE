package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_items",
    foreignKeys = [
        ForeignKey(
            entity = HighlightEntity::class,
            parentColumns = ["id"],
            childColumns = ["annotation_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CatalogBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["annotation_id"], unique = true),
        Index(value = ["book_id"]),
        Index(value = ["next_review_at"]),
        Index(value = ["is_enabled"])
    ]
)
data class ReviewItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "annotation_id")
    val annotationId: String,
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "is_enabled", defaultValue = "1")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long,
    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: Long? = null,
    @ColumnInfo(name = "review_count", defaultValue = "0")
    val reviewCount: Int = 0,
    @ColumnInfo(name = "interval_days", defaultValue = "1")
    val intervalDays: Int = 1,
    @ColumnInfo(name = "ease_factor", defaultValue = "2.5")
    val easeFactor: Float = 2.5f,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
