package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nocap.app.domain.model.EntitlementType

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
        Index(value = ["title"])
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
    val updatedAt: Long = System.currentTimeMillis()
)
