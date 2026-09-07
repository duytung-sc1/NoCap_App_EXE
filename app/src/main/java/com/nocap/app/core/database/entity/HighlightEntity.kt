package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = CatalogBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["created_at"])
    ]
)
data class HighlightEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "locator_json")
    val locatorJson: String,
    val text: String,
    val color: String, // YELLOW, GREEN, BLUE, PINK
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
