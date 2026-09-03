package com.nocap.app.domain.model

data class Bookmark(
    val id: String,
    val bookId: String,
    val locatorJson: String,
    val chapterTitle: String,
    val snippet: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 1L,
    val isDeleted: Boolean = false
)
