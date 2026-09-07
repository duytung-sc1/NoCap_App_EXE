package com.nocap.app

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.nocap.app.core.navigation.AppNavHost
import com.nocap.app.domain.model.PublicationSource
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : FragmentActivity() {

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
        handleIntent(intent)

        setContent {
            EbookAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val sharedSource by pendingSharedSource.collectAsStateWithLifecycle()
                    AppNavHost(
                        pendingImportSource = sharedSource,
                        onClearPendingImport = { pendingSharedSource.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri != null) {
                pendingSharedSource.value = PublicationSource.SharedUri(
                    uri = uri,
                    mimeType = intent.type
                )
                intent.removeExtra(Intent.EXTRA_STREAM)
            }
        } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                val url = extractHttpsUrl(text)
                if (url != null) {
                    pendingSharedSource.value = PublicationSource.SharedUrl(url)
                    intent.removeExtra(Intent.EXTRA_TEXT)
                }
            }
        }
    }

    private fun extractHttpsUrl(text: String): String? {
        val regex = Regex("""https://[^\s]+""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.value
    }
}
