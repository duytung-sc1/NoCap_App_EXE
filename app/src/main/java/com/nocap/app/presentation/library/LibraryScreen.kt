package com.nocap.app.presentation.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import com.nocap.app.core.designsystem.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nocap.app.core.database.entity.CollectionEntity
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onReadBookClick: (String) -> Unit,
    onNavigateToDiscover: () -> Unit,
    viewModel: MyLibraryViewModel = viewModel(
        factory = MyLibraryViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var bookToDelete by remember { mutableStateOf<LibraryBook?>(null) }
    var showImportSourceSheet by remember { mutableStateOf(false) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var duplicateBookDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var inputUrl by remember { mutableStateOf("") }
    var inputUrlError by remember { mutableStateOf<String?>(null) }

    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var collectionToRename by remember { mutableStateOf<CollectionWithBookCount?>(null) }
    var renameCollectionName by remember { mutableStateOf("") }
    var collectionToDelete by remember { mutableStateOf<CollectionWithBookCount?>(null) }
    var activeCollectionDetail by remember { mutableStateOf<CollectionWithBookCount?>(null) }
    var bookForCollectionAssignment by remember { mutableStateOf<LibraryBook?>(null) }

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
            TopAppBar(
                title = {
                    Text(
                        text = "Thư viện của tôi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (uiState.selectedTab == LibraryTab.COLLECTIONS) {
                        IconButton(onClick = {
                            newCollectionName = ""
                            showCreateCollectionDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Tạo bộ sưu tập")
                        }
                    } else {
                        IconButton(onClick = { showImportSourceSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm sách")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == LibraryTab.COLLECTIONS) {
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
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showImportSourceSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Thêm sách") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                selectedTabIndex = if (uiState.selectedTab == LibraryTab.BOOKS) 0 else 1,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = uiState.selectedTab == LibraryTab.BOOKS,
                    onClick = { viewModel.onTabChange(LibraryTab.BOOKS) },
                    text = { Text("Sách (${uiState.books.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = uiState.selectedTab == LibraryTab.COLLECTIONS,
                    onClick = { viewModel.onTabChange(LibraryTab.COLLECTIONS) },
                    text = { Text("Bộ sưu tập (${uiState.collections.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (uiState.selectedTab == LibraryTab.BOOKS) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Tìm trong thư viện...") },
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

                FilterAndSortRow(
                    selectedFilter = uiState.selectedFilter,
                    selectedSort = uiState.selectedSort,
                    onFilterSelected = viewModel::onFilterChange,
                    onSortSelected = viewModel::onSortChange
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isImporting) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val progress = uiState.importProgress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (progress != null) "Đang tải sách..." else "Đang kiểm tra và nhập sách...",
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
                    EmptyLibraryView(
                        searchQuery = uiState.searchQuery,
                        onNavigateToDiscover = onNavigateToDiscover,
                        onImportClick = { showImportSourceSheet = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = uiState.books, key = { it.book.id }) { libraryBook ->
                            LibraryBookItem(
                                libraryBook = libraryBook,
                                onBookClick = {
                                    if (libraryBook.book.categoryId == "imported") {
                                        onReadBookClick(libraryBook.book.id)
                                    } else {
                                        onBookClick(libraryBook.book.id)
                                    }
                                },
                                onReadClick = { onReadBookClick(libraryBook.book.id) },
                                onToggleFavorite = { viewModel.onToggleFavorite(libraryBook.book.id) },
                                onRemoveDownload = { viewModel.onRemoveDownload(libraryBook.book.id) },
                                onDeleteImportedBook = { bookToDelete = libraryBook },
                                onUpdateBook = { viewModel.onUpdateBook(libraryBook.book.id) },
                                onAddToCollection = { bookForCollectionAssignment = libraryBook }
                            )
                        }
                    }
                }
            } else {
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
        }
    }

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
                            text = "${collectionBooks.size} cuốn sách",
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
                        IconButton(onClick = {
                            collectionToDelete = col
                        }) {
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
                            text = "Chưa có sách nào trong bộ sưu tập này.\nNhấn nút tùy chọn trên sách ở tab Sách để thêm vào đây.",
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
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp, 64.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (book.book.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = book.book.coverUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    } else {
                                        Icon(AppIcons.Book, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.book.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = book.book.author,
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

    bookForCollectionAssignment?.let { targetBook ->
        val bookCollectionsFlow = remember(targetBook.book.id) { viewModel.getCollectionsForBook(targetBook.book.id) }
        val currentAssigned by bookCollectionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        var selectedIds by remember { mutableStateOf(setOf<String>()) }

        LaunchedEffect(currentAssigned) {
            selectedIds = currentAssigned.map { it.id }.toSet()
        }

        AlertDialog(
            onDismissRequest = { bookForCollectionAssignment = null },
            title = { Text("Thêm vào bộ sưu tập") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Chọn các bộ sưu tập cho sách: " + targetBook.book.title,
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

    collectionToDelete?.let { col ->
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Xóa bộ sưu tập: " + col.name + "?") },
            text = {
                Text("Bộ sưu tập sẽ bị xóa nhưng các cuốn sách bên trong vẫn được giữ nguyên vẹn trong thư viện của bạn.")
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
                    text = "Thêm sách mới",
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
                                arrayOf("application/epub+zip", "application/pdf", "application/octet-stream", "*/*")
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
                                text = "Tải sách trực tiếp từ liên kết mạng an toàn",
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

    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("Nhập sách từ liên kết") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nhập liên kết tải sách trực tiếp (.epub hoặc .pdf). Chỉ hỗ trợ giao thức bảo mật HTTPS.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextField(
                        value = inputUrl,
                        onValueChange = {
                            inputUrl = it
                            inputUrlError = null
                        },
                        placeholder = { Text("https://example.com/book.epub") },
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

    duplicateBookDialog?.let { (existingBookId, existingTitle) ->
        AlertDialog(
            onDismissRequest = { duplicateBookDialog = null },
            title = { Text("Sách đã tồn tại") },
            text = {
                Text("Cuốn sách: " + existingTitle + " đã có trong thư viện của bạn.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        duplicateBookDialog = null
                        onReadBookClick(existingBookId)
                    }
                ) {
                    Text("Mở sách")
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateBookDialog = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Xóa sách khỏi thư viện?") },
            text = {
                Text("Sách: " + book.book.title + " và toàn bộ dấu trang, ghi chú, tiến độ đọc sẽ bị xóa vĩnh viễn khỏi thiết bị.")
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
                    text = "Tạo bộ sưu tập để phân loại và sắp xếp sách của bạn gọn gàng hơn.",
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
                                text = "${col.bookCount} cuốn sách",
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
private fun FilterAndSortRow(
    selectedFilter: LibraryFilter,
    selectedSort: LibrarySort,
    onFilterSelected: (LibraryFilter) -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(LibraryFilter.entries.toTypedArray()) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.displayName) }
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

@Composable
private fun LibraryBookItem(
    libraryBook: LibraryBook,
    onBookClick: () -> Unit,
    onReadClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveDownload: () -> Unit,
    onDeleteImportedBook: () -> Unit,
    onUpdateBook: () -> Unit,
    onAddToCollection: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isImported = libraryBook.book.categoryId == "imported"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBookClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (libraryBook.book.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = libraryBook.book.coverUrl,
                        contentDescription = libraryBook.book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = libraryBook.book.title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = libraryBook.book.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )

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

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = libraryBook.book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (libraryBook.book.format) {
                            PublicationFormat.PDF -> MaterialTheme.colorScheme.errorContainer
                            PublicationFormat.EPUB -> MaterialTheme.colorScheme.primaryContainer
                            PublicationFormat.CBZ -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = libraryBook.book.format.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (libraryBook.book.format) {
                                PublicationFormat.PDF -> MaterialTheme.colorScheme.onErrorContainer
                                PublicationFormat.EPUB -> MaterialTheme.colorScheme.onPrimaryContainer
                                PublicationFormat.CBZ -> MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isImported) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Sách cá nhân",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (libraryBook.isUpdateAvailable) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Có bản mới",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReadClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        val isReading = progress != null && progress.progression > 0f
                        Text(
                            text = if (isReading) "Tiếp tục đọc" else "Đọc sách",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bộ sưu tập") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onAddToCollection()
                                }
                            )
                            if (!isImported) {
                                DropdownMenuItem(
                                    text = { Text("Xem chi tiết") },
                                    onClick = {
                                        showMenu = false
                                        onBookClick()
                                    }
                                )
                            }
                            if (libraryBook.isUpdateAvailable) {
                                DropdownMenuItem(
                                    text = { Text("Cập nhật sách") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onUpdateBook()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isImported) "Xóa khỏi thư viện" else "Xóa bản tải",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    if (isImported) {
                                        onDeleteImportedBook()
                                    } else {
                                        onRemoveDownload()
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
private fun EmptyLibraryView(
    searchQuery: String,
    onNavigateToDiscover: () -> Unit,
    onImportClick: () -> Unit
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
                    text = "Không tìm thấy sách",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Không có kết quả nào phù hợp với: " + searchQuery,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Thư viện của bạn đang trống",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Khám phá các cuốn sách hay hoặc nhập tệp EPUB/PDF từ thiết bị của bạn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onNavigateToDiscover) {
                        Text("Khám phá sách")
                    }
                    OutlinedButton(onClick = onImportClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhập sách")
                    }
                }
            }
        }
    }
}
