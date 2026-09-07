package com.nocap.app.domain.model

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
    val downloadStatus: DownloadStatus = DownloadStatus.COMPLETED,
    val downloadProgress: Float = 1f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedContentVersion: Long = 1L,
    val contentHash: String? = null,
    val downloadedAt: Long? = null,
    val lastError: String? = null
)
