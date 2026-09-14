package com.nocap.app.domain.model

/** Local text bookmarks belong to a block; archive bookmarks belong to a page. */
object ReaderBookmarkPosition {
    fun matches(savedJson: String, current: DocumentLocator): Boolean = when (val saved = DocumentLocator.fromJson(savedJson)) {
        is TextLocator -> current is TextLocator && saved.blockIndex == current.blockIndex
        is ArchiveLocator -> current is ArchiveLocator && saved.pageIndex == current.pageIndex
        else -> false
    }
}
