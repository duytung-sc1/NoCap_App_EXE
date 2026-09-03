package com.nocap.app.data.catalog

import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.Category
import com.nocap.app.domain.model.EntitlementType

/**
 * Bundled development seed catalog.
 *
 * Design contract:
 * - This object is the ONLY place that knows about the "local" origin of data.
 * - CatalogRepository consumes this via a DataSource interface.
 * - A future RemoteCatalogDataSource can replace or supplement this without
 *   touching the repository or any upstream layer.
 *
 * Cover images use reliable public-domain placeholder URLs (via picsum.photos).
 * In production these will be CDN URLs from the backend.
 */
object SeedCatalogDataSource {

    val categories: List<Category> = listOf(
        Category(id = "fiction",       name = "Fiction",        displayOrder = 0),
        Category(id = "non-fiction",   name = "Non-Fiction",    displayOrder = 1),
        Category(id = "mystery",       name = "Mystery",        displayOrder = 2),
        Category(id = "sci-fi",        name = "Sci-Fi",         displayOrder = 3),
        Category(id = "romance",       name = "Romance",        displayOrder = 4),
        Category(id = "history",       name = "History",        displayOrder = 5),
        Category(id = "biography",     name = "Biography",      displayOrder = 6),
        Category(id = "self-help",     name = "Self-Help",      displayOrder = 7),
    )

    val books: List<CatalogBook> = listOf(
        // --- Featured ---
        CatalogBook(
            id = "book-001",
            title = "The Midnight Library",
            author = "Matt Haig",
            description = "Between life and death there is a library. When Nora Seed finds herself in the Midnight Library, she has a chance to make things right.",
            coverUrl = "https://picsum.photos/seed/book001/300/450",
            categoryId = "fiction",
            fileUrl = "https://www.gutenberg.org/ebooks/11.epub.images",
            fileSizeBytes = 360000L,
            isFeatured = true,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.5f,
            publishedDate = "2020-09-29"
        ),
        CatalogBook(
            id = "book-002",
            title = "Project Hail Mary",
            author = "Andy Weir",
            description = "A lone astronaut must save Earth from disaster in this propulsive new novel from the author of The Martian.",
            coverUrl = "https://picsum.photos/seed/book002/300/450",
            categoryId = "sci-fi",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = true,
            isNew = false,
            isPremium = true,
            playProductId = "premium_book_002",
            entitlementType = EntitlementType.ONE_TIME_PURCHASE,
            rating = 4.8f,
            publishedDate = "2021-05-04"
        ),
        CatalogBook(
            id = "book-003",
            title = "Atomic Habits",
            author = "James Clear",
            description = "A revolutionary system to get 1% better every day. Build good habits, break bad ones.",
            coverUrl = "https://picsum.photos/seed/book003/300/450",
            categoryId = "self-help",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = true,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.7f,
            publishedDate = "2018-10-16"
        ),
        CatalogBook(
            id = "book-004",
            title = "The Thursday Murder Club",
            author = "Richard Osman",
            description = "Four unlikely friends meet weekly to investigate unsolved murders. But then a real body turns up.",
            coverUrl = "https://picsum.photos/seed/book004/300/450",
            categoryId = "mystery",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = true,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.3f,
            publishedDate = "2020-09-03"
        ),
        // --- New Arrivals ---
        CatalogBook(
            id = "book-005",
            title = "Tomorrow, and Tomorrow, and Tomorrow",
            author = "Gabrielle Zevin",
            description = "Two friends build a game empire and something far more fragile than a business.",
            coverUrl = "https://picsum.photos/seed/book005/300/450",
            categoryId = "fiction",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = true,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.6f,
            publishedDate = "2022-07-05"
        ),
        CatalogBook(
            id = "book-006",
            title = "Fourth Wing",
            author = "Rebecca Yarros",
            description = "Enter the brutal and elite world of a war college where dragons choose their riders.",
            coverUrl = "https://picsum.photos/seed/book006/300/450",
            categoryId = "fiction",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = true,
            isPremium = true,
            playProductId = "premium_book_006",
            entitlementType = EntitlementType.ONE_TIME_PURCHASE,
            rating = 4.5f,
            publishedDate = "2023-05-02"
        ),
        CatalogBook(
            id = "book-007",
            title = "Intermezzo",
            author = "Sally Rooney",
            description = "Two brothers, a grief neither can speak of, and the women who enter their lives.",
            coverUrl = "https://picsum.photos/seed/book007/300/450",
            categoryId = "fiction",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = true,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.1f,
            publishedDate = "2024-10-01"
        ),
        CatalogBook(
            id = "book-008",
            title = "The Women",
            author = "Kristin Hannah",
            description = "A young woman follows her brother to serve as an Army nurse in Vietnam.",
            coverUrl = "https://picsum.photos/seed/book008/300/450",
            categoryId = "history",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = true,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.7f,
            publishedDate = "2024-02-06"
        ),
        // --- General catalog ---
        CatalogBook(
            id = "book-009",
            title = "Thinking, Fast and Slow",
            author = "Daniel Kahneman",
            description = "The two systems that drive the way we think — and how we can harness them.",
            coverUrl = "https://picsum.photos/seed/book009/300/450",
            categoryId = "non-fiction",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.6f,
            publishedDate = "2011-10-25"
        ),
        CatalogBook(
            id = "book-010",
            title = "Dune",
            author = "Frank Herbert",
            description = "Set in the far future amidst a feudal interstellar society, Dune is one of the greatest science fiction works ever written.",
            coverUrl = "https://picsum.photos/seed/book010/300/450",
            categoryId = "sci-fi",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.8f,
            publishedDate = "1965-08-01"
        ),
        CatalogBook(
            id = "book-011",
            title = "The Seven Husbands of Evelyn Hugo",
            author = "Taylor Jenkins Reid",
            description = "A reclusive Hollywood star finally tells the truth about her glamorous and scandalous life.",
            coverUrl = "https://picsum.photos/seed/book011/300/450",
            categoryId = "romance",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.7f,
            publishedDate = "2017-06-13"
        ),
        CatalogBook(
            id = "book-012",
            title = "Sapiens",
            author = "Yuval Noah Harari",
            description = "A brief history of humankind — from the Stone Age to the present.",
            coverUrl = "https://picsum.photos/seed/book012/300/450",
            categoryId = "history",
            fileUrl = "",
            fileSizeBytes = 0L,
            isFeatured = false,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.5f,
            publishedDate = "2011-01-01"
        ),
    )
}
