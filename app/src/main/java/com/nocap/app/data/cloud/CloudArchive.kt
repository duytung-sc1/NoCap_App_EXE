package com.nocap.app.data.cloud

import java.io.File
import java.util.zip.ZipInputStream

internal object CloudArchive {
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
