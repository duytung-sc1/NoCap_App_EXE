package com.nocap.app.domain.model

enum class DocumentReaderKind {
    EPUB_REFLOWABLE,
    PDF_FIXED,
    TEXT_REFLOWABLE,
    IMAGE_SINGLE,
    IMAGE_ARCHIVE
}

fun PublicationFormat.toReaderKind(): DocumentReaderKind = when (this) {
    PublicationFormat.EPUB -> DocumentReaderKind.EPUB_REFLOWABLE
    PublicationFormat.PDF -> DocumentReaderKind.PDF_FIXED
    PublicationFormat.TXT,
    PublicationFormat.MARKDOWN,
    PublicationFormat.HTML,
    PublicationFormat.DOCX -> DocumentReaderKind.TEXT_REFLOWABLE
    PublicationFormat.JPEG,
    PublicationFormat.PNG,
    PublicationFormat.WEBP -> DocumentReaderKind.IMAGE_SINGLE
    PublicationFormat.CBZ -> DocumentReaderKind.IMAGE_ARCHIVE
}
