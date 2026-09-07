package com.nocap.app.domain.model

data class CatalogBook(
    val id: String,
    val title: String,
    val author: String,
    val description: String = "",
    val coverUrl: String,
    val categoryId: String,
    val fileUrl: String,
    val fileSizeBytes: Long = 0L,
    val contentVersion: Long = 1L,
    val contentHash: String? = null,
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val isPremium: Boolean = false,
    val playProductId: String? = null,
    val entitlementType: EntitlementType = EntitlementType.FREE,
    val rating: Float = 0f,
    val publishedDate: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val format: PublicationFormat = PublicationFormat.EPUB,
    val mediaType: String = PublicationFormat.EPUB.mediaType,
    val sourceType: PublicationSourceType = PublicationSourceType.LOCAL_FILE,
    val sourceUrl: String? = null,
    val isInInbox: Boolean = false,
    val inboxAddedAt: Long? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val readingStatus: DocumentReadingStatus = DocumentReadingStatus.UNREAD,
    val userTitleOverride: String? = null,
    val userAuthorOverride: String? = null,
    val customCoverPath: String? = null,
    val lastOpenedAt: Long? = null,
    val addedAt: Long = 0L,
    val originalFilename: String? = null
) {
    val displayTitle: String
        get() = userTitleOverride?.takeIf { it.isNotBlank() } ?: title

    val displayAuthor: String
        get() = userAuthorOverride?.takeIf { it.isNotBlank() } ?: author
}
