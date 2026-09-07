package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.EntitlementType
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSourceType

@Entity(
    tableName = "catalog_books",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["is_featured"]),
        Index(value = ["is_new"]),
        Index(value = ["title"]),
        Index(value = ["is_in_inbox"]),
        Index(value = ["is_pinned"]),
        Index(value = ["is_archived"]),
        Index(value = ["reading_status"])
    ]
)
data class CatalogBookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    @ColumnInfo(name = "cover_url")
    val coverUrl: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "file_url")
    val fileUrl: String,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,
    @ColumnInfo(name = "content_version", defaultValue = "1")
    val contentVersion: Long = 1L,
    @ColumnInfo(name = "content_hash")
    val contentHash: String? = null,
    @ColumnInfo(name = "is_featured")
    val isFeatured: Boolean = false,
    @ColumnInfo(name = "is_new")
    val isNew: Boolean = false,
    @ColumnInfo(name = "is_premium")
    val isPremium: Boolean = false,
    @ColumnInfo(name = "play_product_id")
    val playProductId: String? = null,
    @ColumnInfo(name = "entitlement_type")
    val entitlementType: EntitlementType = EntitlementType.FREE,
    val rating: Float = 0f,
    @ColumnInfo(name = "published_date")
    val publishedDate: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "format", defaultValue = "EPUB")
    val format: PublicationFormat = PublicationFormat.EPUB,
    @ColumnInfo(name = "media_type", defaultValue = "application/epub+zip")
    val mediaType: String = "application/epub+zip",
    @ColumnInfo(name = "source_type", defaultValue = "LOCAL_FILE")
    val sourceType: PublicationSourceType = PublicationSourceType.LOCAL_FILE,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,
    @ColumnInfo(name = "is_in_inbox", defaultValue = "0")
    val isInInbox: Boolean = false,
    @ColumnInfo(name = "inbox_added_at")
    val inboxAddedAt: Long? = null,
    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "reading_status", defaultValue = "UNREAD")
    val readingStatus: DocumentReadingStatus = DocumentReadingStatus.UNREAD,
    @ColumnInfo(name = "user_title_override")
    val userTitleOverride: String? = null,
    @ColumnInfo(name = "user_author_override")
    val userAuthorOverride: String? = null,
    @ColumnInfo(name = "custom_cover_path")
    val customCoverPath: String? = null,
    @ColumnInfo(name = "last_opened_at")
    val lastOpenedAt: Long? = null,
    @ColumnInfo(name = "added_at", defaultValue = "0")
    val addedAt: Long = 0L,
    @ColumnInfo(name = "original_filename")
    val originalFilename: String? = null
)
