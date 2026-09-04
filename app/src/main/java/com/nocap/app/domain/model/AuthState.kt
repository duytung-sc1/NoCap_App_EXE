package com.nocap.app.domain.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Guest : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data class RequiresEmailVerification(val user: AuthUser) : AuthState
    data class Error(val message: String) : AuthState
}

