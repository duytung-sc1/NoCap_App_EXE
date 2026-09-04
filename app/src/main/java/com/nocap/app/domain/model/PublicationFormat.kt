package com.nocap.app.domain.model

enum class PublicationFormat(val displayName: String, val defaultExtension: String, val mediaType: String) {
    EPUB("EPUB", "epub", "application/epub+zip"),
    PDF("PDF", "pdf", "application/pdf"),
    CBZ("CBZ", "cbz", "application/vnd.comicbook+zip") // Reserved for future milestone
}
