package com.nocap.app.domain.model

data class UserProfile(
    val id: String,
    val firebaseUid: String? = null,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: String? = null
)
