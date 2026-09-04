package com.nocap.app.domain.repository

interface AuthTokenProvider {
    suspend fun getIdToken(forceRefresh: Boolean = false): String?
}

