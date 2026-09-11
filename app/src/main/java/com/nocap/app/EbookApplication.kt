package com.nocap.app

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class EbookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.nocap.app.core.localization.AppLanguageManager.applyDefault(this)
        com.nocap.app.data.catalog.CloudCatalog.start(this)
        com.nocap.app.data.auth.CloudAuthRepository.getInstance(this)
        com.nocap.app.data.billing.EntitlementRepository.get(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            com.nocap.app.data.auth.CloudAuthRepository.getInstance(this@EbookApplication).authState.collect { auth ->
                if(auth != com.nocap.app.domain.model.AuthState.Loading) {
                    com.nocap.app.data.sync.SyncScheduler.start(this@EbookApplication, com.nocap.app.data.sync.Profiles.active.value)
                }
            }
        }
        com.nocap.app.domain.session.ReadingSessionManager.getInstance(this).recoverStaleSessionsAsync()
    }
}
