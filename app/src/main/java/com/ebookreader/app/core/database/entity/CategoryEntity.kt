package com.ebookreader.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0
)
