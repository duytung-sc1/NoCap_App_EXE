package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "highlight_note_versions",
    foreignKeys = [
        ForeignKey(
            entity = HighlightEntity::class,
            parentColumns = ["id"],
            childColumns = ["highlight_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["highlight_id"]),
        Index(value = ["created_at"])
    ]
)
data class HighlightNoteVersionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "highlight_id")
    val highlightId: String,
    @ColumnInfo(name = "note_text")
    val noteText: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
