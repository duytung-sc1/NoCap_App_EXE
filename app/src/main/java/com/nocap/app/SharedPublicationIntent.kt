package com.nocap.app

import android.content.Intent
import android.net.Uri
import android.os.BadParcelableException
import com.nocap.app.domain.model.PublicationSource

/** Reads external payloads without assuming the sender supplied the right extra type. */
internal fun readSharedPublicationSource(intent: Intent?): PublicationSource? {
    if (intent == null || intent.action !in setOf(Intent.ACTION_SEND, Intent.ACTION_VIEW)) return null
    return try {
        @Suppress("DEPRECATION")
        val stream = if (intent.action == Intent.ACTION_SEND) intent.extras?.get(Intent.EXTRA_STREAM) else null
        val uri = when (stream) {
            is Uri -> stream
            is String -> Uri.parse(stream)
            else -> intent.data
        }
        if (uri != null) {
            PublicationSource.SharedUri(uri, intent.type)
        } else {
            @Suppress("DEPRECATION")
            val text = intent.extras?.get(Intent.EXTRA_TEXT) as? CharSequence
            text?.let { Regex("""https://[^\s]+""", RegexOption.IGNORE_CASE).find(it)?.value }
                ?.let { PublicationSource.SharedUrl(it) }
        }
    } catch (_: BadParcelableException) {
        null
    } catch (_: ClassCastException) {
        null
    }
}

/** Keep the URI grant, but stop a completed import replaying after Activity recreation. */
internal fun Intent.markSharedImportHandled() {
    if (action == Intent.ACTION_SEND || action == Intent.ACTION_VIEW) action = null
}
