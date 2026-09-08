package com.nocap.app.data.search

import com.nocap.app.core.database.dao.BookmarkDao
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.DownloadDao
import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.util.VietnameseUtils
import com.nocap.app.domain.model.KnowledgeItemType
import com.nocap.app.domain.model.KnowledgeSearchResult
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.repository.KnowledgeSearchRepository
import java.io.File

class LocalKnowledgeSearchRepository(
    private val catalogDao: CatalogDao,
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    private val downloadDao: DownloadDao
) : KnowledgeSearchRepository {

    override suspend fun search(
        query: String,
        typeFilter: KnowledgeItemType,
        formatFilter: PublicationFormat?
    ): List<KnowledgeSearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val results = mutableListOf<KnowledgeSearchResult>()

        val books = catalogDao.getAllBooks()
        val booksMap = books.associateBy { it.id }

        fun matchesFormat(format: PublicationFormat): Boolean {
            return formatFilter == null || format == formatFilter
        }

        // 1. Search Documents
        if (typeFilter == KnowledgeItemType.ALL || typeFilter == KnowledgeItemType.DOCUMENT) {
            for (book in books) {
                if (!matchesFormat(book.format)) continue
                val title = book.userTitleOverride ?: book.title
                val author = book.userAuthorOverride ?: book.author
                val matchesTitle = VietnameseUtils.containsNormalized(title, cleanQuery)
                val matchesAuthor = VietnameseUtils.containsNormalized(author, cleanQuery)
                val matchesDesc = VietnameseUtils.containsNormalized(book.description, cleanQuery)
                val matchesFilename = VietnameseUtils.containsNormalized(book.originalFilename, cleanQuery)

                if (matchesTitle || matchesAuthor || matchesDesc || matchesFilename) {
                    val snippet = when {
                        matchesTitle -> "Tiêu đề: $title"
                        matchesAuthor -> "Tác giả: $author"
                        matchesDesc -> book.description.take(120)
                        else -> book.originalFilename ?: title
                    }
                    results.add(
                        KnowledgeSearchResult(
                            id = "doc_${book.id}",
                            type = KnowledgeItemType.DOCUMENT,
                            bookId = book.id,
                            documentTitle = title,
                            documentAuthor = author,
                            format = book.format,
                            title = title,
                            snippet = snippet,
                            locatorJson = null,
                            timestamp = book.updatedAt
                        )
                    )
                }
            }
        }

        // 2. Search Highlights & Notes
        if (typeFilter == KnowledgeItemType.ALL || typeFilter == KnowledgeItemType.HIGHLIGHT || typeFilter == KnowledgeItemType.NOTE) {
            val allHighlights = highlightDao.getAllHighlights()
            for (hl in allHighlights) {
                val book = booksMap[hl.bookId] ?: continue
                if (!matchesFormat(book.format)) continue
                val docTitle = book.userTitleOverride ?: book.title
                val docAuthor = book.userAuthorOverride ?: book.author

                val isNote = !hl.note.isNullOrBlank()
                val matchesText = VietnameseUtils.containsNormalized(hl.text, cleanQuery)
                val matchesNote = isNote && VietnameseUtils.containsNormalized(hl.note, cleanQuery)

                if (isNote) {
                    if ((typeFilter == KnowledgeItemType.ALL || typeFilter == KnowledgeItemType.NOTE) && (matchesNote || matchesText)) {
                        results.add(
                            KnowledgeSearchResult(
                                id = "note_${hl.id}",
                                type = KnowledgeItemType.NOTE,
                                bookId = hl.bookId,
                                documentTitle = docTitle,
                                documentAuthor = docAuthor,
                                format = book.format,
                                title = hl.note,
                                snippet = "Đoạn trích: \"${hl.text.take(100)}\"",
                                locatorJson = hl.locatorJson,
                                timestamp = hl.updatedAt
                            )
                        )
                    } else if (typeFilter == KnowledgeItemType.HIGHLIGHT && matchesText) {
                        results.add(
                            KnowledgeSearchResult(
                                id = "hl_${hl.id}",
                                type = KnowledgeItemType.HIGHLIGHT,
                                bookId = hl.bookId,
                                documentTitle = docTitle,
                                documentAuthor = docAuthor,
                                format = book.format,
                                title = hl.text.take(60),
                                snippet = hl.text,
                                locatorJson = hl.locatorJson,
                                timestamp = hl.createdAt
                            )
                        )
                    }
                } else {
                    if ((typeFilter == KnowledgeItemType.ALL || typeFilter == KnowledgeItemType.HIGHLIGHT) && matchesText) {
                        results.add(
                            KnowledgeSearchResult(
                                id = "hl_${hl.id}",
                                type = KnowledgeItemType.HIGHLIGHT,
                                bookId = hl.bookId,
                                documentTitle = docTitle,
                                documentAuthor = docAuthor,
                                format = book.format,
                                title = hl.text.take(60),
                                snippet = hl.text,
                                locatorJson = hl.locatorJson,
                                timestamp = hl.createdAt
                            )
                        )
                    }
                }
            }
        }

        // 3. Search Bookmarks
        if (typeFilter == KnowledgeItemType.ALL || typeFilter == KnowledgeItemType.BOOKMARK) {
            val allBookmarks = bookmarkDao.getAllBookmarks()
            for (bm in allBookmarks) {
                if (bm.isDeleted) continue
                val book = booksMap[bm.bookId] ?: continue
                if (!matchesFormat(book.format)) continue
                val docTitle = book.userTitleOverride ?: book.title
                val docAuthor = book.userAuthorOverride ?: book.author

                val matchesTitle = VietnameseUtils.containsNormalized(bm.chapterTitle, cleanQuery)
                val matchesSnippet = VietnameseUtils.containsNormalized(bm.snippet, cleanQuery)

                if (matchesTitle || matchesSnippet) {
                    results.add(
                        KnowledgeSearchResult(
                            id = "bm_${bm.id}",
                            type = KnowledgeItemType.BOOKMARK,
                            bookId = bm.bookId,
                            documentTitle = docTitle,
                            documentAuthor = docAuthor,
                            format = book.format,
                            title = bm.chapterTitle,
                            snippet = bm.snippet ?: bm.chapterTitle,
                            locatorJson = bm.locatorJson,
                            timestamp = bm.createdAt
                        )
                    )
                }
            }
        }

        // 4. Search Document Content (for local text files)
        if ((typeFilter == KnowledgeItemType.ALL || typeFilter == KnowledgeItemType.CONTENT) && cleanQuery.length >= 3) {
            val textFormats = setOf(
                PublicationFormat.TXT,
                PublicationFormat.MARKDOWN,
                PublicationFormat.HTML
            )
            for (book in books) {
                if (book.format !in textFormats) continue
                if (!matchesFormat(book.format)) continue
                val download = downloadDao.getDownloadByBookId(book.id) ?: continue
                val file = File(download.localFilePath)
                if (!file.exists() || !file.isFile || file.length() > 5 * 1024 * 1024) continue

                try {
                    val lines = file.readLines()
                    for ((lineIdx, line) in lines.withIndex()) {
                        if (VietnameseUtils.containsNormalized(line, cleanQuery)) {
                            val docTitle = book.userTitleOverride ?: book.title
                            val docAuthor = book.userAuthorOverride ?: book.author
                            results.add(
                                KnowledgeSearchResult(
                                    id = "content_${book.id}_$lineIdx",
                                    type = KnowledgeItemType.CONTENT,
                                    bookId = book.id,
                                    documentTitle = docTitle,
                                    documentAuthor = docAuthor,
                                    format = book.format,
                                    title = "Dòng ${lineIdx + 1}",
                                    snippet = line.trim().take(150),
                                    locatorJson = null,
                                    timestamp = book.updatedAt
                                )
                            )
                            if (results.count { it.bookId == book.id && it.type == KnowledgeItemType.CONTENT } >= 3) {
                                break
                            }
                        }
                    }
                } catch (ignored: Exception) {
                    // Ignore I/O errors during background content search
                }
            }
        }

        return results.sortedByDescending { it.timestamp }
    }
}
