package com.nocap.app.domain.model

data class ReadingProgress(
    val bookId: String,
    val locatorJson: String,
    val progression: Float,
    val chapterTitle: String?,
    val lastReadAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 1L
)
