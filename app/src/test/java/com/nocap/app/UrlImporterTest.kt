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
import okio.buffer

class UrlImporterTest {

    @Test
    fun `network failures are recoverable and leave no temporary files`() = runBlocking {
        for (failure in listOf(java.net.UnknownHostException("offline"), java.net.SocketTimeoutException("timeout"), java.io.IOException("connection lost"))) {
            val client = OkHttpClient.Builder().addInterceptor { throw failure }.build()
            val result = RemotePublicationDownloader(baseClient = client, cacheDirectory = tempFolder.root)
                .download("https://example.com/book.pdf")
            assertTrue(result.exceptionOrNull() is ImportException.DownloadFailed)
            assertTrue(tempFolder.root.listFiles().orEmpty().isEmpty())
        }
    }

    @Test
    fun `server error can be retried without corrupting the successful file`() = runBlocking {
        var attempts = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if (attempts++ == 0) 503 else 200).message("test")
                .body("%PDF-test".toResponseBody("application/pdf".toMediaType())).build()
        }.build()
        val downloader = RemotePublicationDownloader(baseClient = client, cacheDirectory = tempFolder.root)
        assertTrue(downloader.download("https://example.com/book.pdf").isFailure)
        assertTrue(tempFolder.root.listFiles().orEmpty().isEmpty())
        assertEquals("%PDF-test", downloader.download("https://example.com/book.pdf").getOrThrow().tempFile.readText())
        assertEquals(1, tempFolder.root.listFiles().orEmpty().size)
    }

    @Test
    fun `cancelled copy propagates cancellation closes body and removes partial file`() = runBlocking {
        var closed = false
        val buffer = okio.Buffer().write(ByteArray(32_768) { 65 })
        val source = object : okio.ForwardingSource(buffer) {
            override fun close() { closed = true; super.close() }
        }
        val buffered = source.buffer()
        val body = object : okhttp3.ResponseBody() {
            override fun contentType() = "application/pdf".toMediaType()
            override fun contentLength() = 32_768L
            override fun source() = buffered
        }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(body).build()
        }.build()
        var cancelled = false
        try {
            RemotePublicationDownloader(baseClient = client, cacheDirectory = tempFolder.root)
                .download("https://example.com/book.pdf") { throw kotlinx.coroutines.CancellationException("test cancellation") }
        } catch (_: kotlinx.coroutines.CancellationException) { cancelled = true }
        assertTrue(cancelled)
        assertTrue(closed)
        assertTrue(tempFolder.root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `empty response fails and removes temporary file`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("".toResponseBody(null)).build()
        }.build()
        val result = RemotePublicationDownloader(baseClient = client, cacheDirectory = tempFolder.root)
            .download("https://example.com/empty.pdf")
        assertTrue(result.exceptionOrNull() is ImportException.FileNotFound)
        assertTrue(tempFolder.root.listFiles().orEmpty().isEmpty())
    }

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
