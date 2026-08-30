package com.ebookreader.app.domain.model

data class CatalogBook(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverUrl: String,
    val categoryId: String,
    val fileUrl: String,
    val fileSizeBytes: Long,
    val contentVersion: Long = 1L,
    val contentHash: String? = null,
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val isPremium: Boolean = false,
    val playProductId: String? = null,
    val entitlementType: EntitlementType = EntitlementType.FREE,
    val rating: Float = 0f,
    val publishedDate: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
