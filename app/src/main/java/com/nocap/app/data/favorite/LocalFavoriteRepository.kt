package com.nocap.app.data.favorite

import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.FavoriteDao
import com.nocap.app.core.database.entity.FavoriteEntity
import com.nocap.app.core.database.toDomain
import com.nocap.app.core.database.toEntity
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.repository.CatalogRepository
import com.nocap.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalFavoriteRepository(
    private val favoriteDao: FavoriteDao,
    private val catalogDao: CatalogDao,
    private val catalogRepository: CatalogRepository
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<CatalogBook>> {
        return favoriteDao.observeFavorites().map { favorites ->
            favorites.mapNotNull { fav ->
                catalogDao.getBookById(fav.bookId)?.toDomain()
                    ?: catalogRepository.getBookById(fav.bookId)
            }
        }
    }

    override fun observeFavoriteBookIds(): Flow<Set<String>> {
        return favoriteDao.observeFavorites().map { favorites ->
            favorites.map { it.bookId }.toSet()
        }
    }

    override fun observeIsFavorite(bookId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(bookId)
    }

    override suspend fun addFavorite(bookId: String) {
        // Ensure catalog book exists in database to satisfy Room foreign key constraint
        val existingInDb = catalogDao.getBookById(bookId)
        if (existingInDb == null) {
            val bookFromRepo = catalogRepository.getBookById(bookId)
            if (bookFromRepo != null) {
                // Ensure category exists as well
                val categories = catalogRepository.observeCategories().first()
                val category = categories.find { it.id == bookFromRepo.categoryId }
                if (category != null) {
                    catalogDao.insertCategories(listOf(category.toEntity()))
                }
                catalogDao.insertBook(bookFromRepo.toEntity())
            }
        }
        favoriteDao.addFavorite(FavoriteEntity(bookId = bookId))
    }

    override suspend fun removeFavorite(bookId: String) {
        favoriteDao.removeFavorite(bookId)
    }

    override suspend fun toggleFavorite(bookId: String): Boolean {
        val current = favoriteDao.isFavorite(bookId).first()
        return if (current) {
            removeFavorite(bookId)
            false
        } else {
            addFavorite(bookId)
            true
        }
    }
}
