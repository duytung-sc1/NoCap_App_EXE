package com.ebookreader.app.data.reader

import android.content.Context
import com.ebookreader.app.domain.model.TocItem
import org.json.JSONObject
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser
import java.io.File
import java.io.FileNotFoundException

class ReadiumPublicationManager(
    private val context: Context
) {
    private val httpClient by lazy {
        DefaultHttpClient()
    }

    private val assetRetriever by lazy {
        AssetRetriever(context.contentResolver, httpClient)
    }

    private val publicationOpener by lazy {
        PublicationOpener(publicationParser = EpubParser())
    }

    suspend fun openPublication(file: File): Result<Publication> {
        if (!file.exists() || file.length() == 0L) {
            return Result.failure(FileNotFoundException("File not found or empty: ${file.absolutePath}"))
        }

        val assetResult = assetRetriever.retrieve(file)
        val asset = assetResult.getOrNull()
            ?: return Result.failure(
                IllegalStateException("Failed to retrieve asset from file: ${assetResult.failureOrNull()?.message}")
            )

        val pubResult = publicationOpener.open(asset = asset, allowUserInteraction = false)
        val publication = pubResult.getOrNull()
            ?: return Result.failure(
                IllegalStateException("Failed to parse EPUB publication: ${pubResult.failureOrNull()?.message}")
            )

        return Result.success(publication)
    }

    fun serializeLocator(locator: Locator): String {
        return locator.toJSON().toString()
    }

    fun deserializeLocator(json: String?): Locator? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            Locator.fromJSON(JSONObject(json))
        }.getOrNull()
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
