package com.nocap.app.domain.model

/**
 * Domain model representing a book in the user's personal library.
 * Aggregates catalog metadata, local download status, reading progression,
 * favorite status, and content update availability.
 */
data class LibraryBook(
    val book: CatalogBook,
    val downloadedBook: DownloadedBook,
    val readingProgress: ReadingProgress? = null,
    val isFavorite: Boolean = false,
    val isUpdateAvailable: Boolean = false
)
