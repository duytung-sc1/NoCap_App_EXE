package com.nocap.app.data.importer

import java.io.File
import java.io.IOException

/** Resolve only document files owned by this profile, never arbitrary DB paths. */
internal object ManagedDocumentFiles {
    fun resolve(profileFiles: File, path: String): File {
        require(path.isNotBlank()) { "Missing document path" }
        val canonicalProfile = profileFiles.canonicalFile
        val file = File(path).canonicalFile
        require(file.path.startsWith(canonicalProfile.path + File.separator)) { "Document path is outside profile storage" }
        require(!file.isDirectory) { "Document path is a directory" }
        require(isDocumentParent(canonicalProfile, file.parentFile)) { "Document path is outside profile storage" }
        return file
    }

    private fun isDocumentParent(profileFiles: File, parent: File?): Boolean {
        if (parent == null) return false
        if (parent.name != "imported" && parent.name != "books") return false
        var current: File? = parent.parentFile
        while (current != null) {
            if (current == profileFiles) return true
            val name = current.name
            if (name != "files" && !name.startsWith("restored-")) return false
            current = current.parentFile
        }
        return false
    }

    fun delete(profileFiles: File, path: String) {
        val file = resolve(profileFiles, path)
        if (file.exists() && !file.delete()) throw IOException("Could not remove document file")
    }
}
