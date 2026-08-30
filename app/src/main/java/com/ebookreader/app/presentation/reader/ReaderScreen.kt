package com.ebookreader.app.presentation.reader

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import com.ebookreader.app.core.designsystem.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ebookreader.app.core.datastore.ReaderFontFamily
import com.ebookreader.app.core.datastore.ReaderPreferences
import com.ebookreader.app.core.datastore.ReaderTextAlignment
import com.ebookreader.app.core.datastore.ReaderTheme
import com.ebookreader.app.domain.model.Bookmark
import com.ebookreader.app.domain.model.TocItem
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.provideFactory(bookId, LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isCurrentBookmarked.collectAsStateWithLifecycle()

    var showTocSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    var navigatorFragment by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var lastKnownLocator by remember { mutableStateOf<Locator?>(null) }

    // Dynamic reader theme colors
    val (barBackground, barContentColor) = when (preferences.theme) {
        ReaderTheme.LIGHT -> Color(0xFFFFFFFF) to Color(0xFF1C1B1F)
        ReaderTheme.DARK -> Color(0xFF121212) to Color(0xFFE6E1E5)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
    }

    val handleBack: () -> Unit = {
        viewModel.saveCurrentLocationImmediately(lastKnownLocator ?: navigatorFragment?.currentLocator?.value)
        onBackClick()
    }

    BackHandler(onBack = handleBack)

    // Apply real-time preferences to Readium Navigator
    LaunchedEffect(preferences, navigatorFragment) {
        navigatorFragment?.submitPreferences(preferences.toReadiumPreferences())
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
                                contentDescription = "Quay lại",
                                tint = barContentColor
                            )
                        }
                    },
                    actions = {
                        if (uiState is ReaderUiState.Ready) {
                            // Bookmark toggle
                            IconButton(onClick = viewModel::toggleBookmark) {
                                Icon(
                                    imageVector = if (isBookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                                    contentDescription = "Đánh dấu trang",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else barContentColor
                                )
                            }
                            // Bookmarks list
                            IconButton(onClick = { showBookmarksSheet = true }) {
                                Icon(
                                    imageVector = AppIcons.Bookmarks,
                                    contentDescription = "Danh sách đánh dấu",
                                    tint = barContentColor
                                )
                            }
                            // TOC
                            IconButton(onClick = { showTocSheet = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Mục lục",
                                    tint = barContentColor
                                )
                            }
                            // Aa Settings
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Text(
                                    text = "Aa",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = barContentColor
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = barBackground
                    )
                )
            }
        },
        containerColor = barBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ReaderUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Đang mở sách...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = barContentColor
                            )
                        }
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
                        message = "Không tìm thấy file EPUB trên thiết bị.",
                        onBack = onBackClick
                    )
                }
                is ReaderUiState.Error -> {
                    ReaderErrorState(
                        message = "Lỗi mở sách: ${state.message}",
                        onBack = onBackClick,
                        onRetry = viewModel::loadPublication
                    )
                }
                is ReaderUiState.Ready -> {
                    EpubNavigatorContainer(
                        bookId = bookId,
                        publication = state.publication,
                        initialLocator = state.initialLocator,
                        initialPreferences = preferences.toReadiumPreferences(),
                        onNavigatorReady = { nav ->
                            navigatorFragment = nav
                        }
                    )

                    // Track locator changes
                    LaunchedEffect(navigatorFragment) {
                        val nav = navigatorFragment ?: return@LaunchedEffect
                        nav.currentLocator.collect { locator ->
                            lastKnownLocator = locator
                            viewModel.onLocationChanged(locator)
                        }
                    }

                    // Table of Contents Sheet
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

                    // Bookmarks Sheet
                    if (showBookmarksSheet) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showBookmarksSheet = false },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            BookmarksBottomSheetContent(
                                bookmarks = bookmarks,
                                onBookmarkSelected = { bookmark ->
                                    showBookmarksSheet = false
                                    runCatching {
                                        Locator.fromJSON(JSONObject(bookmark.locatorJson))?.let { loc ->
                                            navigatorFragment?.go(loc, animated = true)
                                        }
                                    }
                                },
                                onDeleteBookmark = viewModel::removeBookmark
                            )
                        }
                    }

                    // Settings Bottom Sheet
                    if (showSettingsSheet) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showSettingsSheet = false },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            ReaderSettingsSheetContent(
                                preferences = preferences,
                                onUpdateTheme = viewModel::updateTheme,
                                onUpdateFontFamily = viewModel::updateFontFamily,
                                onUpdateFontSize = viewModel::updateFontSize,
                                onUpdateLineHeight = viewModel::updateLineHeight,
                                onUpdateTextAlignment = viewModel::updateTextAlignment,
                                onUpdateScrollMode = viewModel::updateScrollMode
                            )
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
    onNavigatorReady: (EpubNavigatorFragment) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? FragmentActivity
    val containerId = remember(bookId) { View.generateViewId() }

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
                    activity.supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
                        initialLocator = initialLocator,
                        initialPreferences = initialPreferences
                    )
                    val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                        activity.classLoader,
                        EpubNavigatorFragment::class.java.name
                    ) as EpubNavigatorFragment

                    activity.supportFragmentManager.commit(allowStateLoss = true) {
                        replace(containerId, fragment, "epub_navigator_$bookId")
                    }

                    onNavigatorReady(fragment)
                }
            }
        }
    )
}

@Composable
fun ReaderSettingsSheetContent(
    preferences: ReaderPreferences,
    onUpdateTheme: (ReaderTheme) -> Unit,
    onUpdateFontFamily: (ReaderFontFamily) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateLineHeight: (Float) -> Unit,
    onUpdateTextAlignment: (ReaderTextAlignment) -> Unit,
    onUpdateScrollMode: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Tùy chỉnh đọc sách",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Theme Selection
        Text("Giao diện", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeOptionCard(
                name = "Sáng",
                bgColor = Color(0xFFFFFFFF),
                textColor = Color(0xFF000000),
                isSelected = preferences.theme == ReaderTheme.LIGHT,
                onClick = { onUpdateTheme(ReaderTheme.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                name = "Sepia",
                bgColor = Color(0xFFF4ECD8),
                textColor = Color(0xFF5B4636),
                isSelected = preferences.theme == ReaderTheme.SEPIA,
                onClick = { onUpdateTheme(ReaderTheme.SEPIA) },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                name = "Tối",
                bgColor = Color(0xFF1E1E1E),
                textColor = Color(0xFFE0E0E0),
                isSelected = preferences.theme == ReaderTheme.DARK,
                onClick = { onUpdateTheme(ReaderTheme.DARK) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Font Size Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cỡ chữ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onUpdateFontSize((preferences.fontSizeMultiplier - 0.1f).coerceIn(0.8f, 2.0f)) },
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("A-", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${(preferences.fontSizeMultiplier * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { onUpdateFontSize((preferences.fontSizeMultiplier + 0.1f).coerceIn(0.8f, 2.0f)) },
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("A+", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Font Family Selection
        Text("Kiểu chữ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = preferences.fontFamily == ReaderFontFamily.SYSTEM_DEFAULT,
                onClick = { onUpdateFontFamily(ReaderFontFamily.SYSTEM_DEFAULT) },
                label = { Text("Mặc định") }
            )
            FilterChip(
                selected = preferences.fontFamily == ReaderFontFamily.SERIF,
                onClick = { onUpdateFontFamily(ReaderFontFamily.SERIF) },
                label = { Text("Serif", fontFamily = FontFamily.Serif) }
            )
            FilterChip(
                selected = preferences.fontFamily == ReaderFontFamily.SANS_SERIF,
                onClick = { onUpdateFontFamily(ReaderFontFamily.SANS_SERIF) },
                label = { Text("Sans-Serif", fontFamily = FontFamily.SansSerif) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Line Spacing & Alignment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Căn lề", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.textAlignment == ReaderTextAlignment.START,
                        onClick = { onUpdateTextAlignment(ReaderTextAlignment.START) },
                        label = { Text("Trái") }
                    )
                    FilterChip(
                        selected = preferences.textAlignment == ReaderTextAlignment.JUSTIFY,
                        onClick = { onUpdateTextAlignment(ReaderTextAlignment.JUSTIFY) },
                        label = { Text("Đều") }
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Cuộn dọc", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Switch(
                    checked = preferences.isScrollMode,
                    onCheckedChange = onUpdateScrollMode
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ThemeOptionCard(
    name: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun BookmarksBottomSheetContent(
    bookmarks: List<Bookmark>,
    onBookmarkSelected: (Bookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Dấu trang đã lưu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có dấu trang nào.\nNhấn biểu tượng dấu trang trên thanh công cụ để lưu vị trí đọc.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookmarkSelected(bookmark) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookmark.chapterTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!bookmark.snippet.isNullOrBlank()) {
                                Text(
                                    text = bookmark.snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(bookmark.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa dấu trang",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
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
                color = MaterialTheme.colorScheme.error
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

@Composable
fun TocBottomSheetContent(
    toc: List<TocItem>,
    onChapterSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Mục lục sách",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (toc.isEmpty()) {
            Text(
                text = "Không có mục lục cho cuốn sách này.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                items(toc) { item ->
                    TocItemRow(item = item, depth = 0, onChapterSelected = onChapterSelected)
                }
            }
        }
    }
}

@Composable
fun TocItemRow(
    item: TocItem,
    depth: Int,
    onChapterSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChapterSelected(item.href) }
                .padding(start = (depth * 16).dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

        item.children.forEach { child ->
            TocItemRow(item = child, depth = depth + 1, onChapterSelected = onChapterSelected)
        }
    }
}
