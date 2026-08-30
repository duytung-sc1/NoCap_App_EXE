package com.ebookreader.app.domain.model

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadedBook(
    val bookId: String,
    val localFilePath: String,
    val downloadStatus: DownloadStatus,
    val downloadProgress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val downloadedContentVersion: Long = 1L,
    val contentHash: String? = null,
    val downloadedAt: Long? = null,
    val lastError: String? = null
)
