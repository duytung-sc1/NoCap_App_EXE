package com.nocap.app.data.reader

import android.content.Context
import com.nocap.app.domain.model.TocItem
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileNotFoundException

class ReadiumPublicationManager(context: Context) {
    private val context = context.applicationContext
    private val httpClient by lazy {
        DefaultHttpClient()
    }

    private val assetRetriever by lazy {
        AssetRetriever(context.contentResolver, httpClient)
    }

    private val publicationParser by lazy {
        DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = PdfiumDocumentFactory(context)
        )
    }

    private val publicationOpener by lazy {
        PublicationOpener(publicationParser = publicationParser)
    }

    suspend fun openPublication(file: File): Result<Publication> {
        if (!file.exists() || file.length() == 0L) {
            return Result.failure(FileNotFoundException("File not found or empty: ${file.absolutePath}"))
        }

        val assetResult = assetRetriever.retrieve(file)
        val asset = assetResult.getOrNull()
            ?: return Result.failure(
                IllegalStateException("Không thể đọc tệp sách. Vui lòng kiểm tra hoặc nhập lại tệp.")
            )

        val pubResult = publicationOpener.open(asset = asset, allowUserInteraction = false)
        val publication = pubResult.getOrNull()
            ?: return Result.failure(
                IllegalStateException("Không thể mở nội dung sách. Tệp có thể bị hỏng hoặc không được hỗ trợ.")
            )

        return Result.success(publication)
    }

    fun serializeLocator(locator: Locator): String {
        return locator.toJSON().toString()
    }

    fun deserializeLocator(json: String?, publication: Publication? = null): Locator? {
        if (json.isNullOrBlank()) return null
        val standard = runCatching {
            Locator.fromJSON(JSONObject(json))
        }.getOrNull()
        if (standard != null) return standard

        val docLoc = com.nocap.app.domain.model.DocumentLocator.fromJson(json)
        if (docLoc is com.nocap.app.domain.model.PdfAnnotationLocator && publication != null) {
            val link = publication.readingOrder.firstOrNull() ?: publication.linkWithRel("contents")
            if (link != null) {
                return Locator(
                    href = link.url(),
                    mediaType = link.mediaType ?: org.readium.r2.shared.util.mediatype.MediaType.PDF,
                    locations = Locator.Locations(
                        position = docLoc.pageNumber,
                        progression = docLoc.progression.toDouble()
                    )
                )
            }
        }
        return null
    }


    fun extractTableOfContents(publication: Publication): List<TocItem> {
        return publication.tableOfContents.map { mapLinkToToc(it) }
    }

    private fun mapLinkToToc(link: Link): TocItem {
        val title = link.title ?: link.href.toString().substringAfterLast('/')
        val href = link.href.toString()
        val children = link.children.map { mapLinkToToc(it) }
        return TocItem(title = title, href = href, children = children)
    }

    fun closePublication(publication: Publication) {
        runCatching {
            publication.close()
        }
    }
}
