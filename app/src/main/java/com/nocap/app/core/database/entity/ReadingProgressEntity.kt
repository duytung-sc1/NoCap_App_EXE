package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = CatalogBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadingProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "locator_json")
    val locatorJson: String,
    val progression: Float = 0f,
    @ColumnInfo(name = "chapter_title")
    val chapterTitle: String? = null,
    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_version", defaultValue = "1")
    val syncVersion: Long = 1L
)
