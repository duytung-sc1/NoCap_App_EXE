package com.ebookreader.app.domain.repository

import com.ebookreader.app.domain.model.CatalogBook
import com.ebookreader.app.domain.model.Category
import com.ebookreader.app.domain.model.HomeFeed
import kotlinx.coroutines.flow.Flow

/**
 * Contract for catalog data. The implementation can swap between a local seed source
 * (Milestone 2) and a remote API (future milestone) without changing call sites.
 */
interface CatalogRepository {
    fun observeHomeFeed(): Flow<HomeFeed>
    fun observeFeaturedBooks(): Flow<List<CatalogBook>>
    fun observeNewBooks(): Flow<List<CatalogBook>>
    fun observeCategories(): Flow<List<Category>>
    fun observeBooksByCategory(categoryId: String): Flow<List<CatalogBook>>
    fun searchBooks(query: String): Flow<List<CatalogBook>>
    suspend fun getBookById(bookId: String): CatalogBook?
}
