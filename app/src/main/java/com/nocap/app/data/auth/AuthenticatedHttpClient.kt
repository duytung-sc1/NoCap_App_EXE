package com.nocap.app.data.auth

import com.nocap.app.domain.repository.AuthTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class AuthenticatedHttpClient(
    private val tokenProvider: AuthTokenProvider,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun execute(
        url: String,
        method: String = "GET",
        jsonBody: String? = null
    ): Response = withContext(Dispatchers.IO) {
        var token = tokenProvider.getIdToken(forceRefresh = false)
            ?: throw IllegalStateException("Ngu?i dùng chua du?c xác th?c")

        val body = jsonBody?.toRequestBody(mediaType)
            ?: if (method in listOf("POST", "PUT", "PATCH")) "".toRequestBody(mediaType) else null

        var request = Request.Builder()
            .url(url)
            .method(method, body)
            .header("Authorization", "Bearer $token")
            .build()

        var response = okHttpClient.newCall(request).execute()

        // 401 stale token: force refresh token and retry at most once
        if (response.code == 401) {
            response.close()
            val freshToken = tokenProvider.getIdToken(forceRefresh = true)
            if (!freshToken.isNullOrBlank()) {
                request = Request.Builder()
                    .url(url)
                    .method(method, body)
                    .header("Authorization", "Bearer $freshToken")
                    .build()
                response = okHttpClient.newCall(request).execute()
            }
        }

        response
    }
}

