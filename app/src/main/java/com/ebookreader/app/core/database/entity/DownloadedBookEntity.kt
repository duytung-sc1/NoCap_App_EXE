package com.ebookreader.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ebookreader.app.domain.model.DownloadStatus

@Entity(
    tableName = "downloaded_books",
    foreignKeys = [
        ForeignKey(
            entity = CatalogBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DownloadedBookEntity(
    @PrimaryKey
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "local_file_path")
    val localFilePath: String,
    @ColumnInfo(name = "download_status")
    val downloadStatus: DownloadStatus,
    @ColumnInfo(name = "download_progress")
    val downloadProgress: Float = 0f,
    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,
    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long = 0L,
    @ColumnInfo(name = "downloaded_content_version", defaultValue = "1")
    val downloadedContentVersion: Long = 1L,
    @ColumnInfo(name = "content_hash")
    val contentHash: String? = null,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long? = null,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null
)
