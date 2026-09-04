package com.nocap.app.presentation.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isServerUnavailable: Boolean = false,
    val serverStatusMessage: String? = null
)
