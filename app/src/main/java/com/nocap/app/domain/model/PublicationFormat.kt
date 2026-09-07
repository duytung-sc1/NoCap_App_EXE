package com.nocap.app.domain.model

enum class PublicationFormat(val displayName: String, val defaultExtension: String, val mediaType: String) {
    EPUB("EPUB", "epub", "application/epub+zip"),
    PDF("PDF", "pdf", "application/pdf"),
    TXT("TXT", "txt", "text/plain"),
    MARKDOWN("Markdown", "md", "text/markdown"),
    HTML("HTML", "html", "text/html"),
    DOCX("DOCX", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp"),
    CBZ("CBZ", "cbz", "application/vnd.comicbook+zip");

    val isTextBased: Boolean
        get() = this in setOf(TXT, MARKDOWN, HTML, DOCX)

    val isSingleImage: Boolean
        get() = this in setOf(JPEG, PNG, WEBP)

    val isComicArchive: Boolean
        get() = this == CBZ
}
