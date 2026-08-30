package com.ebookreader.app.domain.model

data class TocItem(
    val title: String,
    val href: String,
    val children: List<TocItem> = emptyList()
)
