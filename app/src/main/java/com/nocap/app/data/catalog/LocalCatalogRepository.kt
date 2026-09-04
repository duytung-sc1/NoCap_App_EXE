package com.nocap.app.data.catalog

import com.nocap.app.core.database.toDomain
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.Category
import com.nocap.app.domain.model.HomeFeed
import com.nocap.app.domain.model.ReadingProgress
import com.nocap.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Milestone 2 implementation: reads from the local seed catalog only.
 *
 * Architecture note:
 * A future RemoteCatalogRepository (or a DataSource abstraction layer) can replace
 * this class. The [CatalogRepository] interface remains the stable contract.
 *
 * The Room database (CatalogDao) is intentionally NOT wired here yet — it will be
 * used as a cache layer in the networking milestone when the remote API is integrated.
 */
class LocalCatalogRepository(
    private val progressDao: com.nocap.app.core.database.dao.ProgressDao? = null,
    private val catalogDao: com.nocap.app.core.database.dao.CatalogDao? = null
) : CatalogRepository {

    private val _allBooks = MutableStateFlow(SeedCatalogDataSource.books)
    private val _categories = MutableStateFlow(SeedCatalogDataSource.categories)
    private val _continueReading = MutableStateFlow<List<ReadingProgress>>(emptyList())

    override fun observeHomeFeed(): Flow<HomeFeed> {
        val continueReadingFlow = progressDao?.observeRecentlyRead()?.map { list ->
            list.map { it.toDomain() }
        } ?: _continueReading

        return combine(
            continueReadingFlow,
            _allBooks.map { it.filter { b -> b.isFeatured } },
            _allBooks.map { it.filter { b -> b.isNew } },
            _categories
        ) { continueReading, featured, newBooks, categories ->
            HomeFeed(
                continueReading = continueReading,
                featuredBooks = featured,
                newBooks = newBooks,
                categories = categories
            )
        }
    }

    override fun observeFeaturedBooks(): Flow<List<CatalogBook>> =
        _allBooks.map { it.filter { b -> b.isFeatured } }

    override fun observeNewBooks(): Flow<List<CatalogBook>> =
        _allBooks.map { it.filter { b -> b.isNew } }

    override fun observeCategories(): Flow<List<Category>> = _categories

    override fun observeBooksByCategory(categoryId: String): Flow<List<CatalogBook>> =
        _allBooks.map { books ->
            if (categoryId == "all") books
            else books.filter { it.categoryId == categoryId }
        }

    override fun searchBooks(query: String): Flow<List<CatalogBook>> =
        _allBooks.map { books ->
            if (query.isBlank()) books
            else {
                val q = query.trim().lowercase()
                books.filter {
                    it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
                }
            }
        }

    override suspend fun getBookById(bookId: String): CatalogBook? =
        catalogDao?.getBookById(bookId)?.toDomain() ?: _allBooks.value.find { it.id == bookId }
}
