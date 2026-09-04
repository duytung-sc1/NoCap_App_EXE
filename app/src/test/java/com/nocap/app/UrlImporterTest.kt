package com.nocap.app

import com.nocap.app.data.importer.RemotePublicationDownloader
import com.nocap.app.domain.repository.ImportException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UrlImporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test rejects insecure HTTP url`() = runBlocking {
        val downloader = RemotePublicationDownloader(cacheDirectory = tempFolder.root)
        val result = downloader.download("http://example.com/book.epub")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ImportException.InsecureScheme)
    }

    @Test
    fun `test rejects invalid url syntax`() = runBlocking {
        val downloader = RemotePublicationDownloader(cacheDirectory = tempFolder.root)
        val result = downloader.download("invalid-url-string")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ImportException.InvalidUrl)
    }

    @Test
    fun `test rejects file exceeding 250MB via Content-Length`() = runBlocking {
        val oversizedBody = object : okhttp3.ResponseBody() {
            override fun contentType() = "application/pdf".toMediaType()
            override fun contentLength() = 260L * 1024L * 1024L
            override fun source() = okio.Buffer().writeUtf8("oversized content header")
        }

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Length", (260L * 1024L * 1024L).toString())
                    .body(oversizedBody)
                    .build()
            }
            .build()

        val downloader = RemotePublicationDownloader(
            baseClient = mockClient,
            cacheDirectory = tempFolder.root
        )

        val result = downloader.download("https://example.com/large.pdf")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ImportException.FileSizeLimitExceeded)
    }

    @Test
    fun `test successful publication download with progress tracking`() = runBlocking {
        val dummyData = "PK\u0003\u0004dummy epub content for testing".toByteArray()
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/epub+zip")
                    .header("Content-Disposition", "attachment; filename=\"test_book.epub\"")
                    .body(dummyData.toResponseBody("application/epub+zip".toMediaType()))
                    .build()
            }
            .build()

        val downloader = RemotePublicationDownloader(
            baseClient = mockClient,
            cacheDirectory = tempFolder.root
        )

        var lastProgressPercent = 0f
        val result = downloader.download("https://example.com/books/sample.epub") { progress ->
            progress.percentage?.let { lastProgressPercent = it }
        }

        assertTrue(result.isSuccess)
        val downloadRes = result.getOrThrow()
        assertEquals("test_book.epub", downloadRes.suggestedFilename)
        assertEquals("application/epub+zip", downloadRes.contentType)
        assertEquals(dummyData.size.toLong(), downloadRes.totalBytes)
        assertTrue(downloadRes.tempFile.exists())
        assertEquals(dummyData.size.toLong(), downloadRes.tempFile.length())
        assertEquals(1.0f, lastProgressPercent, 0.01f)
    }

    @Test
    fun `test rejects redirect downgrade from HTTPS to HTTP`() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(302)
                    .message("Found")
                    .header("Location", "http://insecure-site.org/book.epub")
                    .body("".toResponseBody(null))
                    .build()
            }
            .build()

        val downloader = RemotePublicationDownloader(
            baseClient = mockClient,
            cacheDirectory = tempFolder.root
        )

        val result = downloader.download("https://example.com/redirect")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ImportException.InsecureScheme)
    }

    @Test
    fun `test rejects excessive redirects exceeding limit`() = runBlocking {
        var count = 0
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                count++
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(302)
                    .message("Found")
                    .header("Location", "https://example.com/redirect_$count")
                    .body("".toResponseBody(null))
                    .build()
            }
            .build()

        val downloader = RemotePublicationDownloader(
            baseClient = mockClient,
            cacheDirectory = tempFolder.root
        )

        val result = downloader.download("https://example.com/start")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ImportException.DownloadFailed)
    }
}
