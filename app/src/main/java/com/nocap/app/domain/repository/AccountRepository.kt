package com.nocap.app.domain.repository

import com.nocap.app.domain.model.UserProfile

interface AccountRepository {
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(displayName: String?, photoUrl: String? = null): Result<UserProfile>
    suspend fun deleteProfile(): Result<Unit>
}

