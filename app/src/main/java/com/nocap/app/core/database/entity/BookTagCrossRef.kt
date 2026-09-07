package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_tag_cross_ref",
    primaryKeys = ["book_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = CatalogBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["tag_id"])
    ]
)
data class BookTagCrossRef(
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
