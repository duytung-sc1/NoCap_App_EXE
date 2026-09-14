package com.nocap.app

import com.nocap.app.data.parser.DocxParser
import com.nocap.app.domain.model.TextDocumentBlock
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxImageRegressionTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun drawingsResolveTheirOwnRelationshipAndIgnoreExternalTargets() {
        val file = temp.newFile("images.docx")
        val entries = linkedMapOf(
            "[Content_Types].xml" to "<Types/>",
            "word/document.xml" to """<w:document xmlns:w="urn:w" xmlns:a="urn:a" xmlns:r="urn:r"><w:body><w:p><w:r><w:drawing><a:blip r:embed="rSecond"/></w:drawing><w:drawing><a:blip r:embed="rFirst"/></w:drawing><w:drawing><a:blip r:link="rExternal"/></w:drawing></w:r></w:p></w:body></w:document>""",
            "word/_rels/document.xml.rels" to """<Relationships><Relationship Id="rFirst" Target="media/one.png"/><Relationship Id="rSecond" Target="media/two.png"/><Relationship Id="rExternal" Target="https://example.invalid/image.png" TargetMode="External"/></Relationships>""",
            "word/media/one.png" to "first image",
            "word/media/two.png" to "second image")
        ZipOutputStream(file.outputStream()).use { zip -> entries.forEach { (name, data) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(data.toByteArray()); zip.closeEntry()
        } }
        val cache = temp.newFolder("cache")
        val images = DocxParser.parse(file, cache).blocks.filterIsInstance<TextDocumentBlock.ImageBlock>()
        assertEquals(2, images.size)
        assertEquals(listOf("second image", "first image"), images.map { File(it.localPath!!).readText() })
        val filesBefore = cache.listFiles()!!.map { it.name }.toSet()
        DocxParser.parse(file, cache)
        assertEquals(filesBefore, cache.listFiles()!!.map { it.name }.toSet())
    }
}
