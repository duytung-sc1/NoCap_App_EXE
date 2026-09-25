package com.nocap.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.nocap.app.BuildConfig
import com.nocap.app.domain.model.AuthState
import com.nocap.app.domain.model.AuthUser
import com.nocap.app.domain.repository.AuthRepository
import com.nocap.app.domain.repository.AuthTokenProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CloudAuthRepository private constructor(context: Context) : AuthRepository, AuthTokenProvider {
    private val authMutex = Mutex()
    private suspend fun <T> serialized(block: () -> T): T = withContext(Dispatchers.IO) { authMutex.withLock { block() } }
    private val prefs = context.getSharedPreferences("cloud_auth", Context.MODE_PRIVATE)
    private val client = OkHttpClient()
    private val state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = state
    @Volatile private var token: String? = null
    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
          // Startup restoration must not publish an old account after logout/login.
          serialized {
            runCatching { prefs.getString("session", null)?.let { stored ->
                val data = JSONObject(decrypt(stored))
                if (data.getLong("expiresAt") > System.currentTimeMillis() / 1000) {
                    token = data.getString("token"); publish(data.getJSONObject("user"))
                }
            } }
            if (token == null) clear() else runCatching {
                publish(request("/api/v1/auth/user", "GET").getJSONObject("user"))
            }
          }
        }
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("nocap_cloud_session", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("nocap_cloud_session", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun encrypt(text: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(text.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    private fun decrypt(text: String): String {
        val bytes = Base64.decode(text, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }
    private fun publish(user: JSONObject): AuthUser {
        fun optional(key: String) = user.optString(key).takeIf { it.isNotBlank() && it != "null" }
        val value = AuthUser(user.getString("id"), optional("email"), optional("displayName"), optional("photoUrl"), user.optBoolean("emailVerified"), "cloudflare")
        com.nocap.app.data.sync.Profiles.select("ACCOUNT:${value.uid}")
        state.value = if (value.isEmailVerified) AuthState.Authenticated(value) else AuthState.RequiresEmailVerification(value)
        return value
    }
    private fun clear() { token = null; prefs.edit().clear().commit(); com.nocap.app.data.sync.Profiles.select(com.nocap.app.data.sync.Profiles.LOCAL); state.value = AuthState.Guest }
    private fun request(path: String, method: String = "POST", data: JSONObject = JSONObject()): JSONObject {
        val builder = Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}$path")
            .method(method, if (method == "GET" || method == "DELETE") null else data.toString().toRequestBody("application/json".toMediaType()))
        token?.let { builder.header("Authorization", "Bearer $it") }
        client.newCall(builder.build()).execute().use { response ->
            val result = JSONObject(response.body?.string().orEmpty().ifBlank { "{}" })
            if (!response.isSuccessful) {
                val code = result.optJSONObject("error")?.optString("code")
                if (code == "UNAUTHORIZED") clear()
                throw IllegalStateException(result.optJSONObject("error")?.optString("message") ?: "Lỗi máy chủ (${response.code})")
            }
            return result
        }
    }
    private suspend fun login(path: String, data: JSONObject): Result<AuthUser> = serialized {
        runCatching {
            val result = request("/api/v1/auth/$path", data = data)
            check(prefs.edit().putString("session", encrypt(result.toString())).commit()) { "Không lưu được phiên đăng nhập" }
            token = result.getString("token")
            publish(result.getJSONObject("user"))
        }
    }
    override suspend fun registerWithEmail(email: String, password: String) = login("register", JSONObject().put("email", email).put("password", password))
    override suspend fun loginWithEmail(email: String, password: String) = login("login", JSONObject().put("email", email).put("password", password))
    override suspend fun signInWithGoogle(idToken: String) = login("google", JSONObject().put("idToken", idToken))
    override suspend fun sendEmailVerification(): Result<Unit> = serialized { runCatching { request("/api/v1/auth/send-verification"); Unit } }
    override suspend fun sendPasswordReset(email: String): Result<Unit> = serialized { runCatching { request("/api/v1/auth/forgot-password", data = JSONObject().put("email", email)); Unit } }
    override suspend fun reloadUser(): Result<AuthState> = serialized { runCatching { publish(request("/api/v1/auth/user", "GET").getJSONObject("user")); state.value } }
    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit> = serialized {
        runCatching { publish(request("/api/v1/me", "PATCH", JSONObject().apply { displayName?.let { put("displayName", it) }; photoUrl?.let { put("photoUrl", it) } })); Unit }
    }
    override suspend fun signOut() = serialized { runCatching { if (token != null) request("/api/v1/auth/logout") }; clear() }
    override suspend fun deleteAccount(): Result<Unit> = serialized { runCatching { request("/api/v1/me", "DELETE"); clear() } }
    override fun continueAsGuest() { CoroutineScope(Dispatchers.IO).launch { runCatching { signOut() } } }
    override suspend fun getIdToken(forceRefresh: Boolean): String? = authMutex.withLock { token }
    /** Clears only the session that produced an authentication failure.
     * A delayed callback from an older socket must not sign out a newer login. */
    internal suspend fun invalidateSession(expectedToken: String) = serialized {
        if (token == expectedToken) clear()
    }
    companion object {
        @Volatile private var instance: CloudAuthRepository? = null
        fun getInstance(context: Context): CloudAuthRepository = instance ?: synchronized(this) { instance ?: CloudAuthRepository(context.applicationContext).also { instance = it } }
    }
}
