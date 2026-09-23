package com.nocap.app.data.cloud

import java.io.File
import java.util.zip.ZipInputStream

internal object CloudArchive {
    private fun normalizePath(p: String): String {
        val s = p.replace('\\', '/')
        return if (s.startsWith("/data/data/")) "/data/user/0/" + s.removePrefix("/data/data/") else s
    }

    fun restoredPath(table: String, column: String, value: String, oldRoot: String, newRoot: File): String {
        val localPath = (table == "downloaded_books" && column == "local_file_path") ||
            (table == "catalog_books" && column == "custom_cover_path")
        val cover = table == "catalog_books" && column == "cover_url"
        if (cover && value.isNotEmpty()) {
            val uri = java.net.URI(value)
            require(uri.scheme in setOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null) {
                "Đường dẫn bìa không hợp lệ"
            }
            return value
        }
        if (!localPath || value.isEmpty()) {
            return value
        }

        require(!value.contains("..") && !value.startsWith("file://")) {
            "Đường dẫn sao lưu không hợp lệ"
        }

        val normVal = normalizePath(value)
        val normOld = normalizePath(oldRoot).trimEnd('/') + "/"

        // Security check: cannot reference another profile's storage
        if (normVal.contains("/profiles/")) {
            val oldProfile = normOld.substringAfter("/profiles/").substringBefore('/')
            val valProfile = normVal.substringAfter("/profiles/").substringBefore('/')
            if (oldProfile.isNotEmpty() && valProfile.isNotEmpty() && oldProfile != valProfile) {
                error("Đường dẫn tài liệu nằm ngoài bản sao lưu")
            }
        }

        // 1. Direct match with oldRoot
        if (normVal.startsWith(normOld)) {
            val rel = normVal.removePrefix(normOld).trimStart('/')
            return target(newRoot, rel).canonicalPath
        }

        // 2. Relative to parent app storage (e.g. files/imported/, files/books/, files/covers/)
        if (normOld.contains("/profiles/")) {
            val filesBase = normOld.substringBefore("/profiles/") + "/"
            if (normVal.startsWith(filesBase)) {
                val relFromFiles = normVal.removePrefix(filesBase).trimStart('/')
                if (!relFromFiles.startsWith("profiles/")) {
                    return target(newRoot, relFromFiles).canonicalPath
                }
            }
        }

        // 3. Simple relative path
        if (!normVal.startsWith("/") && !normVal.contains(':')) {
            return target(newRoot, normVal).canonicalPath
        }

        error("Đường dẫn tài liệu nằm ngoài bản sao lưu")
    }

    fun target(root: File, relative: String): File {
        require(relative.isNotBlank() && !relative.contains('\\') && relative.split('/').none { it == ".." || it == "." }) { "Đường dẫn sao lưu không hợp lệ" }
        val file=File(root,relative).canonicalFile
        require(file.path.startsWith(root.canonicalPath+File.separator)) { "Đường dẫn sao lưu không hợp lệ" }
        return file
    }
    fun extract(zipFile: File, root: File, maxBytes: Long = 4L*1024*1024*1024) {
        var expanded=0L
        val names=mutableSetOf<String>()
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while(true) {
                val entry=zip.nextEntry ?: break
                require(names.size<100000 && names.add(entry.name) && (entry.name=="database.json" || entry.name.startsWith("files/"))) { "Tệp sao lưu không hợp lệ" }
                val file=target(root,entry.name)
                if(!entry.isDirectory) {
                    file.parentFile!!.mkdirs()
                    file.outputStream().use { output ->
                        val buffer=ByteArray(8192)
                        while(true) { val count=zip.read(buffer);if(count<0)break;expanded+=count;require(expanded<=maxBytes) { "Bản sao lưu quá lớn" };output.write(buffer,0,count) }
                    }
                }
                zip.closeEntry()
            }
        }
    }
}
