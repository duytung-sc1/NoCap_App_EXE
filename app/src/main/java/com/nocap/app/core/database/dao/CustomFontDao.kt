package com.nocap.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nocap.app.core.database.entity.CustomFontEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFontDao {
    @Query("SELECT * FROM custom_fonts ORDER BY font_family ASC")
    fun observeFonts(): Flow<List<CustomFontEntity>>

    @Query("SELECT * FROM custom_fonts ORDER BY font_family ASC")
    suspend fun getAllFonts(): List<CustomFontEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFont(font: CustomFontEntity)

    @Query("DELETE FROM custom_fonts WHERE id = :id")
    suspend fun deleteFont(id: String)

    @Query("SELECT * FROM custom_fonts WHERE LOWER(TRIM(font_family)) = LOWER(TRIM(:fontFamily)) LIMIT 1")
    suspend fun getFontByName(fontFamily: String): CustomFontEntity?
}
