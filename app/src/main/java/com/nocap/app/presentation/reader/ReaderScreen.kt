package com.nocap.app.presentation.reader

import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.nocap.app.core.designsystem.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton

import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.nocap.app.core.localization.Text
import com.nocap.app.core.localization.localize
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocap.app.MainActivity
import com.nocap.app.core.database.entity.CustomFontEntity
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.core.datastore.ReaderFontFamily
import com.nocap.app.core.datastore.ReaderOrientation
import com.nocap.app.core.datastore.ReaderPreferences
import com.nocap.app.core.datastore.ReaderTextAlignment
import com.nocap.app.core.datastore.ReaderTheme
import com.nocap.app.domain.model.Bookmark
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.TocItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.json.JSONObject
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.AbsoluteUrl
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nocap.app.presentation.reader.archive.ImageArchiveReader
import com.nocap.app.presentation.reader.image.ImageDocumentReader
import com.nocap.app.presentation.reader.text.TextDocumentReader

val HIGHLIGHT_YELLOW = Color(0xFFFFEB3B)
val HIGHLIGHT_GREEN = Color(0xFF81C784)
val HIGHLIGHT_BLUE = Color(0xFF64B5F6)
val HIGHLIGHT_PINK = Color(0xFFF06292)

fun getHighlightTint(colorName: String): Int {
    return when (colorName.uppercase()) {
        "GREEN" -> 0x664CAF50
        "BLUE" -> 0x662196F3
        "PINK" -> 0x66E91E63
        else -> 0x66FFEB3B
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    initialLocatorJson: String? = null,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.provideFactory(bookId, LocalContext.current, initialLocatorJson)
    )
) {

    val context = LocalContext.current
    val activity = context as? MainActivity
    val window = (context as? Activity)?.window

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is ReaderUiState.CustomFormatReady) {
        val customState = uiState as ReaderUiState.CustomFormatReady
        val customBook = customState.book
        when {
            customState.format.isTextBased -> {
                TextDocumentReader(
                    book = customBook,
                    file = customState.file,
                    initialLocatorJson = initialLocatorJson,
                    onBackClick = onBackClick
                )
            }

            customState.format.isSingleImage -> {
                ImageDocumentReader(
                    book = customBook,
                    file = customState.file,
                    onBackClick = onBackClick
                )
            }
            customState.format.isComicArchive -> {
                ImageArchiveReader(
                    book = customBook,
                    file = customState.file,
                    onBackClick = onBackClick
                )
            }
        }
        return
    }
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val customFonts by viewModel.customFonts.collectAsStateWithLifecycle()
    val isUsingBookOverride by viewModel.isUsingBookOverride.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isCurrentBookmarked.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchSupported by viewModel.isSearchSupported.collectAsStateWithLifecycle()

    var showTocSheet by remember { mutableStateOf(false) }
    var showAnnotationsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    var selectedHighlightForEdit by remember { mutableStateOf<HighlightEntity?>(null) }
    var pendingSelectionLocator by remember { mutableStateOf<Locator?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var showPdfTextExtractDialog by remember { mutableStateOf(false) }
    var pdfExtractedText by remember { mutableStateOf<String?>(null) }
    var pdfExtractPageIndex by remember { mutableIntStateOf(0) }
    var showPdfScannedAlert by remember { mutableStateOf(false) }
    var pdfExtractionError by remember { mutableStateOf<String?>(null) }

    var navigatorFragment by remember { mutableStateOf<VisualNavigator?>(null) }
    var lastKnownLocator by remember { mutableStateOf<Locator?>(null) }


    val (barBackground, barContentColor) = when (preferences.theme) {
        ReaderTheme.LIGHT -> Color(0xFFFFFFFF) to Color(0xFF1C1B1F)
        ReaderTheme.DARK -> Color(0xFF121212) to Color(0xFFE6E1E5)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
    }

    val handleBack: () -> Unit = {
        if (!showControls) {
            showControls = true
        } else {
            viewModel.saveCurrentLocationImmediately(lastKnownLocator ?: runCatching { navigatorFragment?.currentLocator?.value }.getOrNull())
            onBackClick()
        }
    }

    BackHandler(onBack = handleBack)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.endActiveSession()
        }
    }

    LaunchedEffect(preferences, navigatorFragment) {
        (navigatorFragment as? EpubNavigatorFragment)?.submitPreferences(preferences.toReadiumPreferences())
    }

    DisposableEffect(navigatorFragment) {
        val nav = navigatorFragment
        if (nav != null) {
            val inputListener = object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
                    val view = nav.publicationView
                    val width = view.width.toFloat()
                    val xRatio = if (width > 0f) event.point.x / width else 0.5f
                    if (xRatio in 0.2f..0.8f) {
                        showControls = !showControls
                        return true
                    }
                    return false
                }
            }
            nav.addInputListener(inputListener)
            onDispose {
                nav.removeInputListener(inputListener)
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(preferences.isFullscreen) {
        if (preferences.isFullscreen) {
            showControls = false
        }
    }

    DisposableEffect(preferences.volumeButtonsTurnPages, navigatorFragment) {
        if (preferences.volumeButtonsTurnPages) {
            activity?.volumeKeyListener = { keyCode ->
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        when (val nav = navigatorFragment) {
                            is EpubNavigatorFragment -> { nav.goBackward(animated = true); true }
                            is PdfNavigatorFragment<*, *> -> { nav.goBackward(animated = true); true }
                            else -> false
                        }
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        when (val nav = navigatorFragment) {
                            is EpubNavigatorFragment -> { nav.goForward(animated = true); true }
                            is PdfNavigatorFragment<*, *> -> { nav.goForward(animated = true); true }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
        } else {
            activity?.volumeKeyListener = null
        }
        onDispose {
            activity?.volumeKeyListener = null
        }
    }

    DisposableEffect(preferences.keepScreenOn) {
        if (preferences.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(preferences.orientation) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        when (preferences.orientation) {
            ReaderOrientation.FOLLOW_SYSTEM -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ReaderOrientation.PORTRAIT -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ReaderOrientation.LANDSCAPE -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    LaunchedEffect(preferences.brightnessFollowSystem, preferences.customBrightness) {
        val lp = window?.attributes
        if (lp != null) {
            lp.screenBrightness = if (preferences.brightnessFollowSystem) {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                preferences.customBrightness.coerceIn(0.05f, 1.0f)
            }
            window.attributes = lp
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val lp = window?.attributes
            if (lp != null) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(preferences.isFullscreen, showControls) {
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (preferences.isFullscreen && !showControls) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(highlights, navigatorFragment) {
        val epubNav = navigatorFragment as? EpubNavigatorFragment ?: return@LaunchedEffect
        val decorations = highlights.mapNotNull { h ->
            runCatching {
                val locator = Locator.fromJSON(JSONObject(h.locatorJson)) ?: return@mapNotNull null
                val tint = getHighlightTint(h.color)
                Decoration(
                    id = h.id,
                    locator = locator,
                    style = Decoration.Style.Highlight(tint = tint, isActive = false)
                )
            }.getOrNull()
        }
        epubNav.applyDecorations(decorations, "highlights")
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomFont(uri) { result ->
                result.onSuccess { font ->
                    Toast.makeText(context, com.nocap.app.core.localization.AppLanguageManager.translate(context, "Đã thêm phông chữ: ") + font.fontFamily, Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, com.nocap.app.core.localization.AppLanguageManager.translate(context, err.message ?: "Không thể thêm phông chữ"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (val s = uiState) {
                                is ReaderUiState.Ready -> s.bookTitle
                                else -> "Đọc sách"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = barContentColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = localize("Quay lại"),
                                tint = barContentColor
                            )
                        }
                    },
                    actions = {
                        if ((uiState as? ReaderUiState.Ready)?.format == PublicationFormat.PDF) {
                            IconButton(onClick = {
                                val pageIndex = (lastKnownLocator?.locations?.position?.minus(1) ?: 0).coerceAtLeast(0)
                                pdfExtractPageIndex = pageIndex
                                scope.launch {
                                    val result = viewModel.extractPdfPageText(pageIndex)
                                    pdfExtractionError = result?.errorMessage
                                        ?: if (result == null) "Không tìm thấy tệp PDF trên thiết bị. Hãy tải lại tài liệu rồi thử lại." else null
                                    if (result == null || result.errorMessage != null || result.text.isBlank()) {
                                        showPdfScannedAlert = true
                                    } else {
                                        pdfExtractedText = result.text
                                        showPdfTextExtractDialog = true
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = localize("Trích xuất văn bản"), tint = barContentColor)
                            }
                        }
                        IconButton(onClick = { showSearchSheet = true }) {
                            Icon(Icons.Default.Search, contentDescription = localize("Tìm kiếm trong sách"), tint = barContentColor)
                        }

                        IconButton(onClick = { showAnnotationsSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = localize("Đánh dấu & ghi chú"), tint = barContentColor)
                        }
                        IconButton(onClick = viewModel::toggleBookmark) {
                            Icon(
                                imageVector = if (isBookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Xóa dấu trang" else "Thêm dấu trang",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else barContentColor
                            )
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = localize("Cài đặt hiển thị"), tint = barContentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = barBackground)
                )
            }
        },
        bottomBar = {
            if (preferences.showReadingProgress || preferences.showPercentage || preferences.showClock) {
                ReaderChromeFooter(
                    visible = showControls || !preferences.isFullscreen,
                    preferences = preferences,
                    currentLocator = lastKnownLocator,
                    backgroundColor = barBackground,
                    contentColor = barContentColor
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(barBackground)
        ) {
            when (val state = uiState) {
                is ReaderUiState.CustomFormatReady -> {}
                is ReaderUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReaderUiState.BookNotDownloaded -> {
                    ReaderErrorState(
                        message = "Sách chưa được tải về máy.",
                        onBack = onBackClick
                    )
                }
                is ReaderUiState.FileNotFound -> {
                    ReaderErrorState(
                        message = "Tệp sách không tồn tại hoặc đã bị xóa khỏi thiết bị.",
                        onBack = onBackClick
                    )
                }
                is ReaderUiState.Error -> {
                    ReaderErrorState(
                        message = "Lỗi mở sách: " + state.message,
                        onBack = onBackClick,
                        onRetry = viewModel::loadPublication
                    )
                }
                is ReaderUiState.Ready -> {
                    if (state.format == PublicationFormat.PDF) {
                        PdfNavigatorContainer(
                            bookId = bookId,
                            publication = state.publication,
                            initialLocator = state.initialLocator,
                            onNavigatorReady = { nav ->
                                navigatorFragment = nav
                            }
                        )
                    } else {
                        EpubNavigatorContainer(
                            bookId = bookId,
                            publication = state.publication,
                            initialLocator = state.initialLocator,
                            initialPreferences = preferences.toReadiumPreferences(),
                            customFonts = customFonts,
                            onNavigatorReady = { nav ->
                                navigatorFragment = nav
                                nav.addDecorationListener("highlights", object : DecorableNavigator.Listener {
                                    override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                                        val match = highlights.find { it.id == event.decoration.id }
                                        if (match != null) {
                                            selectedHighlightForEdit = match
                                            return true
                                        }
                                        return false
                                    }
                                })
                            },
                            onHighlightRequested = { locator, colorHex ->
                                viewModel.addHighlight(locator, colorHex)
                            },
                            onNoteRequested = { locator ->
                                pendingSelectionLocator = locator
                                showAddNoteDialog = true
                            },
                            onCopyQuote = { quote ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Trích dẫn NoCap", quote))
                                Toast.makeText(context, com.nocap.app.core.localization.AppLanguageManager.translate(context, "Đã sao chép trích dẫn"), Toast.LENGTH_SHORT).show()
                            },
                            onShareQuote = { quote ->
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, quote)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ trích dẫn"))
                            }
                        )
                    }

                    LaunchedEffect(navigatorFragment) {
                        val nav = navigatorFragment ?: return@LaunchedEffect
                        val fragment = nav as? Fragment
                        if (fragment != null) {
                            while (!fragment.isAdded) {
                                delay(50)
                            }
                        }
                        try {
                            nav.currentLocator.collect { locator ->
                                lastKnownLocator = locator
                                viewModel.onLocationChanged(locator)
                            }
                        } catch (_: IllegalStateException) {}
                    }

                    if (showTocSheet) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showTocSheet = false },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            TocBottomSheetContent(
                                toc = state.toc,
                                onChapterSelected = { href ->
                                    showTocSheet = false
                                    Url(href)?.let { url ->
                                        state.publication.linkWithHref(url)?.let { link ->
                                            navigatorFragment?.go(link, animated = true)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (showAnnotationsSheet) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showAnnotationsSheet = false },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            AnnotationsBottomSheetContent(
                                bookmarks = bookmarks,
                                highlights = highlights,
                                onBookmarkSelected = { bookmark ->
                                    showAnnotationsSheet = false
                                    runCatching {
                                        Locator.fromJSON(JSONObject(bookmark.locatorJson))?.let { loc ->
                                            navigatorFragment?.go(loc, animated = true)
                                        }
                                    }
                                },
                                onHighlightSelected = { highlight ->
                                    showAnnotationsSheet = false
                                    runCatching {
                                        Locator.fromJSON(JSONObject(highlight.locatorJson))?.let { loc ->
                                            navigatorFragment?.go(loc, animated = true)
                                        }
                                    }
                                },
                                onDeleteBookmark = viewModel::removeBookmark,
                                onDeleteHighlight = viewModel::deleteHighlight,
                                onEditHighlight = { highlight ->
                                    showAnnotationsSheet = false
                                    selectedHighlightForEdit = highlight
                                },
                                onShareHighlight = { highlight ->
                                    val text = "\"" + highlight.text + "\"\n— " + state.bookTitle
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ"))
                                }
                            )
                        }
                    }

                    if (showSearchSheet) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = {
                                showSearchSheet = false
                                viewModel.clearSearch()
                            },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            InBookSearchSheetContent(
                                isSupported = isSearchSupported,
                                query = searchQuery,
                                isSearching = isSearching,
                                results = searchResults,
                                onQueryChange = { viewModel.onSearchQueryChanged(it, immediate = false) },
                                onClearQuery = viewModel::clearSearch,
                                onResultSelected = { result ->
                                    showSearchSheet = false
                                    navigatorFragment?.go(result.locator, animated = true)
                                },
                                onSearchImmediate = { viewModel.onSearchQueryChanged(it, immediate = true) }
                            )
                        }
                    }

                    if (showSettingsSheet) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showSettingsSheet = false },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            ReaderSettingsSheetContent(
                                preferences = preferences,
                                isUsingBookOverride = isUsingBookOverride,
                                customFonts = customFonts,
                                format = state.format,
                                onToggleUseBookOverride = viewModel::toggleUseBookOverride,
                                onResetBookOverride = viewModel::resetBookOverride,
                                onUpdateTheme = viewModel::updateTheme,
                                onUpdateFontFamily = viewModel::updateFontFamily,
                                onUpdateFontSize = viewModel::updateFontSize,
                                onUpdateLineHeight = viewModel::updateLineHeight,
                                onUpdateTextAlignment = viewModel::updateTextAlignment,
                                onUpdateScrollMode = viewModel::updateScrollMode,
                                onUpdateVolumeButtons = viewModel::updateVolumeButtonsTurnPages,
                                onUpdateKeepScreenOn = viewModel::updateKeepScreenOn,
                                onUpdateFullscreen = viewModel::updateFullscreen,
                                onUpdateOrientation = viewModel::updateOrientation,
                                onUpdateBrightness = viewModel::updateBrightness,
                                onUpdateChromeOptions = viewModel::updateChromeOptions,
                                onAddCustomFontClick = {
                                    fontPickerLauncher.launch(
                                        arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype", "*/*")
                                    )
                                },
                                onOpenToc = {
                                    showSettingsSheet = false
                                    showTocSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog && pendingSelectionLocator != null) {
        val locator = pendingSelectionLocator!!
        var noteText by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf("YELLOW") }
        var addToReview by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddNoteDialog = false
                pendingSelectionLocator = null
            },
            title = { Text("Thêm ghi chú & tô sáng") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "\"" + (locator.text.highlight ?: "") + "\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Chọn màu tô sáng:", style = MaterialTheme.typography.labelMedium)
                    ColorPickerRow(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                    TextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Nhập nội dung ghi chú...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = addToReview, onCheckedChange = { addToReview = it })
                        Text("Thêm vào ôn tập hàng ngày", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addHighlight(
                        locator = locator,
                        colorHex = selectedColor,
                        note = noteText.ifBlank { null },
                        addToReview = addToReview
                    )
                    showAddNoteDialog = false
                    pendingSelectionLocator = null
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddNoteDialog = false
                    pendingSelectionLocator = null
                }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showPdfScannedAlert) {
        AlertDialog(
            onDismissRequest = { showPdfScannedAlert = false },
            title = { Text(if (pdfExtractionError == null) "Không có văn bản để chọn" else "Không thể trích xuất văn bản") },
            text = {
                Text(
                    pdfExtractionError
                        ?: "Tài liệu PDF này ở dạng scan hoặc ảnh và không chứa lớp văn bản số. NoCap tôn trọng tính nguyên bản của tài liệu và không giả lập nhận dạng ký tự (OCR)."
                )
            },
            confirmButton = {
                Button(onClick = { showPdfScannedAlert = false; pdfExtractionError = null }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    if (showPdfTextExtractDialog && pdfExtractedText != null) {
        val pageText = pdfExtractedText!!
        var selectedSnippet by remember { mutableStateOf(pageText.take(300)) }
        var noteText by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf("YELLOW") }
        var addToReview by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPdfTextExtractDialog = false },
            title = { Text("Trích đoạn & Ghi chú (Trang ${pdfExtractPageIndex + 1})") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Văn bản trích dẫn:", style = MaterialTheme.typography.labelMedium)
                    TextField(
                        value = selectedSnippet,
                        onValueChange = { selectedSnippet = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    Text("Màu tô sáng:", style = MaterialTheme.typography.labelMedium)
                    ColorPickerRow(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                    TextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Thêm ghi chú (tùy chọn)...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = addToReview, onCheckedChange = { addToReview = it })
                        Text("Thêm vào ôn tập hàng ngày", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val rawProg = (lastKnownLocator?.locations?.let { it.totalProgression ?: it.progression } ?: 0.0).toFloat()
                    val progression = if (rawProg >= 0.98f) 1f else rawProg.coerceIn(0f, 1f)
                    val locator = com.nocap.app.domain.model.PdfAnnotationLocator(
                        pageIndex = pdfExtractPageIndex,
                        pageNumber = pdfExtractPageIndex + 1,
                        progression = progression,
                        selectedText = selectedSnippet,
                        startOffset = 0,
                        endOffset = selectedSnippet.length,
                        contextSnippet = pageText.take(150)
                    )
                    viewModel.addHighlight(
                        text = selectedSnippet,
                        colorHex = selectedColor,
                        locatorJson = locator.toJson(),
                        note = noteText.ifBlank { null },
                        addToReview = addToReview
                    )
                    showPdfTextExtractDialog = false
                }) {
                    Text("Lưu trích dẫn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfTextExtractDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }


    selectedHighlightForEdit?.let { h ->
        var noteText by remember { mutableStateOf(h.note ?: "") }
        var currentColor by remember { mutableStateOf(h.color) }
        var showVersionHistory by remember { mutableStateOf(false) }
        var versions by remember { mutableStateOf<List<com.nocap.app.core.database.entity.HighlightNoteVersionEntity>>(emptyList()) }
        val editScope = rememberCoroutineScope()

        if (showVersionHistory) {
            AlertDialog(
                onDismissRequest = { showVersionHistory = false },
                title = { Text("Lịch sử phiên bản ghi chú") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (versions.isEmpty()) {
                            Text(
                                text = "Chưa có phiên bản cũ nào được ghi lại.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            versions.forEach { ver ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = ver.noteText,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val timeStr = remember(ver.createdAt) {
                                                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ver.createdAt))
                                            }
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            FilledTonalButton(
                                                onClick = {
                                                    noteText = ver.noteText
                                                    showVersionHistory = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Khôi phục", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVersionHistory = false }) {
                        Text("Đóng")
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { selectedHighlightForEdit = null },
            title = { Text("Chỉnh sửa đoạn tô sáng") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "\"" + h.text + "\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    ColorPickerRow(selectedColor = currentColor, onColorSelected = {
                        currentColor = it
                        viewModel.updateHighlightColor(h.id, it)
                    })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ghi chú", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                editScope.launch {
                                    versions = viewModel.getNoteVersions(h.id)
                                    showVersionHistory = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lịch sử phiên bản", fontSize = 11.sp)
                        }
                    }
                    TextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Nội dung ghi chú...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateHighlightNote(h.id, noteText)
                    selectedHighlightForEdit = null
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.deleteHighlight(h.id)
                        selectedHighlightForEdit = null
                    }) {
                        Text("Xóa", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { selectedHighlightForEdit = null }) {
                        Text("Đóng")
                    }
                }
            }
        )
    }
}

@Composable
fun ColorPickerRow(selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf(
        "YELLOW" to HIGHLIGHT_YELLOW,
        "GREEN" to HIGHLIGHT_GREEN,
        "BLUE" to HIGHLIGHT_BLUE,
        "PINK" to HIGHLIGHT_PINK
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { (name, color) ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selectedColor.equals(name, ignoreCase = true)) 3.dp else 1.dp,
                        color = if (selectedColor.equals(name, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .selectable(
                        selected = selectedColor.equals(name, ignoreCase = true),
                        role = androidx.compose.ui.semantics.Role.RadioButton,
                        onClick = { onColorSelected(name) }
                    )
                    .semantics {
                        contentDescription = when (name) {
                            "YELLOW" -> "Tô sáng màu vàng"
                            "GREEN" -> "Tô sáng màu xanh lá"
                            "BLUE" -> "Tô sáng màu xanh dương"
                            else -> "Tô sáng màu hồng"
                        }
                    }
            )
        }
    }
}

@Composable
fun ReaderChromeFooter(
    visible: Boolean,
    preferences: ReaderPreferences,
    currentLocator: Locator?,
    backgroundColor: Color,
    contentColor: Color
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = timeFormat.format(Date())
            delay(10000)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Surface(
            color = backgroundColor.copy(alpha = 0.95f),
            contentColor = contentColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (preferences.showReadingProgress) {
                    Text(
                        text = currentLocator?.title ?: "Tiến trình",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        color = contentColor.copy(alpha = 0.7f)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (preferences.showPercentage) {
                    val rawProg = currentLocator?.locations?.let { it.totalProgression ?: it.progression }
                    val pctText = if (rawProg != null) {
                        val pct = if (rawProg >= 0.98) 100 else (rawProg * 100).roundToInt().coerceIn(0, 100)
                        "$pct%"
                    } else "--%"
                    Text(
                        text = pctText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                if (preferences.showClock) {
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}

@Composable
fun InBookSearchSheetContent(
    isSupported: Boolean,
    query: String,
    isSearching: Boolean,
    results: List<SearchResultItem>,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onResultSelected: (SearchResultItem) -> Unit,
    onSearchImmediate: ((String) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Tìm kiếm trong sách",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (!isSupported) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tính năng tìm kiếm toàn văn hiện tại chỉ hỗ trợ định dạng EPUB.\nĐịnh dạng tài liệu hiện tại không hỗ trợ bộ chỉ mục tìm kiếm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Nhập từ khóa tìm kiếm...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) {
                        val trimmed = query.trim()
                        if (onSearchImmediate != null) {
                            onSearchImmediate(trimmed)
                        } else {
                            onQueryChange(trimmed)
                        }
                    }
                }),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(Icons.Default.Clear, contentDescription = localize("Xóa"))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isSearching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Đang tìm kiếm...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (query.isNotBlank()) {
                Text(
                    text = "Tìm thấy ${results.size} kết quả",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                items(results) { res ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultSelected(res) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!res.chapterTitle.isNullOrBlank()) {
                                Text(
                                    text = res.chapterTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            val annotatedSnippet = remember(res.snippet, query) {
                                buildAnnotatedString {
                                    val snippetText = res.snippet
                                    val trimmedQuery = query.trim()
                                    if (trimmedQuery.isEmpty()) {
                                        append(snippetText)
                                    } else {
                                        var start = 0
                                        while (start < snippetText.length) {
                                            val matchIndex = snippetText.indexOf(trimmedQuery, startIndex = start, ignoreCase = true)
                                            if (matchIndex < 0) {
                                                append(snippetText.substring(start))
                                                break
                                            }
                                            if (matchIndex > start) {
                                                append(snippetText.substring(start, matchIndex))
                                            }
                                            val matchEnd = matchIndex + trimmedQuery.length
                                            withStyle(
                                                SpanStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    background = Color(0xFFFFF176).copy(alpha = 0.8f),
                                                    color = Color.Black
                                                )
                                            ) {
                                                append(snippetText.substring(matchIndex, matchEnd))
                                            }
                                            start = matchEnd
                                        }
                                    }
                                }
                            }
                            Text(
                                text = annotatedSnippet,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnotationsBottomSheetContent(
    bookmarks: List<Bookmark>,
    highlights: List<HighlightEntity>,
    onBookmarkSelected: (Bookmark) -> Unit,
    onHighlightSelected: (HighlightEntity) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onEditHighlight: (HighlightEntity) -> Unit,
    onShareHighlight: (HighlightEntity) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val notes = remember(highlights) { highlights.filter { !it.note.isNullOrBlank() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Dấu trang (${bookmarks.size})") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Tô sáng (${highlights.size})") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Ghi chú (${notes.size})") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTabIndex) {
            0 -> {
                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có dấu trang nào.\nNhấn biểu tượng dấu trang trên thanh tiêu đề để lưu vị trí đọc.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBookmarkSelected(b) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(AppIcons.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(b.chapterTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (!b.snippet.isNullOrBlank()) {
                                        Text(b.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(dateFormat.format(Date(b.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { onDeleteBookmark(b.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = localize("Xóa"), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
            1 -> {
                if (highlights.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có đoạn văn nào được tô sáng.\nChọn đoạn văn trong sách để tô sáng hoặc ghi chú.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        items(highlights, key = { it.id }) { h ->
                            val dotColor = when (h.color.uppercase()) {
                                "GREEN" -> HIGHLIGHT_GREEN
                                "BLUE" -> HIGHLIGHT_BLUE
                                "PINK" -> HIGHLIGHT_PINK
                                else -> HIGHLIGHT_YELLOW
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onHighlightSelected(h) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\"" + h.text + "\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(dateFormat.format(Date(h.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { onShareHighlight(h) }) {
                                    Icon(Icons.Default.Share, contentDescription = localize("Chia sẻ"))
                                }
                                IconButton(onClick = { onEditHighlight(h) }) {
                                    Icon(Icons.Default.Edit, contentDescription = localize("Sửa"))
                                }
                                IconButton(onClick = { onDeleteHighlight(h.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = localize("Xóa"), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
            2 -> {
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có ghi chú nào.\nChọn đoạn văn và chọn Ghi chú để lưu ý tưởng của bạn.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        items(notes, key = { it.id }) { h ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onHighlightSelected(h) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = h.note ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"" + h.text + "\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(dateFormat.format(Date(h.updatedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Row {
                                            IconButton(onClick = { onEditHighlight(h) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = localize("Sửa"), modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { onDeleteHighlight(h.id) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = localize("Xóa"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpubNavigatorContainer(
    bookId: String,
    publication: Publication,
    initialLocator: Locator?,
    initialPreferences: org.readium.r2.navigator.epub.EpubPreferences,
    customFonts: List<CustomFontEntity>,
    onNavigatorReady: (EpubNavigatorFragment) -> Unit,
    onHighlightRequested: (Locator, String) -> Unit,
    onNoteRequested: (Locator) -> Unit,
    onCopyQuote: (String) -> Unit,
    onShareQuote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? FragmentActivity
    val containerId = remember(bookId) { View.generateViewId() }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(bookId) {
        onDispose {
            activity?.let { act ->
                val existing = act.supportFragmentManager.findFragmentByTag("epub_navigator_$bookId")
                if (existing != null) {
                    act.supportFragmentManager.commit(allowStateLoss = true) {
                        remove(existing)
                    }
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                if (activity != null) {
                    val factory = EpubNavigatorFactory(publication)
                    val fontsDir = File(com.nocap.app.data.sync.Profiles.files(ctx), "custom_fonts")
                    val servedPaths = listOfNotNull(
                        if (fontsDir.exists()) fontsDir.absolutePath else null
                    )

                    var fragmentInstance: EpubNavigatorFragment? = null

                    val config = EpubNavigatorFragment.Configuration(
                        servedAssets = servedPaths,
                        selectionActionModeCallback = object : ActionMode.Callback {
                            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                                menu.add(0, 101, 0, com.nocap.app.core.localization.AppLanguageManager.translate(ctx, "Tô sáng"))
                                menu.add(0, 102, 1, com.nocap.app.core.localization.AppLanguageManager.translate(ctx, "Ghi chú"))
                                menu.add(0, 103, 2, com.nocap.app.core.localization.AppLanguageManager.translate(ctx, "Sao chép"))
                                menu.add(0, 104, 3, com.nocap.app.core.localization.AppLanguageManager.translate(ctx, "Chia sẻ"))
                                return true
                            }
                            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
                            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                                val nav = fragmentInstance as? SelectableNavigator
                                if (nav != null) {
                                    coroutineScope.launch {
                                        val selection = nav.currentSelection()
                                        if (selection != null) {
                                            val quoteText = selection.locator.text.highlight ?: selection.locator.text.before ?: ""
                                            val title = publication.metadata.title
                                            when (item.itemId) {
                                                101 -> onHighlightRequested(selection.locator, "YELLOW")
                                                102 -> onNoteRequested(selection.locator)
                                                103 -> onCopyQuote("\"" + quoteText + "\"\n— " + title)
                                                104 -> onShareQuote("\"" + quoteText + "\"\n— " + title)
                                                else -> {}
                                            }
                                        }
                                        mode.finish()
                                    }
                                } else {
                                    mode.finish()
                                }
                                return true
                            }
                            override fun onDestroyActionMode(mode: ActionMode) {}
                        }
                    )

                    for (font in customFonts) {
                        runCatching {
                            config.addFontFamilyDeclaration(FontFamily(font.fontFamily)) {
                                addFontFace {
                                    addSource(File(com.nocap.app.data.sync.Profiles.files(ctx), "custom_fonts/" + font.fileName).absolutePath, false)
                                }
                            }
                        }
                    }

                    activity.supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
                        initialLocator = initialLocator,
                        initialPreferences = initialPreferences,
                        listener = object : EpubNavigatorFragment.Listener {
                            override fun onExternalLinkActivated(url: AbsoluteUrl) {
                                val uri = Uri.parse(url.toString())
                                if (uri.scheme?.lowercase() in setOf("http", "https", "mailto")) {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }.onFailure {
                                        Toast.makeText(ctx, "Không tìm thấy ứng dụng để mở liên kết", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        configuration = config
                    )
                    val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                        activity.classLoader,
                        EpubNavigatorFragment::class.java.name
                    ) as EpubNavigatorFragment
                    fragmentInstance = fragment

                    activity.supportFragmentManager.commit(allowStateLoss = true) {
                        replace(containerId, fragment, "epub_navigator_$bookId")
                    }

                    post {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            onNavigatorReady(fragment)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun PdfNavigatorContainer(
    bookId: String,
    publication: Publication,
    initialLocator: Locator?,
    onNavigatorReady: (VisualNavigator) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? FragmentActivity
    val containerId = remember(bookId) { View.generateViewId() }

    DisposableEffect(bookId) {
        onDispose {
            activity?.let { act ->
                val existing = act.supportFragmentManager.findFragmentByTag("pdf_navigator_$bookId")
                if (existing != null) {
                    act.supportFragmentManager.commit(allowStateLoss = true) {
                        remove(existing)
                    }
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                if (activity != null) {
                    val factory = PdfNavigatorFactory(
                        publication = publication,
                        pdfEngineProvider = PdfiumEngineProvider()
                    )
                    activity.supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
                        initialLocator = initialLocator
                    )
                    val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                        activity.classLoader,
                        PdfNavigatorFragment::class.java.name
                    ) as PdfNavigatorFragment<*, *>

                    activity.supportFragmentManager.commit(allowStateLoss = true) {
                        replace(containerId, fragment, "pdf_navigator_$bookId")
                    }

                    post {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            onNavigatorReady(fragment)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TocBottomSheetContent(
    toc: List<TocItem>,
    onChapterSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Mục lục",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (toc.isEmpty()) {
            Text(
                text = "Cuốn sách này không có mục lục",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                items(toc) { item ->
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterSelected(item.href) }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ReaderSettingsSheetContent(
    preferences: ReaderPreferences,
    isUsingBookOverride: Boolean,
    customFonts: List<CustomFontEntity>,
    format: PublicationFormat,
    onToggleUseBookOverride: (Boolean) -> Unit,
    onResetBookOverride: () -> Unit,
    onUpdateTheme: (ReaderTheme) -> Unit,
    onUpdateFontFamily: (ReaderFontFamily, String?) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateLineHeight: (Float) -> Unit,
    onUpdateTextAlignment: (ReaderTextAlignment) -> Unit,
    onUpdateScrollMode: (Boolean) -> Unit,
    onUpdateVolumeButtons: (Boolean) -> Unit,
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onUpdateFullscreen: (Boolean) -> Unit,
    onUpdateOrientation: (ReaderOrientation) -> Unit,
    onUpdateBrightness: (Boolean, Float) -> Unit,
    onUpdateChromeOptions: (Boolean, Boolean, Boolean) -> Unit,
    onAddCustomFontClick: () -> Unit,
    onOpenToc: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tùy chỉnh đọc",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onOpenToc) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mục lục")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tùy chỉnh riêng cho sách này",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUsingBookOverride) "Đang áp dụng cài đặt riêng" else "Kế thừa cài đặt chung toàn ứng dụng",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isUsingBookOverride,
                            onCheckedChange = onToggleUseBookOverride
                        )
                    }

                    if (isUsingBookOverride) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onResetBookOverride,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đặt lại về mặc định chung")
                        }
                    }
                }
            }
        }

        item {
            Text("Giao diện", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Triple(ReaderTheme.LIGHT, "Sáng", Color(0xFFF5F5F5) to Color(0xFF1C1B1F)),
                    Triple(ReaderTheme.DARK, "Tối", Color(0xFF1E1E1E) to Color(0xFFE6E1E5)),
                    Triple(ReaderTheme.SEPIA, "Vàng giấy", Color(0xFFF4ECD8) to Color(0xFF5B4636))
                ).forEach { (t, label, colors) ->
                    val isSelected = preferences.theme == t
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onUpdateTheme(t) },
                        color = colors.first
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, color = colors.second, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        if (format != PublicationFormat.PDF) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Phông chữ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onAddCustomFontClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm phông (.ttf/.otf)")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.SYSTEM_DEFAULT,
                            onClick = { onUpdateFontFamily(ReaderFontFamily.SYSTEM_DEFAULT, null) },
                            label = { Text("Mặc định") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.SERIF,
                            onClick = { onUpdateFontFamily(ReaderFontFamily.SERIF, null) },
                            label = { Text("Có chân") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.SANS_SERIF,
                            onClick = { onUpdateFontFamily(ReaderFontFamily.SANS_SERIF, null) },
                            label = { Text("Không chân") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.LORA,
                            onClick = { onUpdateFontFamily(ReaderFontFamily.LORA, null) },
                            label = { Text("Lora") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.ROBOTO,
                            onClick = { onUpdateFontFamily(ReaderFontFamily.ROBOTO, null) },
                            label = { Text("Roboto") }
                        )
                    }
                    items(customFonts) { font ->
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.CUSTOM && preferences.customFontName == font.fontFamily,
                            onClick = { onUpdateFontFamily(ReaderFontFamily.CUSTOM, font.fontFamily) },
                            label = { Text(font.fontFamily) }
                        )
                    }
                }
            }

            item {
                Text("Cỡ chữ: ${(preferences.fontSizeMultiplier * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Slider(
                    value = preferences.fontSizeMultiplier,
                    onValueChange = onUpdateFontSize,
                    valueRange = 0.8f..2.0f,
                    steps = 5
                )
            }

            item {
                Text("Giãn dòng: ${preferences.lineHeightMultiplier}x", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Slider(
                    value = preferences.lineHeightMultiplier,
                    onValueChange = onUpdateLineHeight,
                    valueRange = 1.2f..2.0f,
                    steps = 4
                )
            }

            item {
                Text("Căn lề", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.textAlignment == ReaderTextAlignment.JUSTIFY,
                        onClick = { onUpdateTextAlignment(ReaderTextAlignment.JUSTIFY) },
                        label = { Text("Căn đều 2 bên") }
                    )
                    FilterChip(
                        selected = preferences.textAlignment == ReaderTextAlignment.START,
                        onClick = { onUpdateTextAlignment(ReaderTextAlignment.START) },
                        label = { Text("Căn trái") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chế độ cuộn liên tục", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = preferences.isScrollMode, onCheckedChange = onUpdateScrollMode)
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Điều khiển & Màn hình", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Phím âm lượng lật trang", style = MaterialTheme.typography.bodyLarge)
                    Text("Dùng Volume Up/Down để chuyển trang", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = preferences.volumeButtonsTurnPages, onCheckedChange = onUpdateVolumeButtons)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Giữ màn hình luôn sáng", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = preferences.keepScreenOn, onCheckedChange = onUpdateKeepScreenOn)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chế độ toàn màn hình", style = MaterialTheme.typography.bodyLarge)
                    Text("Ẩn thanh trạng thái và thanh điều hướng", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = preferences.isFullscreen, onCheckedChange = onUpdateFullscreen)
            }
        }

        item {
            Text("Khóa hướng màn hình", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = preferences.orientation == ReaderOrientation.FOLLOW_SYSTEM,
                    onClick = { onUpdateOrientation(ReaderOrientation.FOLLOW_SYSTEM) },
                    label = { Text("Theo hệ thống") }
                )
                FilterChip(
                    selected = preferences.orientation == ReaderOrientation.PORTRAIT,
                    onClick = { onUpdateOrientation(ReaderOrientation.PORTRAIT) },
                    label = { Text("Dọc") }
                )
                FilterChip(
                    selected = preferences.orientation == ReaderOrientation.LANDSCAPE,
                    onClick = { onUpdateOrientation(ReaderOrientation.LANDSCAPE) },
                    label = { Text("Ngang") }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Độ sáng theo hệ thống", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = preferences.brightnessFollowSystem,
                    onCheckedChange = { follow -> onUpdateBrightness(follow, preferences.customBrightness) }
                )
            }
            if (!preferences.brightnessFollowSystem) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Độ sáng tùy chỉnh: " + (preferences.customBrightness * 100).toInt() + "%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = preferences.customBrightness,
                    onValueChange = { b -> onUpdateBrightness(false, b) },
                    valueRange = 0.05f..1.0f
                )
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Thông tin đọc sách ở chân trang", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hiển thị tên chương / tiến độ", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = preferences.showReadingProgress,
                    onCheckedChange = { onUpdateChromeOptions(it, preferences.showPercentage, preferences.showClock) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hiển thị phần trăm (%)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = preferences.showPercentage,
                    onCheckedChange = { onUpdateChromeOptions(preferences.showReadingProgress, it, preferences.showClock) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hiển thị đồng hồ thời gian", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = preferences.showClock,
                    onCheckedChange = { onUpdateChromeOptions(preferences.showReadingProgress, preferences.showPercentage, it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReaderErrorState(
    message: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onBack, shape = RoundedCornerShape(8.dp)) {
                    Text("Quay lại")
                }
                if (onRetry != null) {
                    Button(onClick = onRetry, shape = RoundedCornerShape(8.dp)) {
                        Text("Thử lại")
                    }
                }
            }
        }
    }
}
