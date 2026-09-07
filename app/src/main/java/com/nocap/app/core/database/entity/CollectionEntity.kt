package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "sort_type", defaultValue = "RECENTLY_ADDED")
    val sortType: String = "RECENTLY_ADDED"
)
