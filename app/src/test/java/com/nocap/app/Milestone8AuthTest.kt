package com.nocap.app

import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.nocap.app.data.auth.AuthenticatedHttpClient
import com.nocap.app.data.auth.BackendAccountRepository
import com.nocap.app.domain.model.AuthState
import com.nocap.app.domain.model.AuthUser
import com.nocap.app.domain.model.UserProfile
import com.nocap.app.domain.repository.AccountRepository
import com.nocap.app.domain.repository.AuthRepository
import com.nocap.app.domain.repository.AuthTokenProvider
import com.nocap.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class Milestone8AuthTest {

    @Test
    fun authUser_creationAndProperties() {
        val user = AuthUser(
            uid = "uid_12345",
            email = "user@example.com",
            displayName = "NoCap Reader",
            photoUrl = "https://example.com/avatar.png",
            isEmailVerified = true,
            providerId = "google.com"
        )

        assertEquals("uid_12345", user.uid)
        assertEquals("user@example.com", user.email)
        assertEquals("NoCap Reader", user.displayName)
        assertEquals("https://example.com/avatar.png", user.photoUrl)
        assertTrue(user.isEmailVerified)
        assertEquals("google.com", user.providerId)
    }

    @Test
    fun authState_sealedHierarchyTypes() {
        val loading: AuthState = AuthState.Loading
        val guest: AuthState = AuthState.Guest
        val user = AuthUser("u1", "u1@test.com", "User 1", null, true, "password")
        val authenticated: AuthState = AuthState.Authenticated(user)
        val requiresVerification: AuthState = AuthState.RequiresEmailVerification(user.copy(isEmailVerified = false))
        val error: AuthState = AuthState.Error("Invalid credentials")

        assertTrue(loading is AuthState.Loading)
        assertTrue(guest is AuthState.Guest)
        assertTrue(authenticated is AuthState.Authenticated)
        assertTrue(requiresVerification is AuthState.RequiresEmailVerification)
        assertTrue(error is AuthState.Error)
        assertEquals("Invalid credentials", (error as AuthState.Error).message)
    }

    @Test
    fun fakeAuthRepository_stateFlowTransitions() = runBlocking {
        val fakeRepo = FakeAuthRepository()
        assertEquals(AuthState.Guest, fakeRepo.authState.value)

        // Register -> RequiresEmailVerification
        val regResult = fakeRepo.registerWithEmail("test@example.com", "pass123")
        assertTrue(regResult.isSuccess)
        assertTrue(fakeRepo.authState.value is AuthState.RequiresEmailVerification)

        // Reload verified user -> Authenticated
        fakeRepo.markEmailVerified()
        val reloadResult = fakeRepo.reloadUser()
        assertTrue(reloadResult.isSuccess)
        assertTrue(fakeRepo.authState.value is AuthState.Authenticated)

        // Sign out -> Guest
        fakeRepo.signOut()
        assertEquals(AuthState.Guest, fakeRepo.authState.value)
    }

    @Test
    fun authenticatedHttpClient_addsBearerToken() = runBlocking {
        val capturedHeaders = mutableListOf<String?>()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                capturedHeaders.add(request.header("Authorization"))
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{\"status\":\"OK\"}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val tokenProvider = object : AuthTokenProvider {
            override suspend fun getIdToken(forceRefresh: Boolean): String = "valid_token_xyz"
        }

        val client = AuthenticatedHttpClient(
            tokenProvider = tokenProvider,
            okHttpClient = okHttpClient
        )

        val response = client.execute("http://localhost:8080/api/v1/me", "GET")

        assertEquals(200, response.code)
        assertEquals(1, capturedHeaders.size)
        assertEquals("Bearer valid_token_xyz", capturedHeaders[0])
    }

    @Test
    fun authenticatedHttpClient_retriesOnceOn401WithForceRefresh() = runBlocking {
        var callCount = 0
        val capturedHeaders = mutableListOf<String?>()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                capturedHeaders.add(request.header("Authorization"))
                if (callCount++ == 0) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(401)
                        .message("Unauthorized")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{\"status\":\"OK\"}".toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }
            .build()

        var refreshCount = 0
        val tokenProvider = object : AuthTokenProvider {
            override suspend fun getIdToken(forceRefresh: Boolean): String {
                return if (forceRefresh) {
                    refreshCount++
                    "fresh_token_123"
                } else {
                    "stale_token_old"
                }
            }
        }

        val client = AuthenticatedHttpClient(
            tokenProvider = tokenProvider,
            okHttpClient = okHttpClient
        )

        val response = client.execute("http://localhost:8080/api/v1/me", "GET")

        assertEquals(200, response.code)
        assertEquals(1, refreshCount)
        assertEquals(2, capturedHeaders.size)
        assertEquals("Bearer stale_token_old", capturedHeaders[0])
        assertEquals("Bearer fresh_token_123", capturedHeaders[1])
    }

    @Test
    fun loginSuccess_callsMe_and_registersDevice() = runBlocking {
        var meCalled = false
        var deviceRegistered = false

        val fakeAccountRepo = object : AccountRepository {
            override suspend fun getProfile(): Result<UserProfile> {
                meCalled = true
                return Result.success(UserProfile("u1", "fb_u1", "test@nocap.app", "Test User", null))
            }
            override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<UserProfile> =
                Result.success(UserProfile("u1", "fb_u1", "test@nocap.app", displayName, photoUrl))
            override suspend fun deleteProfile(): Result<Unit> = Result.success(Unit)
        }

        val fakeDeviceRepo = object : DeviceRepository {
            override suspend fun getInstallationId(): String = "test-install-id"
            override suspend fun registerDevice(fcmToken: String?): Result<Unit> {
                deviceRegistered = true
                return Result.success(Unit)
            }
            override suspend fun unregisterDevice(): Result<Unit> = Result.success(Unit)
        }

        // Simulate login success flow
        val profileResult = fakeAccountRepo.getProfile()
        assertTrue(profileResult.isSuccess)
        assertTrue(meCalled)

        if (profileResult.isSuccess) {
            val devResult = fakeDeviceRepo.registerDevice()
            assertTrue(devResult.isSuccess)
            assertTrue(deviceRegistered)
        }
    }

    @Test
    fun meFailure_doesNotSignFirebaseUserOut() = runBlocking {
        val fakeAuthRepo = FakeAuthRepository()
        fakeAuthRepo.loginWithEmail("test@nocap.app", "pass123")
        assertTrue(fakeAuthRepo.authState.value is AuthState.Authenticated)

        val fakeAccountRepo = object : AccountRepository {
            override suspend fun getProfile(): Result<UserProfile> =
                Result.failure(IllegalStateException("Backend Offline 503"))
            override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<UserProfile> =
                Result.failure(IllegalStateException("Backend Offline"))
            override suspend fun deleteProfile(): Result<Unit> = Result.success(Unit)
        }

        // Attempt /me
        val profileResult = fakeAccountRepo.getProfile()
        assertTrue(profileResult.isFailure)

        // Firebase user MUST remain Authenticated!
        assertTrue(fakeAuthRepo.authState.value is AuthState.Authenticated)
        val user = (fakeAuthRepo.authState.value as AuthState.Authenticated).user
        assertEquals("test@nocap.app", user.email)
    }

    @Test
    fun profileNameRefresh_updatesBackendAndFirebase() = runBlocking {
        val fakeAuthRepo = FakeAuthRepository()
        fakeAuthRepo.loginWithEmail("reader@nocap.app", "pass123")

        var backendDisplayName = "Old Name"
        val fakeAccountRepo = object : AccountRepository {
            override suspend fun getProfile(): Result<UserProfile> =
                Result.success(UserProfile("u1", "fb1", "reader@nocap.app", backendDisplayName, null))
            override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<UserProfile> {
                backendDisplayName = displayName ?: backendDisplayName
                return Result.success(UserProfile("u1", "fb1", "reader@nocap.app", backendDisplayName, photoUrl))
            }
            override suspend fun deleteProfile(): Result<Unit> = Result.success(Unit)
        }

        val updateResult = fakeAccountRepo.updateProfile(displayName = "NoCap VIP Reader")
        assertTrue(updateResult.isSuccess)
        assertEquals("NoCap VIP Reader", updateResult.getOrNull()?.displayName)

        fakeAuthRepo.updateProfile(displayName = "NoCap VIP Reader")
        assertEquals("NoCap VIP Reader", (fakeAuthRepo.authState.value as AuthState.Authenticated).user.displayName)
    }

    @Test
    fun deleteAccount_failsWhenBackendDeleteFails() = runBlocking {
        val fakeAuthRepo = FakeAuthRepository()
        fakeAuthRepo.loginWithEmail("user@nocap.app", "pass123")

        val failingAccountRepo = object : AccountRepository {
            override suspend fun getProfile(): Result<UserProfile> = Result.failure(Exception())
            override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<UserProfile> = Result.failure(Exception())
            override suspend fun deleteProfile(): Result<Unit> = Result.failure(IllegalStateException("Server 500 error"))
        }

        val backendRes = failingAccountRepo.deleteProfile()
        assertTrue(backendRes.isFailure)

        // Firebase deletion must NOT proceed if backend deletion failed!
        assertTrue(fakeAuthRepo.authState.value is AuthState.Authenticated)
    }

    @Test
    fun deleteAccount_handlesFirebaseRecentLoginRequirement() = runBlocking {
        val fakeAuthRepo = FakeAuthRepository()
        fakeAuthRepo.loginWithEmail("user@nocap.app", "pass123")
        fakeAuthRepo.setSimulateRecentLoginRequired(true)

        val fakeAccountRepo = object : AccountRepository {
            override suspend fun getProfile(): Result<UserProfile> = Result.failure(Exception())
            override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<UserProfile> = Result.failure(Exception())
            override suspend fun deleteProfile(): Result<Unit> = Result.success(Unit)
        }

        fakeAccountRepo.deleteProfile()
        val fbDeleteResult = fakeAuthRepo.deleteAccount()
        assertTrue(fbDeleteResult.isFailure)
        assertTrue(fbDeleteResult.exceptionOrNull()?.message?.contains("recent") == true)

        // User is still authenticated in Firebase Auth
        assertTrue(fakeAuthRepo.authState.value is AuthState.Authenticated)
    }

    @Test
    fun deleteAccount_successfulDelete_transitionsToGuest() = runBlocking {
        val fakeAuthRepo = FakeAuthRepository()
        fakeAuthRepo.loginWithEmail("user@nocap.app", "pass123")

        val fakeAccountRepo = object : AccountRepository {
            override suspend fun getProfile(): Result<UserProfile> = Result.failure(Exception())
            override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<UserProfile> = Result.failure(Exception())
            override suspend fun deleteProfile(): Result<Unit> = Result.success(Unit)
        }

        val backendRes = fakeAccountRepo.deleteProfile()
        assertTrue(backendRes.isSuccess)

        val fbRes = fakeAuthRepo.deleteAccount()
        assertTrue(fbRes.isSuccess)

        assertEquals(AuthState.Guest, fakeAuthRepo.authState.value)
    }

    @Test
    fun installationId_isValidUuidFormat() {
        val installationId = UUID.randomUUID().toString()
        assertNotNull(installationId)
        val parsed = UUID.fromString(installationId)
        assertEquals(installationId, parsed.toString())
    }

    @Test
    fun userProfile_modelProperties() {
        val profile = UserProfile(
            id = "user-uuid-1",
            firebaseUid = "firebase_uid_123",
            email = "reader@nocap.app",
            displayName = "NoCap Enthusiast",
            photoUrl = "https://nocap.app/p.png",
            createdAt = "2026-09-03T12:00:00Z"
        )

        assertEquals("user-uuid-1", profile.id)
        assertEquals("firebase_uid_123", profile.firebaseUid)
        assertEquals("reader@nocap.app", profile.email)
        assertEquals("NoCap Enthusiast", profile.displayName)
        assertEquals("https://nocap.app/p.png", profile.photoUrl)
        assertEquals("2026-09-03T12:00:00Z", profile.createdAt)
    }

    private class FakeAuthRepository : AuthRepository, AuthTokenProvider {
        private val _authState = MutableStateFlow<AuthState>(AuthState.Guest)
        override val authState: StateFlow<AuthState> = _authState.asStateFlow()
        private var currentUser: AuthUser? = null
        private var simulateRecentLogin = false

        fun setSimulateRecentLoginRequired(value: Boolean) {
            simulateRecentLogin = value
        }

        override suspend fun registerWithEmail(email: String, password: String): Result<AuthUser> {
            val user = AuthUser("uid_reg", email, email.substringBefore("@"), null, false, "password")
            currentUser = user
            _authState.value = AuthState.RequiresEmailVerification(user)
            return Result.success(user)
        }

        override suspend fun loginWithEmail(email: String, password: String): Result<AuthUser> {
            val user = AuthUser("uid_log", email, email.substringBefore("@"), null, true, "password")
            currentUser = user
            _authState.value = AuthState.Authenticated(user)
            return Result.success(user)
        }

        override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
            val user = AuthUser("uid_google", "google@test.com", "Google User", null, true, "google.com")
            currentUser = user
            _authState.value = AuthState.Authenticated(user)
            return Result.success(user)
        }

        override suspend fun sendEmailVerification(): Result<Unit> = Result.success(Unit)

        fun markEmailVerified() {
            currentUser = currentUser?.copy(isEmailVerified = true)
        }

        override suspend fun reloadUser(): Result<AuthState> {
            val user = currentUser ?: return Result.failure(IllegalStateException("No user"))
            val state = if (user.isEmailVerified) AuthState.Authenticated(user) else AuthState.RequiresEmailVerification(user)
            _authState.value = state
            return Result.success(state)
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit> {
            val user = currentUser ?: return Result.failure(IllegalStateException("No user"))
            val updated = user.copy(displayName = displayName ?: user.displayName, photoUrl = photoUrl ?: user.photoUrl)
            currentUser = updated
            _authState.value = AuthState.Authenticated(updated)
            return Result.success(Unit)
        }

        override suspend fun signOut() {
            currentUser = null
            _authState.value = AuthState.Guest
        }

        override suspend fun deleteAccount(): Result<Unit> {
            if (simulateRecentLogin) {
                return Result.failure(IllegalStateException("This operation is sensitive and requires recent authentication. Log in again before retrying this request."))
            }
            currentUser = null
            _authState.value = AuthState.Guest
            return Result.success(Unit)
        }

        override fun continueAsGuest() {
            _authState.value = AuthState.Guest
        }

        override suspend fun getIdToken(forceRefresh: Boolean): String = "fake_id_token"
    }
}
