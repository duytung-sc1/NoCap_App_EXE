package com.nocap.app.domain.model

data class Tag(
    val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Long = System.currentTimeMillis()
)
