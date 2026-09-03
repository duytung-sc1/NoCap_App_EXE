package com.nocap.app.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val displayOrder: Int = 0
)
