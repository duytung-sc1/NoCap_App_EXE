package com.nocap.app.domain.repository

interface DeviceRepository {
    suspend fun getInstallationId(): String
    suspend fun registerDevice(fcmToken: String? = null): Result<Unit>
    suspend fun unregisterDevice(): Result<Unit>
}

