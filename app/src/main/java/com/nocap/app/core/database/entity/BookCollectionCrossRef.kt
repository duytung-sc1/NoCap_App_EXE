package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_collection_cross_ref",
    primaryKeys = ["book_id", "collection_id"],
    foreignKeys = [
        ForeignKey(
            entity = CatalogBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["collection_id"])
    ]
)
data class BookCollectionCrossRef(
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
