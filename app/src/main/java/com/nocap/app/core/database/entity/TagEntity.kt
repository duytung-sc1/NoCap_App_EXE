package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["normalized_name"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
