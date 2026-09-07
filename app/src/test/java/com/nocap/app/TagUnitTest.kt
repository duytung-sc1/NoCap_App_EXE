package com.nocap.app

import com.nocap.app.core.database.dao.TagDao
import com.nocap.app.core.database.dao.TagWithBookCount
import com.nocap.app.core.database.entity.BookTagCrossRef
import com.nocap.app.core.database.entity.TagEntity
import com.nocap.app.data.tag.LocalTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagUnitTest {

    private lateinit var fakeDao: FakeTagDao
    private lateinit var repository: LocalTagRepository

    @Before
    fun setup() {
        fakeDao = FakeTagDao()
        repository = LocalTagRepository(fakeDao)
    }

    @Test
    fun createTag_normalizesNameAndPreservesVietnameseDiacritics() = runBlocking {
        val result = repository.createTag("  Kinh Tế Học  ")
        assertTrue(result.isSuccess)
        val tagId = result.getOrThrow()
        val created = fakeDao.getTagById(tagId)
        assertNotNull(created)
        assertEquals("Kinh Tế Học", created?.name)
        assertEquals("kinh tế học", created?.normalizedName)
    }

    @Test
    fun createTag_rejectsBlankOrWhitespaceOnly() = runBlocking {
        val emptyResult = repository.createTag("")
        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is IllegalArgumentException)

        val blankResult = repository.createTag("     ")
        assertTrue(blankResult.isFailure)
        assertTrue(blankResult.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun createTag_rejectsNamesLongerThan40Characters() = runBlocking {
        val longName = "a".repeat(41)
        val result = repository.createTag(longName)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun createTag_rejectsCaseInsensitiveDuplicates() = runBlocking {
        val first = repository.createTag("Triết Học")
        assertTrue(first.isSuccess)

        val duplicate = repository.createTag("triết học")
        assertTrue(duplicate.isFailure)
        assertTrue(duplicate.exceptionOrNull() is IllegalArgumentException)

        val duplicateWithSpaces = repository.createTag("  triết   học  ")
        assertTrue(duplicateWithSpaces.isFailure)
    }

    @Test
    fun renameTag_updatesNameAndRejectsDuplicates() = runBlocking {
        val id1 = repository.createTag("Tech").getOrThrow()
        val id2 = repository.createTag("Science").getOrThrow()

        // Blank rejected
        val blankResult = repository.renameTag(id1, "   ")
        assertTrue(blankResult.isFailure)

        // Duplicate rejected
        val dupResult = repository.renameTag(id1, "science")
        assertTrue(dupResult.isFailure)

        // Valid rename succeeds
        val validResult = repository.renameTag(id1, "Technology")
        assertTrue(validResult.isSuccess)
        assertEquals("Technology", fakeDao.getTagById(id1)?.name)
        assertEquals("technology", fakeDao.getTagById(id1)?.normalizedName)
    }

    @Test
    fun deleteTag_removesTagAndRefsWithoutDeletingBooks() = runBlocking {
        val tagId = repository.createTag("Design").getOrThrow()
        repository.setTagsForBook("book_1", listOf(tagId))
        repository.setTagsForBook("book_2", listOf(tagId))

        assertEquals(2, fakeDao.crossRefs.count { it.tagId == tagId })

        val deleteResult = repository.deleteTag(tagId)
        assertTrue(deleteResult.isSuccess)
        assertFalse(fakeDao.tags.containsKey(tagId))
        assertEquals(0, fakeDao.crossRefs.count { it.tagId == tagId })
    }

    @Test
    fun bulkAddTag_addsTagToMultipleBooks() = runBlocking {
        val tagId = repository.createTag("Programming").getOrThrow()
        val bookIds = listOf("book_a", "book_b", "book_c")

        val bulkResult = repository.bulkAddTag(bookIds, tagId)
        assertTrue(bulkResult.isSuccess)

        val refs = fakeDao.crossRefs.filter { it.tagId == tagId }
        assertEquals(3, refs.size)
        assertTrue(refs.map { it.bookId }.containsAll(bookIds))
    }

    @Test
    fun setTagsForBook_correctlyReplacesTags() = runBlocking {
        val t1 = repository.createTag("Tag1").getOrThrow()
        val t2 = repository.createTag("Tag2").getOrThrow()
        val t3 = repository.createTag("Tag3").getOrThrow()

        // Initial assignment: t1 and t2
        repository.setTagsForBook("book_x", listOf(t1, t2))
        var currentTags = fakeDao.getTagsForBookSync("book_x")
        assertEquals(setOf(t1, t2), currentTags.map { it.id }.toSet())

        // Update to t2 and t3
        repository.setTagsForBook("book_x", listOf(t2, t3))
        currentTags = fakeDao.getTagsForBookSync("book_x")
        assertEquals(setOf(t2, t3), currentTags.map { it.id }.toSet())
    }

    class FakeTagDao : TagDao {
        val tags = mutableMapOf<String, TagEntity>()
        val crossRefs = mutableListOf<BookTagCrossRef>()

        override fun observeTags(): Flow<List<TagEntity>> = flowOf(tags.values.toList())

        override fun observeTagsWithCount(): Flow<List<TagWithBookCount>> {
            val list = tags.values.map { t ->
                val count = crossRefs.count { it.tagId == t.id }
                TagWithBookCount(
                    id = t.id,
                    name = t.name,
                    normalizedName = t.normalizedName,
                    createdAt = t.createdAt,
                    bookCount = count
                )
            }
            return flowOf(list)
        }

        override suspend fun getTagById(tagId: String): TagEntity? = tags[tagId]

        override suspend fun getTagByNormalizedName(normalizedName: String): TagEntity? =
            tags.values.firstOrNull { it.normalizedName.equals(normalizedName.trim().lowercase(), ignoreCase = true) }

        override suspend fun insertTag(tag: TagEntity) {
            tags[tag.id] = tag
        }

        override suspend fun updateTag(tag: TagEntity) {
            tags[tag.id] = tag
        }

        override suspend fun deleteTag(tagId: String) {
            tags.remove(tagId)
            crossRefs.removeAll { it.tagId == tagId }
        }

        override suspend fun addTagToBook(crossRef: BookTagCrossRef) {
            if (crossRefs.none { it.bookId == crossRef.bookId && it.tagId == crossRef.tagId }) {
                crossRefs.add(crossRef)
            }
        }

        override suspend fun addTagsToBooks(crossRefs: List<BookTagCrossRef>) {
            crossRefs.forEach { ref ->
                if (this.crossRefs.none { it.bookId == ref.bookId && it.tagId == ref.tagId }) {
                    this.crossRefs.add(ref)
                }
            }
        }

        override suspend fun removeTagFromBook(bookId: String, tagId: String) {
            crossRefs.removeAll { it.bookId == bookId && it.tagId == tagId }
        }

        override suspend fun deleteTagsForBook(bookId: String) {
            crossRefs.removeAll { it.bookId == bookId }
        }

        override fun observeTagsForBook(bookId: String): Flow<List<TagEntity>> =
            flowOf(getTagsForBookSync(bookId))

        override suspend fun getTagsForBook(bookId: String): List<TagEntity> =
            getTagsForBookSync(bookId)

        fun getTagsForBookSync(bookId: String): List<TagEntity> {
            val tIds = crossRefs.filter { it.bookId == bookId }.map { it.tagId }.toSet()
            return tags.values.filter { it.id in tIds }
        }

        override fun observeAllTagCrossRefs(): Flow<List<BookTagCrossRef>> = flowOf(crossRefs.toList())
    }
}
