package com.nocap.app.presentation.reader.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.nocap.app.core.localization.Text
import com.nocap.app.core.localization.localize
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.ReadingProgressEntity
import com.nocap.app.core.designsystem.AppIcons
import com.nocap.app.data.parser.CbzParser
import com.nocap.app.domain.model.ArchiveLocator
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DocumentReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageArchiveReader(
    book: CatalogBook,
    file: File,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val sessionManager = remember { com.nocap.app.domain.session.ReadingSessionManager.getInstance(context.applicationContext) }
    var sessionId by remember { mutableStateOf<String?>(null) }

    var pages by remember { mutableStateOf<List<CbzParser.CbzPage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var positionRestored by remember { mutableStateOf(false) }
    var restoredPage by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var isBookmarked by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    DisposableEffect(Unit) {
        onDispose {
            val total = pages.size
            val prog = if (total > 0) (pagerState.currentPage.toFloat() / total).coerceIn(0f, 1f) else 0f
            sessionId?.let { sId ->
                sessionManager.endSessionAsync(sId, prog)
            }
        }
    }

    // 1. Enumerate pages and restore saved progress
    LaunchedEffect(book.id) {
        withContext(Dispatchers.IO) {
            try {
                val pageList = CbzParser.validateAndListPages(file)
                pages = pageList

                db.catalogDao().updateLastOpenedAt(book.id, System.currentTimeMillis())
                if (book.readingStatus == DocumentReadingStatus.UNREAD) {
                    db.catalogDao().updateReadingStatus(book.id, DocumentReadingStatus.READING)
                }

                val progress = db.progressDao().getProgress(book.id)
                val locator = progress?.locatorJson?.let { ArchiveLocator.fromJson(it) }

                sessionId = sessionManager.startSession(
                    bookId = book.id,
                    format = book.format.name,
                    startProgress = locator?.progression ?: 0f
                )

                val bms = db.bookmarkDao().getBookmarksForBook(book.id)

                isBookmarked = bms.isNotEmpty()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    restoredPage = locator?.pageIndex?.coerceIn(0, pageList.lastIndex.coerceAtLeast(0)) ?: 0
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Không thể mở tệp CBZ"
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(pages, isLoading) {
        if (isLoading || pages.isEmpty() || errorMessage != null) return@LaunchedEffect
        pagerState.scrollToPage(restoredPage)
        positionRestored = true
    }

    // 2. Track page changes and save progress
    LaunchedEffect(pagerState, pages, positionRestored) {
        if (!positionRestored) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIdx ->
                if (pages.isEmpty()) return@collect
                val currentPage = pages.getOrNull(pageIdx) ?: return@collect
                val progression = (pageIdx.toFloat() + 1f) / pages.size
                val locator = ArchiveLocator(
                    pageIndex = pageIdx,
                    entryName = currentPage.entryName,
                    progression = progression
                )

                withContext(Dispatchers.IO) {
                    db.progressDao().saveProgress(
                        ReadingProgressEntity(
                            bookId = book.id,
                            locatorJson = locator.toJson(),
                            progression = progression,
                            chapterTitle = "Trang ${pageIdx + 1}",
                            lastReadAt = System.currentTimeMillis()
                        )
                    )
                }
            }
    }

    // Save on dispose
    DisposableEffect(Unit) {
        onDispose {
            if (positionRestored && pages.isNotEmpty()) {
                val pageIdx = pagerState.currentPage
                val currentPage = pages.getOrNull(pageIdx)
                if (currentPage != null) {
                    val progression = (pageIdx.toFloat() + 1f) / pages.size
                    val locator = ArchiveLocator(
                        pageIndex = pageIdx,
                        entryName = currentPage.entryName,
                        progression = progression
                    )
                    com.nocap.app.domain.session.ReadingSessionManager.processScope.launch {
                        db.progressDao().saveProgress(
                            ReadingProgressEntity(
                                bookId = book.id,
                                locatorJson = locator.toJson(),
                                progression = progression,
                                chapterTitle = "Trang ${pageIdx + 1}",
                                lastReadAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "Lỗi tải tệp CBZ",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else if (pages.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIdx ->
                val page = pages[pageIdx]
                var pageFile by remember { mutableStateOf<File?>(null) }

                LaunchedEffect(page.entryName) {
                    withContext(Dispatchers.IO) {
                        val cacheFile = File(context.cacheDir, "cbz_page_${book.id}_$pageIdx.jpg")
                        if (!cacheFile.exists()) {
                            try {
                                CbzParser.extractPageToFile(file, page.entryName, cacheFile)
                            } catch (_: Exception) {}
                        }
                        pageFile = cacheFile
                    }
                }

                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    if (scale == 1f) {
                        offset = Offset.Zero
                    } else {
                        offset += offsetChange
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .transformable(state = transformState),
                    contentAlignment = Alignment.Center
                ) {
                    if (pageFile != null) {
                        AsyncImage(
                            model = pageFile,
                            contentDescription = localize("Trang ${pageIdx + 1}"),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Top Bar
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = book.displayTitle,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localize("Trở về"), tint = Color.White)
                        }
                    },
                    actions = {
                        // Bookmark
                        IconButton(onClick = {
                            val currentIdx = pagerState.currentPage
                            val page = pages.getOrNull(currentIdx)
                            if (page != null) {
                                val locator = ArchiveLocator(
                                    pageIndex = currentIdx,
                                    entryName = page.entryName,
                                    progression = (currentIdx + 1).toFloat() / pages.size
                                )
                                scope.launch(Dispatchers.IO) {
                                    db.bookmarkDao().insertBookmark(
                                        BookmarkEntity(
                                            id = UUID.randomUUID().toString(),
                                            bookId = book.id,
                                            locatorJson = locator.toJson(),
                                            chapterTitle = "Trang ${currentIdx + 1}",
                                            snippet = page.fileName
                                        )
                                    )
                                    isBookmarked = true
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isBookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                                contentDescription = localize("Đánh dấu trang"),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.8f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }

            // Bottom Navigation & Page Indicator
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = localize("Trang trước"), tint = Color.White)
                        }

                        Text(
                            text = "${pagerState.currentPage + 1} / ${pages.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < pages.size - 1) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            },
                            enabled = pagerState.currentPage < pages.size - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = localize("Trang sau"), tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
