package com.nocap.app.domain.model

import android.net.Uri

enum class PublicationSourceType {
    LOCAL_FILE,
    REMOTE_URL,
    SHARED_FILE,
    SHARED_URL
}

sealed interface PublicationSource {
    val sourceType: PublicationSourceType

    data class LocalUri(
        val uri: Uri,
        val displayNameHint: String? = null
    ) : PublicationSource {
        override val sourceType: PublicationSourceType = PublicationSourceType.LOCAL_FILE
    }

    data class RemoteUrl(
        val url: String
    ) : PublicationSource {
        override val sourceType: PublicationSourceType = PublicationSourceType.REMOTE_URL
    }

    data class SharedUri(
        val uri: Uri,
        val mimeType: String? = null
    ) : PublicationSource {
        override val sourceType: PublicationSourceType = PublicationSourceType.SHARED_FILE
    }

    data class SharedUrl(
        val url: String
    ) : PublicationSource {
        override val sourceType: PublicationSourceType = PublicationSourceType.SHARED_URL
    }
}

data class DownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long,
    val isIndeterminate: Boolean = totalBytes <= 0L
) {
    val percentage: Float?
        get() = if (totalBytes > 0L) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else null
}
