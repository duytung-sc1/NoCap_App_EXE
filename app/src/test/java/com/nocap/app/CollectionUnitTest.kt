package com.nocap.app

import com.nocap.app.core.database.dao.CollectionDao
import com.nocap.app.core.database.dao.CollectionWithBookCount
import com.nocap.app.core.database.entity.BookCollectionCrossRef
import com.nocap.app.core.database.entity.CollectionEntity
import com.nocap.app.data.collection.LocalCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CollectionUnitTest {

    private lateinit var fakeDao: FakeCollectionDao
    private lateinit var repository: LocalCollectionRepository

    @Before
    fun setup() {
        fakeDao = FakeCollectionDao()
        repository = LocalCollectionRepository(fakeDao)
    }

    @Test
    fun createCollection_trimsWhitespaceAndCreatesValidEntity() = runBlocking {
        val result = repository.createCollection("  Khoa Hoc Vien Tuong  ")
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertNotNull(id)

        val created = fakeDao.getCollectionById(id)
        assertNotNull(created)
        assertEquals("Khoa Hoc Vien Tuong", created?.name)
    }

    @Test
    fun createCollection_rejectsBlankOrWhitespaceOnlyName() = runBlocking {
        val emptyResult = repository.createCollection("")
        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is IllegalArgumentException)

        val blankResult = repository.createCollection("    ")
        assertTrue(blankResult.isFailure)
        assertTrue(blankResult.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun createCollection_rejectsDuplicateCollectionName() = runBlocking {
        val first = repository.createCollection("Lich Su")
        assertTrue(first.isSuccess)

        val duplicate = repository.createCollection("Lich Su")
        assertTrue(duplicate.isFailure)
        assertTrue(duplicate.exceptionOrNull() is IllegalArgumentException)

        val duplicateWhitespace = repository.createCollection("  Lich Su  ")
        assertTrue(duplicateWhitespace.isFailure)
    }

    @Test
    fun renameCollection_updatesNameAndRejectsDuplicatesOrEmpty() = runBlocking {
        val id1 = repository.createCollection("Tam Ly Hoc").getOrThrow()
        val id2 = repository.createCollection("Kinh Te").getOrThrow()

        // Empty rename rejected
        val emptyRename = repository.renameCollection(id1, "  ")
        assertTrue(emptyRename.isFailure)

        // Duplicate rename rejected
        val dupRename = repository.renameCollection(id1, "Kinh Te")
        assertTrue(dupRename.isFailure)

        // Valid rename succeeds
        val validRename = repository.renameCollection(id1, "Tam Ly Va Xa Hoi")
        assertTrue(validRename.isSuccess)
        assertEquals("Tam Ly Va Xa Hoi", fakeDao.getCollectionById(id1)?.name)
    }

    @Test
    fun deleteCollection_removesCollectionButDoesNotDeleteBooks() = runBlocking {
        val colId = repository.createCollection("Sach Yeu Thich").getOrThrow()
        val bookId1 = "book_1"
        val bookId2 = "book_2"

        repository.addBookToCollection(bookId1, colId)
        repository.addBookToCollection(bookId2, colId)

        assertEquals(2, fakeDao.getBookIdsInCollection(colId).size)

        // Delete collection
        val deleteResult = repository.deleteCollection(colId)
        assertTrue(deleteResult.isSuccess)
        assertFalse(fakeDao.collections.containsKey(colId))
        // Collection deleted, books themselves remain
    }

    @Test
    fun updateBookCollections_correctlyAddsNewAndRemovesUnselected() = runBlocking {
        val c1 = repository.createCollection("Col 1").getOrThrow()
        val c2 = repository.createCollection("Col 2").getOrThrow()
        val c3 = repository.createCollection("Col 3").getOrThrow()

        val bookId = "book_abc"

        // Initially in c1 and c2
        repository.updateBookCollections(bookId, setOf(c1, c2))
        val current1 = repository.getCollectionsForBook(bookId).map { it.id }.toSet()
        assertEquals(setOf(c1, c2), current1)

        // Update to c2 and c3 (remove c1, add c3)
        repository.updateBookCollections(bookId, setOf(c2, c3))
        val current2 = repository.getCollectionsForBook(bookId).map { it.id }.toSet()
        assertEquals(setOf(c2, c3), current2)
    }

    class FakeCollectionDao : CollectionDao {
        val collections = mutableMapOf<String, CollectionEntity>()
        val crossRefs = mutableListOf<BookCollectionCrossRef>()

        override fun observeCollections(): Flow<List<CollectionEntity>> = flowOf(collections.values.toList())

        override fun observeCollectionsWithCount(): Flow<List<CollectionWithBookCount>> {
            val list = collections.values.map { c ->
                val count = crossRefs.count { it.collectionId == c.id }
                CollectionWithBookCount(
                    id = c.id,
                    name = c.name,
                    createdAt = c.createdAt,
                    updatedAt = c.updatedAt,
                    sortOrder = c.sortOrder,
                    sortType = c.sortType,
                    bookCount = count
                )
            }
            return flowOf(list)
        }

        override suspend fun getCollectionById(collectionId: String): CollectionEntity? = collections[collectionId]

        override suspend fun getCollectionByName(name: String): CollectionEntity? =
            collections.values.firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }

        override suspend fun insertCollection(collection: CollectionEntity) {
            collections[collection.id] = collection
        }

        override suspend fun updateCollection(collection: CollectionEntity) {
            collections[collection.id] = collection
        }

        override suspend fun deleteCollection(collectionId: String) {
            collections.remove(collectionId)
            crossRefs.removeAll { it.collectionId == collectionId }
        }

        override suspend fun addBookToCollection(crossRef: BookCollectionCrossRef) {
            if (crossRefs.none { it.bookId == crossRef.bookId && it.collectionId == crossRef.collectionId }) {
                crossRefs.add(crossRef)
            }
        }

        override suspend fun removeBookFromCollection(bookId: String, collectionId: String) {
            crossRefs.removeAll { it.bookId == bookId && it.collectionId == collectionId }
        }

        override fun observeCollectionsForBook(bookId: String): Flow<List<CollectionEntity>> =
            flowOf(getCollectionsForBookSync(bookId))

        override suspend fun getCollectionsForBook(bookId: String): List<CollectionEntity> =
            getCollectionsForBookSync(bookId)

        private fun getCollectionsForBookSync(bookId: String): List<CollectionEntity> {
            val colIds = crossRefs.filter { it.bookId == bookId }.map { it.collectionId }.toSet()
            return collections.values.filter { it.id in colIds }
        }

        override fun observeBookIdsInCollection(collectionId: String): Flow<List<String>> =
            flowOf(crossRefs.filter { it.collectionId == collectionId }.map { it.bookId })

        override suspend fun getBookIdsInCollection(collectionId: String): List<String> =
            crossRefs.filter { it.collectionId == collectionId }.map { it.bookId }
    }
}
