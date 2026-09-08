package com.nocap.app

import com.nocap.app.data.catalog.LocalCatalogRepository
import com.nocap.app.data.catalog.SeedCatalogDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalCatalogRepositoryTest {

    private lateinit var repository: LocalCatalogRepository

    @Before
    fun setup() {
        repository = LocalCatalogRepository()
    }

    @Test
    fun observeHomeFeed_emitsExpectedSections() = runTest {
        val feed = repository.observeHomeFeed().first()

        assertNotNull(feed)
        assertTrue(feed.featuredBooks.isNotEmpty())
        assertTrue(feed.featuredBooks.all { it.isFeatured })
        assertTrue(feed.newBooks.isNotEmpty())
        assertTrue(feed.newBooks.all { it.isNew })
        assertEquals(SeedCatalogDataSource.categories.size, feed.categories.size)
    }

    @Test
    fun observeBooksByCategory_filtersCorrectly() = runTest {
        val fictionBooks = repository.observeBooksByCategory("fiction").first()
        assertTrue(fictionBooks.isNotEmpty())
        assertTrue(fictionBooks.all { it.categoryId == "fiction" })

        val allBooks = repository.observeBooksByCategory("all").first()
        assertEquals(SeedCatalogDataSource.books.size, allBooks.size)
    }

    @Test
    fun searchBooks_matchesTitleAndAuthor() = runTest {
        val resultsByTitle = repository.searchBooks("truyen kieu").first()
        assertEquals(1, resultsByTitle.size)
        assertEquals("Truyện Kiều", resultsByTitle.first().title)

        val resultsByAuthor = repository.searchBooks("nguyen dinh chieu").first()
        assertEquals(1, resultsByAuthor.size)
        assertEquals("Nguyễn Đình Chiểu", resultsByAuthor.first().author)

        val emptyQuery = repository.searchBooks("").first()
        assertEquals(SeedCatalogDataSource.books.size, emptyQuery.size)
    }

    @Test
    fun getBookById_returnsCorrectBook() = runTest {
        val book = repository.getBookById("ws-vi-kieu")
        assertNotNull(book)
        assertEquals("Truyện Kiều", book?.title)
    }
}
