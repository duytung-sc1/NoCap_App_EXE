package com.nocap.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_fonts")
data class CustomFontEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "font_family")
    val fontFamily: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
