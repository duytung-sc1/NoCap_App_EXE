package com.nocap.app.data.auth

import android.os.Build
import com.nocap.app.BuildConfig
import com.nocap.app.core.datastore.DevicePreferencesDataStore
import com.nocap.app.domain.repository.AuthTokenProvider
import com.nocap.app.domain.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LocalDeviceRepository(
    private val devicePreferencesDataStore: DevicePreferencesDataStore,
    tokenProvider: AuthTokenProvider,
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    private val appVersion: String = "1.0.0"
) : DeviceRepository {

    private val httpClient = AuthenticatedHttpClient(tokenProvider)

    override suspend fun getInstallationId(): String {
        return devicePreferencesDataStore.getOrCreateInstallationId()
    }

    override suspend fun registerDevice(fcmToken: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val installationId = getInstallationId()
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

            val jsonBody = JSONObject().apply {
                put("platform", "android")
                put("appVersion", appVersion)
                put("deviceModel", if (deviceModel.isNotBlank()) deviceModel else "Android Device")
                if (!fcmToken.isNullOrBlank()) put("fcmToken", fcmToken)
            }.toString()

            val response = httpClient.execute(
                url = "$baseUrl/api/v1/devices/$installationId",
                method = "PUT",
                jsonBody = jsonBody
            )
            response.use {
                if (!it.isSuccessful) {
                    throw IllegalStateException("Loi khi dang ky thiet bi: HTTP ${it.code}")
                }
                Unit
            }
        }
    }

    override suspend fun unregisterDevice(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val installationId = getInstallationId()
            val response = httpClient.execute(
                url = "$baseUrl/api/v1/devices/$installationId",
                method = "DELETE"
            )
            response.use {
                if (!it.isSuccessful && it.code != 404) {
                    throw IllegalStateException("Loi khi huy dang ky thiet bi: HTTP ${it.code}")
                }
                Unit
            }
        }
    }
}
