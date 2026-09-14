package com.nocap.app.data.parser

import java.io.File
import java.security.MessageDigest

internal object DocumentCacheKey {
    fun forFile(file: File, entry: String, revision: String = ""): String {
        val identity = "${file.canonicalPath}\u0000${file.length()}\u0000${file.lastModified()}\u0000$revision\u0000$entry"
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
