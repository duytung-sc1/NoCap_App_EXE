package com.ebookreader.app.data.bookmark

import com.ebookreader.app.core.database.dao.BookmarkDao
import com.ebookreader.app.core.database.dao.CatalogDao
import com.ebookreader.app.core.database.toDomain
import com.ebookreader.app.core.database.toEntity
import com.ebookreader.app.domain.model.Bookmark
import com.ebookreader.app.domain.repository.BookmarkRepository
import com.ebookreader.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalBookmarkRepository(
    private val bookmarkDao: BookmarkDao,
    private val catalogDao: CatalogDao,
    private val catalogRepository: CatalogRepository
) : BookmarkRepository {

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.observeBookmarksForBook(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addBookmark(bookmark: Bookmark) {
        // Ensure catalog book exists in database to satisfy Room foreign key constraint
        val existing = catalogDao.getBookById(bookmark.bookId)
        if (existing == null) {
            val bookFromRepo = catalogRepository.getBookById(bookmark.bookId)
            if (bookFromRepo != null) {
                val categories = catalogRepository.observeCategories().first()
                val category = categories.find { it.id == bookFromRepo.categoryId }
                if (category != null) {
                    catalogDao.insertCategories(listOf(category.toEntity()))
                }
                catalogDao.insertBook(bookFromRepo.toEntity())
            }
        }
        bookmarkDao.insertBookmark(bookmark.toEntity())
    }

    override suspend fun removeBookmark(bookmarkId: String) {
        bookmarkDao.hardDeleteBookmark(bookmarkId)
    }
}
