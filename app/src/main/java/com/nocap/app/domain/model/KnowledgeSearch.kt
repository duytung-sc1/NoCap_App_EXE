package com.nocap.app.domain.model

enum class KnowledgeItemType(val displayName: String) {
    ALL("Tất cả"),
    DOCUMENT("Tài liệu"),
    HIGHLIGHT("Đoạn trích"),
    NOTE("Ghi chú"),
    BOOKMARK("Đánh dấu"),
    CONTENT("Nội dung")
}

data class KnowledgeSearchResult(
    val id: String,
    val type: KnowledgeItemType,
    val bookId: String,
    val documentTitle: String,
    val documentAuthor: String?,
    val format: PublicationFormat,
    val title: String,
    val snippet: String,
    val locatorJson: String?,
    val timestamp: Long
)
