package com.nocap.app.domain.model

/**
 * Domain model representing a document/book in the user's personal library.
 * Aggregates catalog metadata, local download status, reading progression,
 * favorite status, update availability, tags, and collections.
 */
data class LibraryBook(
    val book: CatalogBook,
    val downloadedBook: DownloadedBook,
    val readingProgress: ReadingProgress? = null,
    val isFavorite: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val collections: List<String> = emptyList()
)
