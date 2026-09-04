package com.nocap.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.nocap.app.domain.model.AuthState
import com.nocap.app.domain.model.AuthUser
import com.nocap.app.domain.repository.AuthRepository
import com.nocap.app.domain.repository.AuthTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository, AuthTokenProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _authState.value = mapFirebaseUserToState(user)
    }

    init {
        auth.addAuthStateListener(authStateListener)
        // Initial resolution
        _authState.value = mapFirebaseUserToState(auth.currentUser)
    }

    private fun mapFirebaseUserToState(user: FirebaseUser?): AuthState {
        if (user == null) {
            return AuthState.Guest
        }
        val authUser = user.toAuthUser()
        val isPasswordProvider = authUser.providerId == "password"
        return if (isPasswordProvider && !user.isEmailVerified) {
            AuthState.RequiresEmailVerification(authUser)
        } else {
            AuthState.Authenticated(authUser)
        }
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Không nhận được thông tin người dùng từ Firebase")
            runCatching { user.sendEmailVerification().await() }
            user.toAuthUser()
        }
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Không nhận được thông tin người dùng từ Firebase")
            user.toAuthUser()
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw IllegalStateException("Không nhận được thông tin người dùng từ Google")
            user.toAuthUser()
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser ?: throw IllegalStateException("Chưa đăng nhập")
            user.sendEmailVerification().await()
            Unit
        }
    }

    override suspend fun reloadUser(): Result<AuthState> = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser ?: throw IllegalStateException("Chưa đăng nhập")
            user.reload().await()
            val newState = mapFirebaseUserToState(auth.currentUser)
            _authState.value = newState
            newState
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.sendPasswordResetEmail(email.trim()).await()
            Unit
        }
    }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser ?: throw IllegalStateException("Chưa đăng nhập")
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                if (displayName != null) setDisplayName(displayName)
                if (photoUrl != null) setPhotoUri(android.net.Uri.parse(photoUrl))
            }.build()
            user.updateProfile(profileUpdates).await()
            // Reload user state to reflect change
            user.reload().await()
            _authState.value = mapFirebaseUserToState(auth.currentUser)
            Unit
        }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        auth.signOut()
        _authState.value = AuthState.Guest
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser ?: throw IllegalStateException("Chưa đăng nhập")
            user.delete().await()
            _authState.value = AuthState.Guest
            Unit
        }
    }

    override fun continueAsGuest() {
        _authState.value = AuthState.Guest
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String? = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser ?: return@withContext null
            user.getIdToken(forceRefresh).await()?.token
        }.getOrNull()
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        val nonFirebaseProvider = providerData.firstOrNull { it.providerId != "firebase" }?.providerId
        val effectiveProvider = nonFirebaseProvider ?: "password"
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName ?: email?.substringBefore("@"),
            photoUrl = photoUrl?.toString(),
            isEmailVerified = isEmailVerified,
            providerId = effectiveProvider
        )
    }
}
