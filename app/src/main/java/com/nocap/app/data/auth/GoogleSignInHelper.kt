package com.nocap.app.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSignInHelper {

    suspend fun getGoogleIdToken(context: Context, serverClientId: String): Result<String> = withContext(Dispatchers.Main) {
        runCatching {
            val activityContext = context.findActivity() ?: context
            val credentialManager = CredentialManager.create(activityContext)

            // This helper is called from an explicit "Sign in with Google" button. The
            // dedicated option always presents Google's account-selection flow instead
            // of silently reusing an opaque saved credential.
            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleCredential.idToken
            } else {
                throw IllegalStateException("Loại xác thực không được hỗ trợ: ${credential.type}")
            }
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
