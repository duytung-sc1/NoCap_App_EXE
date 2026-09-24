package com.nocap.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocap.app.core.designsystem.EbookAppTheme
import com.nocap.app.core.localization.AppLanguageManager
import com.nocap.app.core.navigation.AppNavHost
import com.nocap.app.domain.model.PublicationSource
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : FragmentActivity() {
    private var sharedImportHandled = false

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    private val pendingSharedSource = MutableStateFlow<PublicationSource?>(null)
    var volumeKeyListener: ((Int) -> Boolean)? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (volumeKeyListener?.invoke(keyCode) == true) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedImportHandled = savedInstanceState?.getBoolean("shared_import_handled") ?: false
        if (!sharedImportHandled) handleIntent(intent)

        setContent {
            val auth by com.nocap.app.data.auth.CloudAuthRepository.getInstance(this).authState.collectAsStateWithLifecycle()
            val profile by com.nocap.app.data.sync.Profiles.active.collectAsStateWithLifecycle()
            EbookAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val sharedSource by pendingSharedSource.collectAsStateWithLifecycle()
                    if (auth == com.nocap.app.domain.model.AuthState.Loading) {
                        androidx.compose.material3.CircularProgressIndicator()
                    } else androidx.compose.runtime.key(profile) {
                        AppNavHost(
                            pendingImportSource = sharedSource,
                            onClearPendingImport = {
                                pendingSharedSource.value = null
                                sharedImportHandled = true
                                intent?.markSharedImportHandled()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("shared_import_handled", sharedImportHandled)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        com.nocap.app.data.billing.PlayBilling.get(this).connect()
        com.nocap.app.data.sync.SyncScheduler.onForeground(this)
    }

    override fun onStop() {
        super.onStop()
        com.nocap.app.data.sync.SyncScheduler.onBackground()
    }

    private fun handleIntent(intent: Intent?) {
        readSharedPublicationSource(intent)?.let {
            sharedImportHandled = false
            pendingSharedSource.value = it
        }
    }
}
