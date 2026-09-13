package com.nocap.app

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nocap.app.domain.model.PublicationSource
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPublicationIntentTest {
    private val uri = Uri.parse("content://nocap.test/document/sample.docx")

    @Test fun completedViewDoesNotReplayButPendingViewCanResume() {
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(uri, (readSharedPublicationSource(Intent(intent)) as PublicationSource.SharedUri).uri)
        intent.markSharedImportHandled()
        assertNull(readSharedPublicationSource(Intent(intent)))
        assertEquals(uri, intent.data)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test fun malformedExtrasDoNotCrash() {
        val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, 42)
            .putExtra(Intent.EXTRA_TEXT, 42)
        assertNull(readSharedPublicationSource(intent))
        intent.putExtra(Intent.EXTRA_STREAM, android.os.Bundle())
        assertNull(readSharedPublicationSource(intent))
    }

    @Test fun uriAndLegacyStringSharesStillWork() {
        for (intent in listOf(Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri),
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri.toString()))) {
            assertEquals(uri, (readSharedPublicationSource(intent) as PublicationSource.SharedUri).uri)
            intent.markSharedImportHandled()
            assertNull(readSharedPublicationSource(intent))
        }
    }

    @Test fun viewUsesItsDataAndTextSharesStillWork() {
        val view = Intent(Intent.ACTION_VIEW, uri).putExtra(Intent.EXTRA_STREAM, Uri.parse("content://other/file"))
        assertEquals(uri, (readSharedPublicationSource(view) as PublicationSource.SharedUri).uri)
        val text = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "Read https://example.com/book.epub")
        assertEquals(PublicationSource.SharedUrl("https://example.com/book.epub"), readSharedPublicationSource(text))
        assertNull(readSharedPublicationSource(Intent(Intent.ACTION_MAIN, uri)))
    }
}
