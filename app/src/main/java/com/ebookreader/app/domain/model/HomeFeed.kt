package com.ebookreader.app.domain.model

/**
 * Aggregated data for the Home screen feed.
 * Assembled by CatalogRepository from local seed / future remote API.
 */
data class HomeFeed(
    val continueReading: List<ReadingProgress> = emptyList(),
    val featuredBooks: List<CatalogBook> = emptyList(),
    val newBooks: List<CatalogBook> = emptyList(),
    val categories: List<Category> = emptyList()
)
