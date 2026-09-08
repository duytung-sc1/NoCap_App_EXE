package com.nocap.app

import android.app.Application

class EbookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi-VN"))
        com.nocap.app.data.catalog.CloudCatalog.start(this)
        com.nocap.app.data.auth.CloudAuthRepository.getInstance(this)
        com.nocap.app.domain.session.ReadingSessionManager.getInstance(this).recoverStaleSessionsAsync()
    }
}
