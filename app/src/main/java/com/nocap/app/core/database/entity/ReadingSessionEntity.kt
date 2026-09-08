package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_sessions",
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
        Index(value = ["started_at"])
    ]
)
data class ReadingSessionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "ended_at")
    val endedAt: Long? = null,
    @ColumnInfo(name = "duration_ms", defaultValue = "0")
    val durationMs: Long = 0L,
    @ColumnInfo(name = "start_progress", defaultValue = "0.0")
    val startProgress: Float = 0f,
    @ColumnInfo(name = "end_progress", defaultValue = "0.0")
    val endProgress: Float = 0f,
    @ColumnInfo(name = "format")
    val format: String
)
