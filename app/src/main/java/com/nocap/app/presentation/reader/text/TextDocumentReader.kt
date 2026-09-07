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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDocumentReader(
    book: CatalogBook,
    file: File,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }

    var document by remember { mutableStateOf<TextDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }

    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    var theme by remember { mutableStateOf(ReaderTheme.LIGHT) }

    var isBookmarked by remember { mutableStateOf(false) }
    var savedLocator by remember { mutableStateOf<TextLocator?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

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
                val locator = progress?.locatorJson?.let { TextLocator.fromJson(it) }
                savedLocator = locator

                val parsedDoc = when (book.format) {
                    PublicationFormat.TXT -> TxtParser.validateAndParse(file, book.title)
                    PublicationFormat.MARKDOWN -> MarkdownParser.validateAndParse(file, book.title)
                    PublicationFormat.HTML -> HtmlSanitizerParser.validateAndParse(file, book.title)
                    PublicationFormat.DOCX -> {
                        val cacheDir = File(context.cacheDir, "docx_media").apply { if (!exists()) mkdirs() }
                        DocxParser.parse(file, cacheDir, book.title)
                    }
                    else -> throw IllegalArgumentException("Định dạng không được hỗ trợ bởi TextDocumentReader: ${book.format}")
                }
                document = parsedDoc

                // Update last opened and reading status
                db.catalogDao().updateLastOpenedAt(book.id, System.currentTimeMillis())
                if (book.readingStatus == DocumentReadingStatus.UNREAD) {
                    db.catalogDao().updateReadingStatus(book.id, DocumentReadingStatus.READING)
                }

                // Check bookmark
                val bms = db.bookmarkDao().getBookmarksForBook(book.id)
                isBookmarked = bms.isNotEmpty()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (locator != null && locator.blockIndex > 0) {
                        scope.launch {
                            listState.scrollToItem(locator.blockIndex.coerceIn(0, (parsedDoc.blocks.size - 1).coerceAtLeast(0)))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Không thể mở tài liệu"
                    isLoading = false
                }
            }
        }
    }

    // 2. Track reading progress and save periodically
    LaunchedEffect(listState, document) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val totalBlocks = document?.blocks?.size ?: 1
                val progression = if (totalBlocks > 0) (index.toFloat() / totalBlocks).coerceIn(0f, 1f) else 0f
                val blockText = document?.blocks?.getOrNull(index)?.plainText?.take(100)

                val locator = TextLocator(
                    blockIndex = index,
                    characterOffset = 0,
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
            val idx = listState.firstVisibleItemIndex
            val totalBlocks = document?.blocks?.size ?: 1
            val progression = if (totalBlocks > 0) (idx.toFloat() / totalBlocks).coerceIn(0f, 1f) else 0f
            val locator = TextLocator(blockIndex = idx, characterOffset = 0, progression = progression)
            scope.launch(Dispatchers.IO) {
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "Lỗi tải tài liệu",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            val doc = document!!

            // Main readable content
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showControls = !showControls }
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(72.dp))
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                        }
                    },
                    actions = {
                        // Search
                        IconButton(onClick = { showSearchSheet = !showSearchSheet }) {
                            Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                        }
                        // Bookmark
                        IconButton(onClick = {
                            val currentIdx = listState.firstVisibleItemIndex
                            val blockText = doc.blocks.getOrNull(currentIdx)?.plainText?.take(100) ?: ""
                            val locator = TextLocator(blockIndex = currentIdx, characterOffset = 0, snippet = blockText)
                            scope.launch(Dispatchers.IO) {
                                db.bookmarkDao().insertBookmark(
                                    BookmarkEntity(
                                        id = UUID.randomUUID().toString(),
                                        bookId = book.id,
                                        locatorJson = locator.toJson(),
                                        chapterTitle = doc.title,
                                        snippet = blockText
                                    )
                                )
                                isBookmarked = true
                            }
                        }) {
                            Icon(
                                imageVector = if (isBookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                                contentDescription = "Đánh dấu",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Settings
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Cài đặt")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor.copy(alpha = 0.95f),
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
                                Icon(Icons.Default.Close, contentDescription = "Đóng")
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
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Trước")
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
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sau")
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
                            ThemeChip("Sepia", ReaderTheme.SEPIA, theme == ReaderTheme.SEPIA) { theme = ReaderTheme.SEPIA }
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
