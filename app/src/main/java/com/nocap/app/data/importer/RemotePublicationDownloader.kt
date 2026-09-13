package com.nocap.app.data.importer

import android.content.Context
import com.nocap.app.domain.model.DownloadProgress
import com.nocap.app.domain.repository.ImportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class RemotePublicationDownloader(
    private val context: Context? = null,
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .build(),
    private val cacheDirectory: File? = null
) {

    companion object {
        const val MAX_FILE_SIZE_BYTES = 250L * 1024L * 1024L // 250 MB
        const val MAX_REDIRECTS = 5
        private const val BUFFER_SIZE = 8192
    }

    private val client = baseClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    data class DownloadResult(
        val tempFile: File,
        val suggestedFilename: String?,
        val contentType: String?,
        val totalBytes: Long
    )

    suspend fun download(
        rawUrl: String,
        onProgress: ((DownloadProgress) -> Unit)? = null
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        val httpUrl = rawUrl.trim().toHttpUrlOrNull()
            ?: return@withContext Result.failure(ImportException.InvalidUrl("Địa chỉ URL không hợp lệ: ${sanitizeUrl(rawUrl)}"))

        if (!httpUrl.isHttps) {
            return@withContext Result.failure(ImportException.InsecureScheme("Chỉ hỗ trợ liên kết bảo mật HTTPS"))
        }

        var currentUrl = httpUrl
        var redirectCount = 0
        var response: Response? = null

        val tmpPath = System.getProperty("java.io.tmpdir") ?: "."
        val baseDir = cacheDirectory ?: context?.cacheDir ?: File(tmpPath)
        val tempFile = File(baseDir, "remote_import_${UUID.randomUUID()}.tmp")

        try {
            while (redirectCount <= MAX_REDIRECTS) {
                currentCoroutineContext().ensureActive()

                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", "NoCap-Reader/1.0")
                    .get()
                    .build()

                val call = client.newCall(request)
                val resp = try {
                    call.execute()
                } catch (e: IOException) {
                    currentCoroutineContext().ensureActive()
                    return@withContext Result.failure(
                        ImportException.DownloadFailed("Lỗi kết nối mạng: ${e.localizedMessage}")
                    )
                }

                if (resp.isRedirect) {
                    val location = resp.header("Location")
                    resp.close()

                    if (location.isNullOrBlank()) {
                        return@withContext Result.failure(
                            ImportException.DownloadFailed("Chuyển hướng không có địa chỉ Location hợp lệ")
                        )
                    }

                    val nextUrl = currentUrl.resolve(location)
                        ?: return@withContext Result.failure(
                            ImportException.InvalidUrl("Địa chỉ chuyển hướng không hợp lệ: ${sanitizeUrl(location)}")
                        )

                    if (!nextUrl.isHttps) {
                        return@withContext Result.failure(
                            ImportException.InsecureScheme("Không cho phép chuyển hướng sang HTTP không an toàn")
                        )
                    }

                    currentUrl = nextUrl
                    redirectCount++
                    if (redirectCount > MAX_REDIRECTS) {
                        return@withContext Result.failure(
                            ImportException.DownloadFailed("Vượt quá số lần chuyển hướng tối đa ($MAX_REDIRECTS)")
                        )
                    }
                    continue
                }

                if (!resp.isSuccessful) {
                    val code = resp.code
                    resp.close()
                    return@withContext Result.failure(
                        ImportException.DownloadFailed("Máy chủ trả về mã lỗi HTTP $code")
                    )
                }

                response = resp
                break
            }

            val finalResponse = response
                ?: return@withContext Result.failure(ImportException.DownloadFailed("Không nhận được phản hồi từ máy chủ"))

            val body = finalResponse.body
                ?: return@withContext Result.failure(ImportException.FileNotFound("Dữ liệu tải về rỗng"))

            val contentLengthHeader = finalResponse.header("Content-Length")?.toLongOrNull()
            val contentLength = if (contentLengthHeader != null && contentLengthHeader > 0) {
                contentLengthHeader
            } else {
                body.contentLength()
            }

            if (contentLength > MAX_FILE_SIZE_BYTES) {
                finalResponse.close()
                return@withContext Result.failure(ImportException.FileSizeLimitExceeded())
            }

            val contentType = finalResponse.header("Content-Type")
            val contentDisposition = finalResponse.header("Content-Disposition")
            val suggestedFilename = parseFilenameFromHeader(contentDisposition)
                ?: currentUrl.pathSegments.lastOrNull()?.takeIf { it.isNotBlank() }

            var downloadedBytes = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        currentCoroutineContext().ensureActive()

                        downloadedBytes += bytesRead
                        if (downloadedBytes > MAX_FILE_SIZE_BYTES) {
                            output.flush()
                            tempFile.delete()
                            return@withContext Result.failure(ImportException.FileSizeLimitExceeded())
                        }

                        output.write(buffer, 0, bytesRead)

                        onProgress?.invoke(
                            DownloadProgress(
                                bytesRead = downloadedBytes,
                                totalBytes = if (contentLength > 0) contentLength else -1L
                            )
                        )
                    }
                    output.flush()
                }
            }
            finalResponse.close()

            if (tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext Result.failure(ImportException.FileNotFound("Tệp tải về có kích thước 0 byte"))
            }

            // Store only the readable article for web pages, never the site's full UI/links.
            val prefix = tempFile.inputStream().use { input -> val bytes = ByteArray(1024); val count = input.read(bytes); String(bytes, 0, count.coerceAtLeast(0), Charsets.UTF_8) }
            if (contentType?.substringBefore(';')?.trim()?.lowercase() in setOf("text/html", "application/xhtml+xml") ||
                Regex("<(?:!doctype\\s+html|html|head|body)\\b", RegexOption.IGNORE_CASE).containsMatchIn(prefix)) {
                if (tempFile.length() > 8L * 1024 * 1024) throw ImportException.DownloadFailed("Trang HTML quá lớn để trích xuất nội dung.")
                val declaredCharset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val clean = com.nocap.app.data.parser.WebArticleExtractor.extract(tempFile.readText(declaredCharset))
                tempFile.writeText(clean, Charsets.UTF_8)
            }

            Result.success(
                DownloadResult(
                    tempFile = tempFile,
                    suggestedFilename = suggestedFilename,
                    contentType = contentType,
                    totalBytes = tempFile.length()
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            if (e is ImportException) {
                Result.failure(e)
            } else {
                Result.failure(ImportException.DownloadFailed("Lỗi khi tải tệp: ${e.localizedMessage}"))
            }
        } finally {
            response?.close()
        }
    }

    private fun parseFilenameFromHeader(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val match = Regex("""filename\*?=['"]?(?:UTF-8'')?([^'";]+)['"]?""", RegexOption.IGNORE_CASE)
            .find(header)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun sanitizeUrl(url: String): String {
        return try {
            val u = url.toHttpUrlOrNull() ?: return url.take(50)
            "${u.scheme}://${u.host}${u.encodedPath}"
        } catch (_: Exception) {
            url.take(50)
        }
    }
}
