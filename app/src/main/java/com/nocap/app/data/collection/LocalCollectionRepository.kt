package com.nocap.app.data.collection

import com.nocap.app.core.database.dao.CollectionDao
import com.nocap.app.core.database.dao.CollectionWithBookCount
import com.nocap.app.core.database.entity.BookCollectionCrossRef
import com.nocap.app.core.database.entity.CollectionEntity
import com.nocap.app.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LocalCollectionRepository(
    private val collectionDao: CollectionDao
) : CollectionRepository {

    override fun observeCollections(): Flow<List<CollectionWithBookCount>> {
        return collectionDao.observeCollectionsWithCount()
    }

    override fun observeCollectionsForBook(bookId: String): Flow<List<CollectionEntity>> {
        return collectionDao.observeCollectionsForBook(bookId)
    }

    override suspend fun getCollectionsForBook(bookId: String): List<CollectionEntity> {
        return collectionDao.getCollectionsForBook(bookId)
    }

    override suspend fun createCollection(name: String): Result<String> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Tên bộ sưu tập không được để trống"))
        }
        val existing = collectionDao.getCollectionByName(trimmed)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Bộ sưu tập '$trimmed' đã tồn tại"))
        }

        val id = UUID.randomUUID().toString()
        val entity = CollectionEntity(
            id = id,
            name = trimmed,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return try {
            collectionDao.insertCollection(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameCollection(id: String, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Tên bộ sưu tập không được để trống"))
        }
        val current = collectionDao.getCollectionById(id)
            ?: return Result.failure(IllegalArgumentException("Không tìm thấy bộ sưu tập"))

        val existing = collectionDao.getCollectionByName(trimmed)
        if (existing != null && existing.id != id) {
            return Result.failure(IllegalArgumentException("Bộ sưu tập '$trimmed' đã tồn tại"))
        }

        val updated = current.copy(
            name = trimmed,
            updatedAt = System.currentTimeMillis()
        )
        return try {
            collectionDao.updateCollection(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCollection(id: String): Result<Unit> {
        return try {
            collectionDao.deleteCollection(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addBookToCollection(bookId: String, collectionId: String): Result<Unit> {
        return try {
            collectionDao.addBookToCollection(
                BookCollectionCrossRef(
                    bookId = bookId,
                    collectionId = collectionId,
                    addedAt = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeBookFromCollection(bookId: String, collectionId: String): Result<Unit> {
        return try {
            collectionDao.removeBookFromCollection(bookId, collectionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBookCollections(bookId: String, selectedCollectionIds: Set<String>): Result<Unit> {
        return try {
            val currentCollections = collectionDao.getCollectionsForBook(bookId).map { it.id }.toSet()
            val toAdd = selectedCollectionIds - currentCollections
            val toRemove = currentCollections - selectedCollectionIds

            for (colId in toAdd) {
                collectionDao.addBookToCollection(
                    BookCollectionCrossRef(bookId = bookId, collectionId = colId, addedAt = System.currentTimeMillis())
                )
            }
            for (colId in toRemove) {
                collectionDao.removeBookFromCollection(bookId, colId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeBookIdsInCollection(collectionId: String): Flow<List<String>> {
        return collectionDao.observeBookIdsInCollection(collectionId)
    }

    override suspend fun getCollectionById(collectionId: String): CollectionEntity? {
        return collectionDao.getCollectionById(collectionId)
    }
}
