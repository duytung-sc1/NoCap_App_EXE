package com.ebookreader.app.domain.repository

import com.ebookreader.app.domain.model.CatalogBook
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<CatalogBook>>
    fun observeFavoriteBookIds(): Flow<Set<String>>
    fun observeIsFavorite(bookId: String): Flow<Boolean>
    suspend fun addFavorite(bookId: String)
    suspend fun removeFavorite(bookId: String)
    suspend fun toggleFavorite(bookId: String): Boolean
}
