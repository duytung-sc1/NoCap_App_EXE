package com.nocap.app.domain.repository

import android.net.Uri
import com.nocap.app.core.database.entity.CustomFontEntity
import kotlinx.coroutines.flow.Flow

interface CustomFontRepository {
    fun observeFonts(): Flow<List<CustomFontEntity>>
    suspend fun getAllFonts(): List<CustomFontEntity>
    suspend fun importFont(uri: Uri, suggestedName: String? = null): Result<CustomFontEntity>
    suspend fun deleteFont(id: String): Result<Unit>
}
