package com.nocap.app.data.auth

import com.nocap.app.BuildConfig
import com.nocap.app.domain.model.UserProfile
import com.nocap.app.domain.repository.AccountRepository
import com.nocap.app.domain.repository.AuthTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BackendAccountRepository(
    tokenProvider: AuthTokenProvider,
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL
) : AccountRepository {

    private val httpClient = AuthenticatedHttpClient(tokenProvider)

    override suspend fun getProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val response = httpClient.execute(
                url = "$baseUrl/api/v1/me",
                method = "GET"
            )
            response.use {
                if (!it.isSuccessful) {
                    throw IllegalStateException("Loi khi tai thong tin ho so: HTTP ${it.code}")
                }
                val bodyString = it.body?.string().orEmpty()
                val json = JSONObject(bodyString)
                UserProfile(
                    id = json.getString("id"),
                    firebaseUid = json.optString("firebaseUid").takeIf { s -> s.isNotBlank() },
                    email = json.getString("email"),
                    displayName = json.optString("displayName").takeIf { s -> s.isNotBlank() && s != "null" },
                    photoUrl = json.optString("photoUrl").takeIf { s -> s.isNotBlank() && s != "null" },
                    createdAt = json.optString("createdAt").takeIf { s -> s.isNotBlank() }
                )
            }
        }
    }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonBody = JSONObject().apply {
                if (displayName != null) put("displayName", displayName)
                if (photoUrl != null) put("photoUrl", photoUrl)
            }.toString()

            val response = httpClient.execute(
                url = "$baseUrl/api/v1/me",
                method = "PATCH",
                jsonBody = jsonBody
            )
            response.use {
                if (!it.isSuccessful) {
                    throw IllegalStateException("Loi khi cap nhat thong tin: HTTP ${it.code}")
                }
                val bodyString = it.body?.string().orEmpty()
                val json = JSONObject(bodyString)
                UserProfile(
                    id = json.getString("id"),
                    firebaseUid = json.optString("firebaseUid").takeIf { s -> s.isNotBlank() },
                    email = json.getString("email"),
                    displayName = json.optString("displayName").takeIf { s -> s.isNotBlank() && s != "null" },
                    photoUrl = json.optString("photoUrl").takeIf { s -> s.isNotBlank() && s != "null" },
                    createdAt = json.optString("createdAt").takeIf { s -> s.isNotBlank() }
                )
            }
        }
    }

    override suspend fun deleteProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = httpClient.execute(
                url = "$baseUrl/api/v1/me",
                method = "DELETE"
            )
            response.use {
                if (!it.isSuccessful) {
                    throw IllegalStateException("Loi khi xoa ho so nguoi dung: HTTP ${it.code}")
                }
                Unit
            }
        }
    }
}
