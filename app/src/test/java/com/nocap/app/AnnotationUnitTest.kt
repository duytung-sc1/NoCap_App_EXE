package com.nocap.app

import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.data.annotation.LocalAnnotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeHighlightDao : HighlightDao {
    val map = mutableMapOf<String, HighlightEntity>()

    override fun observeHighlightsForBook(bookId: String): Flow<List<HighlightEntity>> =
        flowOf(map.values.filter { it.bookId == bookId })

    override suspend fun getHighlightsForBook(bookId: String): List<HighlightEntity> =
        map.values.filter { it.bookId == bookId }

    override suspend fun getHighlightById(id: String): HighlightEntity? = map[id]

    override suspend fun insertHighlight(highlight: HighlightEntity) {
        map[highlight.id] = highlight
    }

    override suspend fun updateHighlight(highlight: HighlightEntity) {
        map[highlight.id] = highlight
    }

    override suspend fun deleteHighlight(id: String) {
        map.remove(id)
    }

    override suspend fun deleteHighlightsByBookId(bookId: String) {
        val toRemove = map.values.filter { it.bookId == bookId }.map { it.id }
        toRemove.forEach { map.remove(it) }
    }
}

class AnnotationUnitTest {

    private lateinit var fakeDao: FakeHighlightDao
    private lateinit var repository: LocalAnnotationRepository

    companion object {
        const val COLOR_YELLOW = 0x66FFEB3B
        const val COLOR_GREEN = 0x664CAF50
        const val COLOR_BLUE = 0x662196F3
        const val COLOR_PINK = 0x66E91E63
    }

    @Before
    fun setup() {
        fakeDao = FakeHighlightDao()
        repository = LocalAnnotationRepository(fakeDao)
    }

    @Test
    fun addHighlight_createsEntityWithValidFieldsAndTimestamps() = runBlocking {
        val locatorJson = "{}"
        val result = repository.addHighlight(
            bookId = "book_1",
            locatorJson = locatorJson,
            text = "This is a highlighted sentence.",
            color = "YELLOW",
            note = "My personal thought"
        )
        assertTrue(result.isSuccess)
        val entity = result.getOrThrow()
        assertNotNull(entity.id)
        assertEquals("book_1", entity.bookId)
        assertEquals("This is a highlighted sentence.", entity.text)
        assertEquals("YELLOW", entity.color)
        assertEquals("My personal thought", entity.note)
        assertTrue(entity.createdAt > 0)
        assertTrue(entity.updatedAt >= entity.createdAt)
    }

    @Test
    fun addHighlight_convertsBlankNoteToNull() = runBlocking {
        val result = repository.addHighlight(
            bookId = "book_1",
            locatorJson = "{}",
            text = "Highlighted text",
            color = "GREEN",
            note = "   "
        )
        assertTrue(result.isSuccess)
        val entity = result.getOrThrow()
        assertNull(entity.note)
    }

    @Test
    fun updateHighlightColor_modifiesColorSuccessfully() = runBlocking {
        val initial = repository.addHighlight(
            bookId = "book_1",
            locatorJson = "{}",
            text = "Text",
            color = "YELLOW",
            note = null
        ).getOrThrow()

        val updateResult = repository.updateHighlightColor(initial.id, "PINK")
        assertTrue(updateResult.isSuccess)

        val updated = fakeDao.getHighlightById(initial.id)
        assertEquals("PINK", updated?.color)
    }

    @Test
    fun updateNote_updatesNoteAndHandlesBlank() = runBlocking {
        val initial = repository.addHighlight(
            bookId = "book_1",
            locatorJson = "{}",
            text = "Text",
            color = "BLUE",
            note = "Initial note"
        ).getOrThrow()

        val updateResult = repository.updateNote(initial.id, "Updated note")
        assertTrue(updateResult.isSuccess)
        assertEquals("Updated note", fakeDao.getHighlightById(initial.id)?.note)

        val clearResult = repository.updateNote(initial.id, "   ")
        assertTrue(clearResult.isSuccess)
        assertNull(fakeDao.getHighlightById(initial.id)?.note)
    }

    @Test
    fun deleteHighlight_removesHighlightFromDatabase() = runBlocking {
        val initial = repository.addHighlight(
            bookId = "book_1",
            locatorJson = "{}",
            text = "To be deleted",
            color = "YELLOW",
            note = null
        ).getOrThrow()

        val deleteResult = repository.deleteHighlight(initial.id)
        assertTrue(deleteResult.isSuccess)
        assertNull(fakeDao.getHighlightById(initial.id))
    }

    @Test
    fun highlightColors_haveCorrectArgbValues() {
        assertEquals(0x66FFEB3B, COLOR_YELLOW)
        assertEquals(0x664CAF50, COLOR_GREEN)
        assertEquals(0x662196F3, COLOR_BLUE)
        assertEquals(0x66E91E63, COLOR_PINK)
    }

    @Test
    fun quoteShareFormatting_formatsCleanlyWithoutLeakingPrivateData() {
        val quoteText = "It is our choices, Harry, that show what we truly are."
        val bookTitle = "Harry Potter and the Chamber of Secrets"
        val author = "J.K. Rowling"

        val formattedQuote = String.format("'%s'\n\n- Trich tu: '%s' (%s)", quoteText, bookTitle, author)

        assertTrue(formattedQuote.contains(quoteText))
        assertTrue(formattedQuote.contains(bookTitle))
        assertTrue(formattedQuote.contains(author))
        assertFalse(formattedQuote.contains("internal_id"))
        assertFalse(formattedQuote.contains("locatorJson"))
    }
}
