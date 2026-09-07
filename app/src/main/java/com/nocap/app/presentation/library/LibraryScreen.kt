package com.nocap.app.presentation.library

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nocap.app.core.database.dao.CollectionWithBookCount
import com.nocap.app.core.database.dao.TagWithBookCount
import com.nocap.app.core.database.entity.CollectionEntity
import com.nocap.app.core.datastore.LibrarySmartView
import com.nocap.app.core.datastore.LibrarySort
import com.nocap.app.core.designsystem.AppIcons
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSource
import com.nocap.app.domain.model.Tag
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onReadBookClick: (String) -> Unit,
    onNavigateToDiscover: () -> Unit,
    viewModel: MyLibraryViewModel = viewModel(
        factory = MyLibraryViewModel.provideFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog & Sheet states
    var bookToDelete by remember { mutableStateOf<LibraryBook?>(null) }
    var showImportSourceSheet by remember { mutableStateOf(false) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var duplicateBookDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var inputUrl by remember { mutableStateOf("") }
    var inputUrlError by remember { mutableStateOf<String?>(null) }

    // Collection management states
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var collectionToRename by remember { mutableStateOf<CollectionWithBookCount?>(null) }
    var renameCollectionName by remember { mutableStateOf("") }
    var collectionToDelete by remember { mutableStateOf<CollectionWithBookCount?>(null) }
    var activeCollectionDetail by remember { mutableStateOf<CollectionWithBookCount?>(null) }
    var bookForCollectionAssignment by remember { mutableStateOf<LibraryBook?>(null) }

    // Tag management states
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var tagToRename by remember { mutableStateOf<TagWithBookCount?>(null) }
    var renameTagName by remember { mutableStateOf("") }
    var tagToDelete by remember { mutableStateOf<TagWithBookCount?>(null) }
    var activeTagDetail by remember { mutableStateOf<TagWithBookCount?>(null) }
    var bookForTagAssignment by remember { mutableStateOf<LibraryBook?>(null) }

    // Metadata editor state
    var bookForMetadataEdit by remember { mutableStateOf<LibraryBook?>(null) }

    // Bulk action dialog states
    var showBulkTagDialog by remember { mutableStateOf(false) }
    var showBulkCollectionDialog by remember { mutableStateOf(false) }
    var showBulkStatusDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onImportPublication(PublicationSource.LocalUri(uri))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ImportSuccess -> {
                    snackbarHostState.showSnackbar("Đã thêm \"${event.bookTitle}\" vào thư viện!")
                }
                is LibraryEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is LibraryEvent.DuplicateFound -> {
                    duplicateBookDialog = event.bookId to event.title
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Đã chọn: ${uiState.selectedBookIds.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::exitSelectionMode) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng chọn")
                        }
                    },
                    actions = {
                        TextButton(onClick = viewModel::selectAll) {
                            Text("Tất cả", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Tài liệu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        when (uiState.selectedTab) {
                            LibraryTab.COLLECTIONS -> {
                                IconButton(onClick = {
                                    newCollectionName = ""
                                    showCreateCollectionDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Tạo bộ sưu tập")
                                }
                            }
                            LibraryTab.TAGS -> {
                                IconButton(onClick = {
                                    newTagName = ""
                                    showCreateTagDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Tạo thẻ")
                                }
                            }
                            else -> {
                                IconButton(onClick = { showImportSourceSheet = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Thêm tài liệu")
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (uiState.isSelectionMode && uiState.selectedBookIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showBulkTagDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(AppIcons.Tag, contentDescription = "Gắn thẻ", modifier = Modifier.size(20.dp))
                                Text("Thẻ", fontSize = 10.sp)
                            }
                        }
                        IconButton(onClick = { showBulkCollectionDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Bộ sưu tập", modifier = Modifier.size(20.dp))
                                Text("Bộ sưu tập", fontSize = 10.sp)
                            }
                        }
                        IconButton(onClick = { showBulkStatusDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Trạng thái", modifier = Modifier.size(20.dp))
                                Text("Trạng thái", fontSize = 10.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.bulkSetPinned(true) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(AppIcons.Pin, contentDescription = "Ghim", modifier = Modifier.size(20.dp))
                                Text("Ghim", fontSize = 10.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.bulkSetArchived(true) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Clear, contentDescription = "Lưu trữ", modifier = Modifier.size(20.dp))
                                Text("Lưu trữ", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                when (uiState.selectedTab) {
                    LibraryTab.COLLECTIONS -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                newCollectionName = ""
                                showCreateCollectionDialog = true
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Tạo bộ sưu tập") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    LibraryTab.TAGS -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                newTagName = ""
                                showCreateTagDialog = true
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Tạo thẻ mới") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    else -> {
                        ExtendedFloatingActionButton(
                            onClick = { showImportSourceSheet = true },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Thêm tài liệu") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = uiState.selectedTab == LibraryTab.ALL,
                    onClick = { viewModel.onTabChange(LibraryTab.ALL) },
                    text = { Text("Tất cả", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = uiState.selectedTab == LibraryTab.INBOX,
                    onClick = { viewModel.onTabChange(LibraryTab.INBOX) },
                    text = {
                        Text(
                            if (uiState.inboxCount > 0) "Inbox (${uiState.inboxCount})" else "Inbox",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == LibraryTab.COLLECTIONS,
                    onClick = { viewModel.onTabChange(LibraryTab.COLLECTIONS) },
                    text = { Text("Bộ sưu tập (${uiState.collections.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = uiState.selectedTab == LibraryTab.TAGS,
                    onClick = { viewModel.onTabChange(LibraryTab.TAGS) },
                    text = { Text("Thẻ (${uiState.tags.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (uiState.selectedTab) {
                LibraryTab.ALL, LibraryTab.INBOX -> {
                    // Search Bar
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Tìm tên, tác giả, thẻ, bộ sưu tập...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Tìm kiếm",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Smart Views and Sort (in ALL tab)
                    if (uiState.selectedTab == LibraryTab.ALL) {
                        SmartViewsAndSortRow(
                            selectedSmartView = uiState.selectedSmartView,
                            selectedSort = uiState.selectedSort,
                            onSmartViewSelected = viewModel::onSmartViewChange,
                            onSortSelected = viewModel::onSortChange
                        )
                    } else {
                        // Inbox header reminder
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tài liệu mới nhập sẽ ở đây. Nhấn \"Hoàn thành\" khi bạn đã phân loại xong.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (uiState.isImporting) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val progress = uiState.importProgress
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (progress != null) "Đang tải tài liệu..." else "Đang xử lý tệp...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = viewModel::cancelImport) {
                                        Text("Hủy", color = MaterialTheme.colorScheme.error)
                                    }
                                }

                                if (progress != null) {
                                    val mbRead = "%.1f".format(progress.bytesRead / (1024f * 1024f))
                                    if (progress.totalBytes > 0) {
                                        val mbTotal = "%.1f".format(progress.totalBytes / (1024f * 1024f))
                                        val pct = ((progress.percentage ?: 0f) * 100).toInt()
                                        LinearProgressIndicator(
                                            progress = { progress.percentage ?: 0f },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "$mbRead MB / $mbTotal MB ($pct%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        Text(
                                            text = "$mbRead MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }

                    if (uiState.isLoading && uiState.books.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.books.isEmpty()) {
                        EmptyDocumentsView(
                            tab = uiState.selectedTab,
                            smartView = uiState.selectedSmartView,
                            searchQuery = uiState.searchQuery,
                            onImportClick = { showImportSourceSheet = true },
                            onNavigateToDiscover = onNavigateToDiscover
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = uiState.books, key = { it.book.id }) { libraryBook ->
                                val isSelected = uiState.selectedBookIds.contains(libraryBook.book.id)
                                DocumentCardItem(
                                    libraryBook = libraryBook,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleBookSelection(libraryBook.book.id)
                                        } else {
                                            if (libraryBook.book.categoryId == "imported") {
                                                onReadBookClick(libraryBook.book.id)
                                            } else {
                                                onBookClick(libraryBook.book.id)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode(libraryBook.book.id)
                                        }
                                    },
                                    onReadClick = { onReadBookClick(libraryBook.book.id) },
                                    onToggleFavorite = { viewModel.onToggleFavorite(libraryBook.book.id) },
                                    onTogglePin = { viewModel.onTogglePin(libraryBook.book.id, libraryBook.book.isPinned) },
                                    onToggleArchive = { viewModel.onToggleArchive(libraryBook.book.id, libraryBook.book.isArchived) },
                                    onSetInbox = { inInbox -> viewModel.onSetInboxState(libraryBook.book.id, inInbox) },
                                    onSetStatus = { status -> viewModel.onSetReadingStatus(libraryBook.book.id, status) },
                                    onEditMetadata = { bookForMetadataEdit = libraryBook },
                                    onManageTags = { bookForTagAssignment = libraryBook },
                                    onAddToCollection = { bookForCollectionAssignment = libraryBook },
                                    onDeleteBook = { bookToDelete = libraryBook }
                                )
                            }
                        }
                    }
                }

                LibraryTab.COLLECTIONS -> {
                    CollectionsView(
                        collections = uiState.collections,
                        onCreateClick = {
                            newCollectionName = ""
                            showCreateCollectionDialog = true
                        },
                        onCollectionClick = { collection ->
                            activeCollectionDetail = collection
                        },
                        onRenameClick = { collection ->
                            collectionToRename = collection
                            renameCollectionName = collection.name
                        },
                        onDeleteClick = { collection ->
                            collectionToDelete = collection
                        }
                    )
                }

                LibraryTab.TAGS -> {
                    TagsView(
                        tags = uiState.tags,
                        onCreateClick = {
                            newTagName = ""
                            showCreateTagDialog = true
                        },
                        onTagClick = { tag ->
                            activeTagDetail = tag
                        },
                        onRenameClick = { tag ->
                            tagToRename = tag
                            renameTagName = tag.name
                        },
                        onDeleteClick = { tag ->
                            tagToDelete = tag
                        }
                    )
                }
            }
        }
    }

    // Sheet: Active Collection Detail
    activeCollectionDetail?.let { col ->
        val collectionBooksFlow = remember(col.id) { viewModel.observeBooksInCollection(col.id) }
        val collectionBooks by collectionBooksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { activeCollectionDetail = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${collectionBooks.size} tài liệu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            collectionToRename = col
                            renameCollectionName = col.name
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Đổi tên")
                        }
                        IconButton(onClick = { collectionToDelete = col }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (collectionBooks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có tài liệu nào trong bộ sưu tập này.",
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
                            .height(380.dp)
                    ) {
                        items(collectionBooks, key = { it.book.id }) { book ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeCollectionDetail = null
                                        onReadBookClick(book.book.id)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BookThumbnail(book = book.book, modifier = Modifier.size(44.dp, 64.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.book.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = book.book.displayAuthor,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.removeBookFromCollection(book.book.id, col.id)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Xóa khỏi bộ sưu tập",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheet: Active Tag Detail
    activeTagDetail?.let { tagWithCount ->
        val tagBooksFlow = remember(tagWithCount.id) { viewModel.observeBooksForTag(tagWithCount.id) }
        val tagBooks by tagBooksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { activeTagDetail = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${tagWithCount.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${tagBooks.size} tài liệu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            tagToRename = tagWithCount
                            renameTagName = tagWithCount.name
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Đổi tên")
                        }
                        IconButton(onClick = { tagToDelete = tagWithCount }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (tagBooks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có tài liệu nào gắn thẻ này.",
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
                            .height(380.dp)
                    ) {
                        items(tagBooks, key = { it.book.id }) { book ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTagDetail = null
                                        onReadBookClick(book.book.id)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BookThumbnail(book = book.book, modifier = Modifier.size(44.dp, 64.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.book.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = book.book.displayAuthor,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Book For Collection Assignment
    bookForCollectionAssignment?.let { targetBook ->
        val bookCollectionsFlow = remember(targetBook.book.id) { viewModel.getCollectionsForBook(targetBook.book.id) }
        val currentAssigned by bookCollectionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        var selectedIds by remember { mutableStateOf(setOf<String>()) }

        LaunchedEffect(currentAssigned) {
            selectedIds = currentAssigned.map { it.id }.toSet()
        }

        AlertDialog(
            onDismissRequest = { bookForCollectionAssignment = null },
            title = { Text("Bộ sưu tập tài liệu") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Chọn các bộ sưu tập cho: \"${targetBook.book.displayTitle}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (uiState.collections.isEmpty()) {
                        Text(
                            text = "Chưa có bộ sưu tập nào. Hãy tạo bộ sưu tập mới trước.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(uiState.collections, key = { it.id }) { col ->
                                val isChecked = selectedIds.contains(col.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (isChecked) selectedIds - col.id else selectedIds + col.id
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) selectedIds + col.id else selectedIds - col.id
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = col.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBookCollections(targetBook.book.id, selectedIds)
                        bookForCollectionAssignment = null
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookForCollectionAssignment = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Tag Assignment For Single Book
    bookForTagAssignment?.let { targetBook ->
        val bookTagsFlow = remember(targetBook.book.id) { viewModel.getTagsForBook(targetBook.book.id) }
        val currentAssignedTags by bookTagsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        var selectedTagIds by remember { mutableStateOf(setOf<String>()) }
        var quickTagName by remember { mutableStateOf("") }

        LaunchedEffect(currentAssignedTags) {
            selectedTagIds = currentAssignedTags.map { it.id }.toSet()
        }

        AlertDialog(
            onDismissRequest = { bookForTagAssignment = null },
            title = { Text("Gắn thẻ tài liệu") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Chọn thẻ cho: \"${targetBook.book.displayTitle}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Quick create tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = quickTagName,
                            onValueChange = { quickTagName = it },
                            placeholder = { Text("Tạo thẻ mới...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (quickTagName.isNotBlank()) {
                                    viewModel.createTag(quickTagName.trim()) { result ->
                                        result.onSuccess { newId ->
                                            selectedTagIds = selectedTagIds + newId
                                        }
                                    }
                                    quickTagName = ""
                                }
                            },
                            enabled = quickTagName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.tags.isEmpty()) {
                        Text(
                            text = "Chưa có thẻ nào. Nhập tên ở trên để tạo thẻ mới.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(uiState.tags, key = { it.id }) { tag ->
                                val isChecked = selectedTagIds.contains(tag.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTagIds = if (isChecked) selectedTagIds - tag.id else selectedTagIds + tag.id
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedTagIds = if (checked) selectedTagIds + tag.id else selectedTagIds - tag.id
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "#${tag.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBookTags(targetBook.book.id, selectedTagIds.toList())
                        bookForTagAssignment = null
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookForTagAssignment = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Metadata Editor
    bookForMetadataEdit?.let { targetBook ->
        var titleInput by remember(targetBook) {
            mutableStateOf(targetBook.book.userTitleOverride ?: targetBook.book.title)
        }
        var authorInput by remember(targetBook) {
            mutableStateOf(targetBook.book.userAuthorOverride ?: targetBook.book.author)
        }
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.onSetCustomCover(targetBook.book.id, uri, context)
            }
        }

        AlertDialog(
            onDismissRequest = { bookForMetadataEdit = null },
            title = { Text("Sửa thông tin tài liệu") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Override
                    Column {
                        Text("Tiêu đề hiển thị:", style = MaterialTheme.typography.labelMedium)
                        TextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (targetBook.book.userTitleOverride != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gốc: ${targetBook.book.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { titleInput = targetBook.book.title }) {
                                    Text("Đặt lại", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Author Override
                    Column {
                        Text("Tác giả hiển thị:", style = MaterialTheme.typography.labelMedium)
                        TextField(
                            value = authorInput,
                            onValueChange = { authorInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (targetBook.book.userAuthorOverride != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gốc: ${targetBook.book.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { authorInput = targetBook.book.author }) {
                                    Text("Đặt lại", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Custom Cover
                    Column {
                        Text("Ảnh bìa:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chọn ảnh mới")
                            }
                            if (targetBook.book.customCoverPath != null) {
                                TextButton(onClick = { viewModel.onRemoveCustomCover(targetBook.book.id, context) }) {
                                    Text("Khôi phục bìa gốc", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = if (titleInput.trim() == targetBook.book.title) null else titleInput.trim()
                        val finalAuthor = if (authorInput.trim() == targetBook.book.author) null else authorInput.trim()
                        viewModel.onUpdateMetadata(targetBook.book.id, finalTitle, finalAuthor)
                        bookForMetadataEdit = null
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookForMetadataEdit = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Bulk Tag
    if (showBulkTagDialog) {
        AlertDialog(
            onDismissRequest = { showBulkTagDialog = false },
            title = { Text("Gắn thẻ hàng loạt") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn thẻ để gắn cho ${uiState.selectedBookIds.size} tài liệu đã chọn:")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.tags.isEmpty()) {
                        Text("Chưa có thẻ nào. Hãy tạo thẻ trong tab Thẻ trước.")
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(uiState.tags, key = { it.id }) { tag ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.bulkAddTag(tag.id)
                                            showBulkTagDialog = false
                                        }
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(AppIcons.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("#${tag.name}", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkTagDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Bulk Collection
    if (showBulkCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showBulkCollectionDialog = false },
            title = { Text("Thêm vào bộ sưu tập hàng loạt") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn bộ sưu tập cho ${uiState.selectedBookIds.size} tài liệu:")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.collections.isEmpty()) {
                        Text("Chưa có bộ sưu tập nào.")
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(uiState.collections, key = { it.id }) { col ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.bulkAddCollection(col.id)
                                            showBulkCollectionDialog = false
                                        }
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(col.name, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkCollectionDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Bulk Status
    if (showBulkStatusDialog) {
        AlertDialog(
            onDismissRequest = { showBulkStatusDialog = false },
            title = { Text("Đổi trạng thái đọc") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentReadingStatus.entries.forEach { status ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.bulkSetReadingStatus(status)
                                    showBulkStatusDialog = false
                                }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = status.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkStatusDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Create Tag
    if (showCreateTagDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = { Text("Tạo thẻ mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập tên thẻ (tự động chuẩn hóa chữ thường):")
                    TextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("vd: kinh-te, triet-hoc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.createTag(newTagName.trim())
                            showCreateTagDialog = false
                        }
                    },
                    enabled = newTagName.isNotBlank()
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTagDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Rename Tag
    tagToRename?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = { Text("Đổi tên thẻ") },
            text = {
                TextField(
                    value = renameTagName,
                    onValueChange = { renameTagName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameTagName.isNotBlank()) {
                            viewModel.renameTag(tag.id, renameTagName.trim())
                            tagToRename = null
                        }
                    },
                    enabled = renameTagName.isNotBlank()
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToRename = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Delete Tag
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Xóa thẻ: #${tag.name}?") },
            text = {
                Text("Thẻ sẽ bị xóa khỏi hệ thống nhưng tất cả tài liệu vẫn được giữ nguyên vẹn trong thư viện.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag.id)
                        if (activeTagDetail?.id == tag.id) {
                            activeTagDetail = null
                        }
                        tagToDelete = null
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Create Collection
    if (showCreateCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = { Text("Tạo bộ sưu tập mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập tên bộ sưu tập:")
                    TextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = { Text("Tên bộ sưu tập") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.createCollection(newCollectionName.trim())
                            showCreateCollectionDialog = false
                        }
                    },
                    enabled = newCollectionName.isNotBlank()
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Rename Collection
    collectionToRename?.let { col ->
        AlertDialog(
            onDismissRequest = { collectionToRename = null },
            title = { Text("Đổi tên bộ sưu tập") },
            text = {
                TextField(
                    value = renameCollectionName,
                    onValueChange = { renameCollectionName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameCollectionName.isNotBlank()) {
                            viewModel.renameCollection(col.id, renameCollectionName.trim())
                            collectionToRename = null
                        }
                    },
                    enabled = renameCollectionName.isNotBlank()
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToRename = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Delete Collection
    collectionToDelete?.let { col ->
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Xóa bộ sưu tập: " + col.name + "?") },
            text = {
                Text("Bộ sưu tập sẽ bị xóa nhưng các tài liệu bên trong vẫn được giữ nguyên vẹn trong thư viện.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCollection(col.id)
                        if (activeCollectionDetail?.id == col.id) {
                            activeCollectionDetail = null
                        }
                        collectionToDelete = null
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Sheet: Import Source Picker
    if (showImportSourceSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showImportSourceSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Thêm tài liệu mới",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showImportSourceSheet = false
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/epub+zip",
                                    "application/pdf",
                                    "text/plain",
                                    "text/markdown",
                                    "text/html",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/jpeg",
                                    "image/png",
                                    "image/webp",
                                    "application/x-cbz",
                                    "application/zip",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Từ thiết bị",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Chọn tệp EPUB hoặc PDF có sẵn trên máy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showImportSourceSheet = false
                            inputUrl = ""
                            inputUrlError = null
                            showUrlInputDialog = true
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Từ liên kết HTTPS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tải tài liệu trực tiếp từ liên kết mạng an toàn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: URL Input
    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("Nhập tài liệu từ liên kết") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nhập liên kết tải tài liệu trực tiếp (.epub hoặc .pdf). Chỉ hỗ trợ giao thức bảo mật HTTPS.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextField(
                        value = inputUrl,
                        onValueChange = {
                            inputUrl = it
                            inputUrlError = null
                        },
                        placeholder = { Text("https://example.com/document.epub") },
                        singleLine = true,
                        isError = inputUrlError != null,
                        supportingText = inputUrlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inputUrl.trim()
                        if (!trimmed.startsWith("https://", ignoreCase = true)) {
                            inputUrlError = "Chỉ chấp nhận liên kết HTTPS an toàn"
                        } else {
                            showUrlInputDialog = false
                            viewModel.onImportPublication(PublicationSource.RemoteUrl(trimmed))
                        }
                    }
                ) {
                    Text("Tải về")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlInputDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Duplicate Found
    duplicateBookDialog?.let { (existingBookId, existingTitle) ->
        AlertDialog(
            onDismissRequest = { duplicateBookDialog = null },
            title = { Text("Tài liệu đã tồn tại") },
            text = {
                Text("Tài liệu: \"$existingTitle\" đã có trong thư viện của bạn.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        duplicateBookDialog = null
                        onReadBookClick(existingBookId)
                    }
                ) {
                    Text("Mở tài liệu")
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateBookDialog = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Dialog: Delete Book
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Xóa tài liệu khỏi thư viện?") },
            text = {
                Text("Tài liệu: \"${book.book.displayTitle}\" và toàn bộ dấu trang, ghi chú, tiến độ đọc sẽ bị xóa vĩnh viễn khỏi thiết bị.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteImportedBook(book.book.id)
                        bookToDelete = null
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun SmartViewsAndSortRow(
    selectedSmartView: LibrarySmartView,
    selectedSort: LibrarySort,
    onSmartViewSelected: (LibrarySmartView) -> Unit,
    onSortSelected: (LibrarySort) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(LibrarySmartView.entries.toTypedArray()) { smartView ->
                FilterChip(
                    selected = selectedSmartView == smartView,
                    onClick = { onSmartViewSelected(smartView) },
                    label = { Text(smartView.displayName, fontSize = 12.sp) }
                )
            }
        }

        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = AppIcons.Sort,
                    contentDescription = "Sắp xếp",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                LibrarySort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sort.displayName,
                                fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSortSelected(sort)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentCardItem(
    libraryBook: LibraryBook,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReadClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onSetInbox: (Boolean) -> Unit,
    onSetStatus: (DocumentReadingStatus) -> Unit,
    onEditMetadata: () -> Unit,
    onManageTags: () -> Unit,
    onAddToCollection: () -> Unit,
    onDeleteBook: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val book = libraryBook.book

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            BookThumbnail(
                book = book,
                modifier = Modifier
                    .width(70.dp)
                    .height(104.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header row: Title + Pin/Favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = book.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (book.isPinned) {
                            Icon(
                                imageVector = AppIcons.Pin,
                                contentDescription = "Đã ghim",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (libraryBook.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Yêu thích",
                                tint = if (libraryBook.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = book.displayAuthor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Badges row: Format + Status + Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Format badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (book.format) {
                            PublicationFormat.PDF -> MaterialTheme.colorScheme.errorContainer
                            PublicationFormat.EPUB -> MaterialTheme.colorScheme.primaryContainer
                            PublicationFormat.CBZ -> MaterialTheme.colorScheme.tertiaryContainer
                            PublicationFormat.TXT,
                            PublicationFormat.MARKDOWN,
                            PublicationFormat.HTML,
                            PublicationFormat.DOCX -> MaterialTheme.colorScheme.secondaryContainer
                            PublicationFormat.JPEG,
                            PublicationFormat.PNG,
                            PublicationFormat.WEBP -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = book.format.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (book.format) {
                                PublicationFormat.PDF -> MaterialTheme.colorScheme.onErrorContainer
                                PublicationFormat.EPUB -> MaterialTheme.colorScheme.onPrimaryContainer
                                PublicationFormat.CBZ -> MaterialTheme.colorScheme.onTertiaryContainer
                                PublicationFormat.TXT,
                                PublicationFormat.MARKDOWN,
                                PublicationFormat.HTML,
                                PublicationFormat.DOCX -> MaterialTheme.colorScheme.onSecondaryContainer
                                PublicationFormat.JPEG,
                                PublicationFormat.PNG,
                                PublicationFormat.WEBP -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Reading status badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (book.readingStatus) {
                            DocumentReadingStatus.UNREAD -> MaterialTheme.colorScheme.surfaceVariant
                            DocumentReadingStatus.READING -> MaterialTheme.colorScheme.secondaryContainer
                            DocumentReadingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = book.readingStatus.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (book.readingStatus) {
                                DocumentReadingStatus.UNREAD -> MaterialTheme.colorScheme.onSurfaceVariant
                                DocumentReadingStatus.READING -> MaterialTheme.colorScheme.onSecondaryContainer
                                DocumentReadingStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Tag chips (first 2 tags)
                    libraryBook.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (libraryBook.tags.size > 2) {
                        Text(
                            text = "+${libraryBook.tags.size - 2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Progress Indicator
                val progress = libraryBook.readingProgress
                if (progress != null && progress.progression > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress.progression },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = "${(progress.progression * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action buttons row: Read button + Inbox quick action + 3-dots Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReadClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        val isReading = progress != null && progress.progression > 0f
                        Text(
                            text = if (isReading) "Tiếp tục đọc" else "Đọc tài liệu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (book.isInInbox) {
                            FilledTonalButton(
                                onClick = { onSetInbox(false) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp).padding(end = 4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hoàn thành", fontSize = 11.sp)
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Tùy chọn",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = {
                                    showMenu = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Đọc tài liệu") },
                                    leadingIcon = { Icon(AppIcons.Book, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onReadClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sửa thông tin") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onEditMetadata()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gắn thẻ") },
                                    leadingIcon = { Icon(AppIcons.Tag, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onManageTags()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bộ sưu tập") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onAddToCollection()
                                    }
                                )

                                HorizontalDivider()

                                // Status options
                                DropdownMenuItem(
                                    text = { Text("Đánh dấu: Chưa đọc") },
                                    leadingIcon = {
                                        if (book.readingStatus == DocumentReadingStatus.UNREAD) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onSetStatus(DocumentReadingStatus.UNREAD)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Đánh dấu: Đang đọc") },
                                    leadingIcon = {
                                        if (book.readingStatus == DocumentReadingStatus.READING) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onSetStatus(DocumentReadingStatus.READING)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Đánh dấu: Hoàn thành") },
                                    leadingIcon = {
                                        if (book.readingStatus == DocumentReadingStatus.COMPLETED) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onSetStatus(DocumentReadingStatus.COMPLETED)
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(if (book.isPinned) "Bỏ ghim" else "Ghim tài liệu") },
                                    leadingIcon = { Icon(AppIcons.Pin, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onTogglePin()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (book.isInInbox) "Đánh dấu đã tổ chức" else "Chuyển vào Inbox") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onSetInbox(!book.isInInbox)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (book.isArchived) "Bỏ lưu trữ" else "Lưu trữ tài liệu") },
                                    leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onToggleArchive()
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Xóa khỏi thư viện", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteBook()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookThumbnail(
    book: com.nocap.app.domain.model.CatalogBook,
    modifier: Modifier = Modifier
) {
    val coverModel = book.customCoverPath ?: book.coverUrl.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (coverModel != null) {
            AsyncImage(
                model = coverModel,
                contentDescription = book.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.displayTitle,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CollectionsView(
    collections: List<CollectionWithBookCount>,
    onCreateClick: () -> Unit,
    onCollectionClick: (CollectionWithBookCount) -> Unit,
    onRenameClick: (CollectionWithBookCount) -> Unit,
    onDeleteClick: (CollectionWithBookCount) -> Unit
) {
    if (collections.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chưa có bộ sưu tập nào",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tạo bộ sưu tập để phân loại và sắp xếp tài liệu của bạn gọn gàng hơn.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onCreateClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo bộ sưu tập mới")
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(collections, key = { it.id }) { col ->
                var showColMenu by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCollectionClick(col) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = col.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${col.bookCount} tài liệu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(onClick = { showColMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                            }
                            DropdownMenu(
                                expanded = showColMenu,
                                onDismissRequest = { showColMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Đổi tên") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showColMenu = false
                                        onRenameClick(col)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa bộ sưu tập", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showColMenu = false
                                        onDeleteClick(col)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagsView(
    tags: List<TagWithBookCount>,
    onCreateClick: () -> Unit,
    onTagClick: (TagWithBookCount) -> Unit,
    onRenameClick: (TagWithBookCount) -> Unit,
    onDeleteClick: (TagWithBookCount) -> Unit
) {
    if (tags.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = AppIcons.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chưa có thẻ nào",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gắn thẻ tài liệu để tìm kiếm đa chiều và phân loại kiến thức theo chủ đề.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onCreateClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo thẻ mới")
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tags, key = { it.id }) { tag ->
                var showTagMenu by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagClick(tag) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${tag.bookCount} tài liệu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(onClick = { showTagMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                            }
                            DropdownMenu(
                                expanded = showTagMenu,
                                onDismissRequest = { showTagMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Đổi tên") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showTagMenu = false
                                        onRenameClick(tag)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa thẻ", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showTagMenu = false
                                        onDeleteClick(tag)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDocumentsView(
    tab: LibraryTab,
    smartView: LibrarySmartView,
    searchQuery: String,
    onImportClick: () -> Unit,
    onNavigateToDiscover: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = AppIcons.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (searchQuery.isNotBlank()) {
                Text(
                    text = "Không tìm thấy tài liệu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Không có kết quả nào phù hợp với: \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (tab == LibraryTab.INBOX) {
                Text(
                    text = "Hộp thư đến trống",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tuyệt vời! Tất cả tài liệu của bạn đã được sắp xếp và tổ chức.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (smartView == LibrarySmartView.ARCHIVED) {
                Text(
                    text = "Không có tài liệu lưu trữ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Các tài liệu đã đọc xong hoặc muốn ẩn đi sẽ hiển thị ở đây.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Thư viện tài liệu của bạn đang trống",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhập tài liệu EPUB hoặc PDF từ thiết bị hoặc tải từ liên kết để bắt đầu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onImportClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhập tài liệu")
                    }
                    OutlinedButton(onClick = onNavigateToDiscover) {
                        Text("Khám phá sách")
                    }
                }
            }
        }
    }
}
