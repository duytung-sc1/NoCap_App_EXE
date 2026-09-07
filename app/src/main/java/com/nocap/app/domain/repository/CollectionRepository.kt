package com.nocap.app.domain.repository

import com.nocap.app.core.database.dao.CollectionWithBookCount
import com.nocap.app.core.database.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeCollections(): Flow<List<CollectionWithBookCount>>
    fun observeCollectionsForBook(bookId: String): Flow<List<CollectionEntity>>
    suspend fun getCollectionsForBook(bookId: String): List<CollectionEntity>
    suspend fun createCollection(name: String): Result<String>
    suspend fun renameCollection(id: String, newName: String): Result<Unit>
    suspend fun deleteCollection(id: String): Result<Unit>
    suspend fun addBookToCollection(bookId: String, collectionId: String): Result<Unit>
    suspend fun removeBookFromCollection(bookId: String, collectionId: String): Result<Unit>
    suspend fun updateBookCollections(bookId: String, selectedCollectionIds: Set<String>): Result<Unit>
    fun observeBookIdsInCollection(collectionId: String): Flow<List<String>>
    suspend fun getCollectionById(collectionId: String): CollectionEntity?
}
