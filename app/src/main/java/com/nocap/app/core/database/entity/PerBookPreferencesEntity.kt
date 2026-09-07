package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "per_book_preferences",
    foreignKeys = [
        ForeignKey(
            entity = CatalogBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PerBookPreferencesEntity(
    @PrimaryKey
    @ColumnInfo(name = "book_id")
    val bookId: String,
    val theme: String? = null,
    @ColumnInfo(name = "font_family")
    val fontFamily: String? = null,
    @ColumnInfo(name = "font_size")
    val fontSize: Float? = null,
    @ColumnInfo(name = "line_height")
    val lineHeight: Float? = null,
    @ColumnInfo(name = "text_alignment")
    val textAlignment: String? = null,
    @ColumnInfo(name = "scroll_mode")
    val scrollMode: Boolean? = null,
    @ColumnInfo(name = "use_book_override", defaultValue = "0")
    val useBookOverride: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
