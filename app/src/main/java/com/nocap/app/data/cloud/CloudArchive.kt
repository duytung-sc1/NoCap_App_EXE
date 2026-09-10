package com.nocap.app.data.cloud

import java.io.File
import java.util.zip.ZipInputStream

internal object CloudArchive {
    fun restoredPath(table: String, column: String, value: String, oldRoot: String, newRoot: File): String {
        val localPath = (table == "downloaded_books" && column == "local_file_path") ||
            (table == "catalog_books" && column == "custom_cover_path")
        val cover = table == "catalog_books" && column == "cover_url"
        if (value.startsWith(oldRoot)) return target(newRoot, value.removePrefix(oldRoot)).absolutePath
        if (localPath && value.isNotEmpty()) error("Đường dẫn tài liệu nằm ngoài bản sao lưu")
        if (cover && value.isNotEmpty()) {
            val uri = java.net.URI(value)
            require(uri.scheme in setOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null) {
                "Đường dẫn bìa không hợp lệ"
            }
        }
        return value
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
