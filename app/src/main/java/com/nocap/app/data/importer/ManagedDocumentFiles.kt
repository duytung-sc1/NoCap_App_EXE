package com.nocap.app.data.importer

import java.io.File
import java.io.IOException

/** Resolve only document files owned by this profile, never arbitrary DB paths. */
internal object ManagedDocumentFiles {
    fun resolve(profileFiles: File, path: String): File {
        require(path.isNotBlank()) { "Missing document path" }
        val file = File(path).canonicalFile
        val roots = listOf("imported", "books").map { File(profileFiles, it).canonicalFile }
        require(file.parentFile in roots) { "Document path is outside profile storage" }
        require(!file.isDirectory) { "Document path is a directory" }
        return file
    }

    fun delete(profileFiles: File, path: String) {
        val file = resolve(profileFiles, path)
        if (file.exists() && !file.delete()) throw IOException("Could not remove document file")
    }
}
