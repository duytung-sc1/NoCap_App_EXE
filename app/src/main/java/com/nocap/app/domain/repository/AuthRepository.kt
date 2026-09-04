package com.nocap.app.domain.repository

import com.nocap.app.domain.model.AuthState
import com.nocap.app.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun loginWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadUser(): Result<AuthState>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun updateProfile(displayName: String?, photoUrl: String? = null): Result<Unit>
    suspend fun signOut()
    suspend fun deleteAccount(): Result<Unit>
    fun continueAsGuest()
}
