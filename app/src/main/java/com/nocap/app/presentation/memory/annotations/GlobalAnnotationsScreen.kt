
package com.nocap.app.presentation.memory.annotations

import com.nocap.app.core.localization.Text
import com.nocap.app.core.localization.localize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocap.app.core.util.VietnameseUtils
import com.nocap.app.presentation.memory.BookmarkCard
import com.nocap.app.presentation.memory.HighlightCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAnnotationsScreen(
    onBackClick: () -> Unit,
    onNavigateToReader: (bookId: String, locatorJson: String?) -> Unit,
    viewModel: GlobalAnnotationsViewModel = viewModel(
        factory = GlobalAnnotationsViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Toàn bộ Ghi chú & Đoạn trích") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localize("Quay lại"))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Lọc trong ghi chú...", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = localize("Xóa"))
                        }
                    }
                }
            )

            // Tabs / Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnnotationTab.entries.forEach { tab ->
                    FilterChip(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        label = { Text(tab.label) }
                    )
                }
            }

            HorizontalDivider()

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val query = uiState.searchQuery.trim()

                // Filter highlights based on tab and query
                val filteredHighlights = uiState.highlights.filter { item ->
                    val matchesTab = when (uiState.selectedTab) {
                        AnnotationTab.ALL -> true
                        AnnotationTab.HIGHLIGHTS -> item.highlight.note.isNullOrBlank()
                        AnnotationTab.NOTES -> !item.highlight.note.isNullOrBlank()
                        AnnotationTab.BOOKMARKS -> false
                        AnnotationTab.IN_REVIEW -> item.isUnderReview
                    }
                    val matchesQuery = query.isBlank() ||
                            VietnameseUtils.containsNormalized(item.highlight.text, query) ||
                            VietnameseUtils.containsNormalized(item.highlight.note, query) ||
                            VietnameseUtils.containsNormalized(item.book.title, query)
                    matchesTab && matchesQuery
                }

                // Filter bookmarks based on tab and query
                val filteredBookmarks = if (uiState.selectedTab == AnnotationTab.ALL || uiState.selectedTab == AnnotationTab.BOOKMARKS) {
                    uiState.bookmarks.filter { item ->
                        query.isBlank() ||
                                VietnameseUtils.containsNormalized(item.bookmark.chapterTitle, query) ||
                                VietnameseUtils.containsNormalized(item.bookmark.snippet, query) ||
                                VietnameseUtils.containsNormalized(item.book.title, query)
                    }
                } else emptyList()

                if (filteredHighlights.isEmpty() && filteredBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có nội dung nào phù hợp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredHighlights, key = { "hl_${it.highlight.id}" }) { item ->
                            HighlightCard(
                                item = item,
                                onClick = {
                                    onNavigateToReader(item.book.id, item.highlight.locatorJson)
                                },
                                onToggleReview = {
                                    viewModel.toggleReview(item.highlight.id, item.book.id, item.isUnderReview)
                                }
                            )
                        }

                        items(filteredBookmarks, key = { "bm_${it.bookmark.id}" }) { item ->
                            BookmarkCard(
                                item = item,
                                onClick = {
                                    onNavigateToReader(item.book.id, item.bookmark.locatorJson)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
