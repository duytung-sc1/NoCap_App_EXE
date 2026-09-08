package com.nocap.app.domain.model

import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.HighlightEntity

data class HighlightWithBook(
    val highlight: HighlightEntity,
    val book: CatalogBookEntity,
    val isUnderReview: Boolean = false
)

data class BookmarkWithBook(
    val bookmark: BookmarkEntity,
    val book: CatalogBookEntity
)
