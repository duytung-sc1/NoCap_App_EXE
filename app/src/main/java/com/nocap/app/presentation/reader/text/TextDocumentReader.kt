package com.nocap.app.presentation.reader.text

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import com.nocap.app.core.localization.Text
import com.nocap.app.core.localization.localize
import androidx.compose.material3.TextButton

import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.data.review.LocalReviewRepository
import com.nocap.app.domain.session.ReadingSessionManager
import com.nocap.app.presentation.reader.ColorPickerRow

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.flow.sample
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.ReadingProgressEntity
import com.nocap.app.core.datastore.ReaderTheme
import com.nocap.app.core.designsystem.AppIcons
import com.nocap.app.data.parser.DocxParser
import com.nocap.app.data.parser.HtmlSanitizerParser
import com.nocap.app.data.parser.MarkdownParser
import com.nocap.app.data.parser.TxtParser
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.TextDocument
import com.nocap.app.domain.model.TextDocumentBlock
import com.nocap.app.domain.model.TextLocator
import com.nocap.app.domain.model.TextSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TextDocumentReader(
    book: CatalogBook,
    file: File,
    initialLocatorJson: String? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val sessionManager = remember { ReadingSessionManager.getInstance(context.applicationContext) }
    var sessionId by remember { mutableStateOf<String?>(null) }

    var document by remember { mutableStateOf<TextDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var positionRestored by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var selectedBlockForNote by remember { mutableStateOf<Pair<Int, TextDocumentBlock>?>(null) }

    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    var theme by remember { mutableStateOf(ReaderTheme.LIGHT) }

    val readerBookmarks by remember(book.id) { db.bookmarkDao().observeBookmarksForBook(book.id) }.collectAsState(initial = emptyList())
    var savedLocator by remember { mutableStateOf<TextLocator?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose {
            val total = document?.blocks?.size ?: 1
            val finalProg = if (total > 0) (listState.firstVisibleItemIndex.toFloat() / total).coerceIn(0f, 1f) else 0f
            sessionId?.let { sId ->
                sessionManager.endSessionAsync(sId, finalProg)
            }
        }
    }

    // Colors according to theme
    val (backgroundColor, textColor) = when (theme) {
        ReaderTheme.LIGHT -> Color(0xFFFFFFFF) to Color(0xFF1C1B1F)
        ReaderTheme.DARK -> Color(0xFF121212) to Color(0xFFE6E1E5)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
    }

    // 1. Load document and saved progress asynchronously
    LaunchedEffect(book.id) {
        withContext(Dispatchers.IO) {
            try {
                val progress = db.progressDao().getProgress(book.id)
                val locator = initialLocatorJson?.let { TextLocator.fromJson(it) } ?: progress?.locatorJson?.let { TextLocator.fromJson(it) }
                savedLocator = locator
                sessionId = sessionManager.startSession(
                    bookId = book.id,
                    format = book.format.name,
                    startProgress = locator?.progression ?: 0f
                )

                val parsedDoc = when (book.format) {
                    PublicationFormat.TXT -> TxtParser.validateAndParse(file, book.title)
                    PublicationFormat.MARKDOWN -> MarkdownParser.validateAndParse(file, book.title)
                    PublicationFormat.HTML -> HtmlSanitizerParser.validateAndParse(file, book.title, mainContentOnly = book.sourceUrl?.startsWith("https://") == true)
                    PublicationFormat.DOCX -> {
                        val cacheDir = File(context.cacheDir, "docx_media").apply { if (!exists()) mkdirs() }
                        DocxParser.parse(file, cacheDir, book.title)
                    }
                    else -> throw IllegalArgumentException("Trình đọc văn bản không hỗ trợ định dạng ${book.format}")
                }
                document = parsedDoc

                // Update last opened and reading status
                db.catalogDao().updateLastOpenedAt(book.id, System.currentTimeMillis())
                if (book.readingStatus == DocumentReadingStatus.UNREAD) {
                    db.catalogDao().updateReadingStatus(book.id, DocumentReadingStatus.READING)
                }

                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Không thể mở tài liệu"
                    isLoading = false
                }
            }
        }
    }

    // Wait until the list is composed before restoring; never persist its initial zero position.
    LaunchedEffect(document, isLoading) {
        val loaded = document ?: return@LaunchedEffect
        if (isLoading || errorMessage != null) return@LaunchedEffect
        val locator = savedLocator
        if (locator != null && loaded.blocks.isNotEmpty()) {
            listState.scrollToItem(locator.blockIndex.coerceIn(0, loaded.blocks.lastIndex), locator.scrollOffsetPx)
        }
        positionRestored = true
    }

    // 2. Track reading progress and save periodically
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    LaunchedEffect(listState, document, positionRestored) {
        if (!positionRestored || document == null) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .sample(500)
            .collect { (index, offset) ->
                val totalBlocks = document?.blocks?.size ?: 1
                val progression = if (totalBlocks > 0) (index.toFloat() / totalBlocks).coerceIn(0f, 1f) else 0f
                val blockText = document?.blocks?.getOrNull(index)?.plainText?.take(100)

                val locator = TextLocator(
                    blockIndex = index,
                    characterOffset = 0,
                    scrollOffsetPx = offset,
                    progression = progression,
                    snippet = blockText
                )

                withContext(Dispatchers.IO) {
                    db.progressDao().saveProgress(
                        ReadingProgressEntity(
                            bookId = book.id,
                            locatorJson = locator.toJson(),
                            progression = progression,
                            chapterTitle = document?.title ?: book.displayTitle,
                            lastReadAt = System.currentTimeMillis()
                        )
                    )
                }
            }
    }

    // Save location immediately on exit
    DisposableEffect(Unit) {
        onDispose {
            if (!positionRestored || document == null) return@onDispose
            val idx = listState.firstVisibleItemIndex
            val totalBlocks = document?.blocks?.size ?: 1
            val progression = if (totalBlocks > 0) (idx.toFloat() / totalBlocks).coerceIn(0f, 1f) else 0f
            val locator = TextLocator(blockIndex = idx, characterOffset = 0, progression = progression,
                scrollOffsetPx = listState.firstVisibleItemScrollOffset)
            ReadingSessionManager.processScope.launch {
                db.progressDao().saveProgress(
                    ReadingProgressEntity(
                        bookId = book.id,
                        locatorJson = locator.toJson(),
                        progression = progression,
                        chapterTitle = document?.title ?: book.displayTitle,
                        lastReadAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorMessage != null) {
            com.nocap.app.presentation.components.WorkspaceState(
                title = "Chưa mở được tài liệu",
                message = "Hãy quay lại thư viện và mở lại. Nếu vẫn gặp lỗi, thử nhập lại tệp gốc. Tài liệu và ghi chú của bạn vẫn được giữ.",
                actionLabel = "Về thư viện", onAction = onBackClick
            )
        } else {
            val doc = document!!

            val density = LocalDensity.current
            var controlBarHeight by remember { mutableStateOf(112.dp) }

            // Keep the reading viewport below the visible controls.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (showControls) controlBarHeight else 0.dp)
                    .clickable { showControls = !showControls }
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!doc.warningMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = doc.warningMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                itemsIndexed(doc.blocks) { idx, block ->
                    val isHighlighted = searchMatches.isNotEmpty() && currentMatchIndex in searchMatches.indices && searchMatches[currentMatchIndex] == idx
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isHighlighted) {
                                    Modifier
                                        .background(Color(0xFFFFF176).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                } else Modifier
                            )
                            .combinedClickable(
                                onClick = { showControls = !showControls },
                                onLongClickLabel = "Tô sáng hoặc thêm ghi chú",
                                onLongClick = {
                                    if (block.plainText.isNotBlank()) {
                                        selectedBlockForNote = Pair(idx, block)
                                    }
                                }
                            )
                    ) {

                        RenderBlock(
                            block = block,
                            fontSizeSp = fontSizeSp,
                            textColor = textColor,
                            searchHighlight = if (searchQuery.isNotBlank()) searchQuery else null
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Top Control Bar
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    modifier = Modifier.onSizeChanged {
                        controlBarHeight = with(density) { it.height.toDp() }
                    },
                    title = {
                        Text(
                            text = doc.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localize("Trở về"))
                        }
                    },
                    actions = {
                        // Search
                        IconButton(onClick = { showSearchSheet = !showSearchSheet }) {
                            Icon(Icons.Default.Search, contentDescription = localize("Tìm kiếm"))
                        }
                        // Bookmark
                        IconButton(onClick = {
                            val currentIdx = listState.firstVisibleItemIndex
                            val blockText = doc.blocks.getOrNull(currentIdx)?.plainText?.take(100) ?: ""
                            val locator = TextLocator(blockIndex = currentIdx, characterOffset = 0, snippet = blockText,
                                scrollOffsetPx = listState.firstVisibleItemScrollOffset)
                            scope.launch(Dispatchers.IO) {
                                db.bookmarkDao().insertReaderBookmarkIfAbsent(
                                    BookmarkEntity(
                                        id = UUID.randomUUID().toString(),
                                        bookId = book.id,
                                        locatorJson = locator.toJson(),
                                        chapterTitle = doc.title,
                                        snippet = blockText
                                    )
                                )
                            }
                        }) {
                            Icon(
                                imageVector = if (readerBookmarks.any { com.nocap.app.domain.model.ReaderBookmarkPosition.matches(it.locatorJson, TextLocator(blockIndex = listState.firstVisibleItemIndex)) }) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                                contentDescription = localize("Đánh dấu"),
                                tint = if (readerBookmarks.any { com.nocap.app.domain.model.ReaderBookmarkPosition.matches(it.locatorJson, TextLocator(blockIndex = listState.firstVisibleItemIndex)) }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Settings
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = localize("Cài đặt"))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        titleContentColor = textColor,
                        actionIconContentColor = textColor,
                        navigationIconContentColor = textColor
                    )
                )
            }

            // Search Bar Widget
            AnimatedVisibility(
                visible = showSearchSheet,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { q ->
                                    searchQuery = q
                                    if (q.length >= 2) {
                                        val matches = mutableListOf<Int>()
                                        doc.blocks.forEachIndexed { i, b ->
                                            if (b.plainText.contains(q, ignoreCase = true)) {
                                                matches.add(i)
                                            }
                                        }
                                        searchMatches = matches
                                        currentMatchIndex = 0
                                        if (matches.isNotEmpty()) {
                                            scope.launch {
                                                listState.animateScrollToItem(matches.first())
                                            }
                                        }
                                    } else {
                                        searchMatches = emptyList()
                                    }
                                },
                                placeholder = { Text("Tìm trong tài liệu...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            IconButton(onClick = {
                                showSearchSheet = false
                                searchQuery = ""
                                searchMatches = emptyList()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = localize("Đóng"))
                            }
                        }

                        if (searchMatches.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kết quả ${currentMatchIndex + 1}/${searchMatches.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (currentMatchIndex > 0) {
                                                currentMatchIndex--
                                                scope.launch { listState.animateScrollToItem(searchMatches[currentMatchIndex]) }
                                            }
                                        },
                                        enabled = currentMatchIndex > 0
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = localize("Trước"))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (currentMatchIndex < searchMatches.size - 1) {
                                                currentMatchIndex++
                                                scope.launch { listState.animateScrollToItem(searchMatches[currentMatchIndex]) }
                                            }
                                        },
                                        enabled = currentMatchIndex < searchMatches.size - 1
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = localize("Sau"))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Typography & Theme Settings Sheet
            if (showSettingsSheet) {
                ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Cài đặt đọc tài liệu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // Theme selection
                        Text("Chế độ màu", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemeChip("Sáng", ReaderTheme.LIGHT, theme == ReaderTheme.LIGHT) { theme = ReaderTheme.LIGHT }
                            ThemeChip("Vàng giấy", ReaderTheme.SEPIA, theme == ReaderTheme.SEPIA) { theme = ReaderTheme.SEPIA }
                            ThemeChip("Tối", ReaderTheme.DARK, theme == ReaderTheme.DARK) { theme = ReaderTheme.DARK }
                        }

                        HorizontalDivider()

                        // Font size
                        Text("Cỡ chữ: ${fontSizeSp.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = fontSizeSp,
                            onValueChange = { fontSizeSp = it },
                            valueRange = 12f..32f,
                            steps = 10
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            selectedBlockForNote?.let { (idx, block) ->
                var noteText by remember { mutableStateOf("") }
                var selectedColor by remember { mutableStateOf("YELLOW") }
                var addToReview by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { selectedBlockForNote = null },
                    title = { Text("Trích đoạn & Ghi chú") },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "“" + block.plainText.take(200) + "”",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Chọn màu:", style = MaterialTheme.typography.labelMedium)
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
                            val totalBlocks = document?.blocks?.size ?: 1
                            val prog = if (totalBlocks > 0) (idx.toFloat() / totalBlocks).coerceIn(0f, 1f) else 0f
                            val locator = TextLocator(
                                blockIndex = idx,
                                characterOffset = 0,
                                progression = prog,
                                snippet = block.plainText.take(100)
                            )
                            val highlightId = UUID.randomUUID().toString()
                            val highlight = HighlightEntity(
                                id = highlightId,
                                bookId = book.id,
                                locatorJson = locator.toJson(),
                                text = block.plainText,
                                color = selectedColor,
                                note = noteText.ifBlank { null }
                            )
                            scope.launch(Dispatchers.IO) {
                                db.highlightDao().insertHighlight(highlight)
                                if (addToReview || noteText.isNotBlank()) {
                                    LocalReviewRepository(db.reviewDao()).addToReview(highlightId, book.id)
                                }
                            }
                            selectedBlockForNote = null
                        }) {
                            Text("Lưu")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedBlockForNote = null }) {
                            Text("Hủy")
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun ThemeChip(label: String, targetTheme: ReaderTheme, isSelected: Boolean, onClick: () -> Unit) {
    val (chipBg, chipBorder) = when (targetTheme) {
        ReaderTheme.LIGHT -> Color(0xFFFFFFFF) to Color(0xFFCCCCCC)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFFD8C7A5)
        ReaderTheme.DARK -> Color(0xFF242424) to Color(0xFF444444)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = chipBg,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (targetTheme == ReaderTheme.DARK) Color.White else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun RenderBlock(
    block: TextDocumentBlock,
    fontSizeSp: Float,
    textColor: Color,
    searchHighlight: String?
) {
    when (block) {
        is TextDocumentBlock.Heading -> {
            val (hSize, hWeight) = when (block.level) {
                1 -> (fontSizeSp * 1.5f) to FontWeight.Bold
                2 -> (fontSizeSp * 1.3f) to FontWeight.Bold
                3 -> (fontSizeSp * 1.15f) to FontWeight.SemiBold
                else -> fontSizeSp to FontWeight.SemiBold
            }
            Text(
                text = block.text,
                fontSize = hSize.sp,
                fontWeight = hWeight,
                color = textColor,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }
        is TextDocumentBlock.Paragraph -> {
            Text(
                text = buildSpannedText(block.spans, textColor, searchHighlight),
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.5f).sp,
                color = textColor
            )
        }
        is TextDocumentBlock.ListItem -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (block.ordered) "${block.index}. " else "• ",
                    fontSize = fontSizeSp.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildSpannedText(block.spans, textColor, searchHighlight),
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.5f).sp,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is TextDocumentBlock.Quote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buildSpannedText(block.spans, textColor.copy(alpha = 0.85f), searchHighlight),
                    fontSize = fontSizeSp.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = (fontSizeSp * 1.5f).sp,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }
        is TextDocumentBlock.CodeBlock -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = block.code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (fontSizeSp * 0.9f).sp,
                    color = textColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        is TextDocumentBlock.Table -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                for (row in block.rows) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (cell in row) {
                            Text(
                                text = cell,
                                fontSize = (fontSizeSp * 0.95f).sp,
                                color = textColor,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                }
            }
        }
        is TextDocumentBlock.ImageBlock -> {
            if (block.localPath != null) {
                AsyncImage(
                    model = block.localPath,
                    contentDescription = block.altText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp)
                )
            }
        }
        is TextDocumentBlock.Divider -> {
            HorizontalDivider(
                color = textColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

private fun buildSpannedText(
    spans: List<TextSpan>,
    defaultColor: Color,
    searchHighlight: String?
) = buildAnnotatedString {
    for (span in spans) {
        val spanStyle = SpanStyle(
            fontWeight = if (span.isBold) FontWeight.Bold else null,
            fontStyle = if (span.isItalic) FontStyle.Italic else null,
            textDecoration = when {
                span.isUnderline && span.linkUrl != null -> TextDecoration.Underline
                span.isUnderline -> TextDecoration.Underline
                else -> null
            },
            fontFamily = if (span.isCode) FontFamily.Monospace else null,
            color = if (span.linkUrl != null) Color(0xFF1E88E5) else defaultColor
        )
        withStyle(spanStyle) {
            if (searchHighlight != null && searchHighlight.isNotBlank()) {
                val text = span.text
                var start = 0
                while (true) {
                    val idx = text.indexOf(searchHighlight, start, ignoreCase = true)
                    if (idx == -1) {
                        append(text.substring(start))
                        break
                    }
                    append(text.substring(start, idx))
                    withStyle(SpanStyle(background = Color(0xFFFFF176), color = Color.Black)) {
                        append(text.substring(idx, idx + searchHighlight.length))
                    }
                    start = idx + searchHighlight.length
                }
            } else {
                append(span.text)
            }
        }
    }
}
