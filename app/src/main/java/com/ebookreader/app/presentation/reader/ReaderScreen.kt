package com.ebookreader.app.presentation.reader

import android.view.View
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ebookreader.app.domain.model.TocItem
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

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
    var showTocSheet by remember { mutableStateOf(false) }
    var navigatorFragment by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var lastKnownLocator by remember { mutableStateOf<Locator?>(null) }

    val handleBack: () -> Unit = {
        viewModel.saveCurrentLocationImmediately(lastKnownLocator ?: navigatorFragment?.currentLocator?.value)
        onBackClick()
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val s = uiState) {
                            is ReaderUiState.Ready -> s.bookTitle
                            else -> "Đọc sách"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (uiState is ReaderUiState.Ready) {
                        IconButton(onClick = { showTocSheet = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Mục lục")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            sheetState = sheetState
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
                        initialLocator = initialLocator
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
