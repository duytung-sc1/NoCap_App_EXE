package com.nocap.app.data.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.security.MessageDigest

object Profiles {
    const val LOCAL = "DEVICE_LOCAL"
    private val selected = MutableStateFlow(LOCAL)
    val active: StateFlow<String> = selected
    fun select(profile: String) { require(profile == LOCAL || profile.startsWith("ACCOUNT:")); selected.value = profile }
    fun key(profile: String): String = MessageDigest.getInstance("SHA-256").digest(profile.toByteArray()).joinToString("") { "%02x".format(it) }
    fun databaseName(profile: String) = if (profile == LOCAL) "ebook_reader.db" else "profile_${key(profile)}.db"
    fun files(context: Context, profile: String = active.value): File =
        if (profile == LOCAL) context.filesDir else File(context.filesDir, "profiles/${key(profile)}").apply { mkdirs() }
}
