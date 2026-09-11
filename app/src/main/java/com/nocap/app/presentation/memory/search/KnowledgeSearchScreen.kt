
package com.nocap.app.presentation.memory.search

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocap.app.core.designsystem.AppIcons
import com.nocap.app.domain.model.KnowledgeItemType
import com.nocap.app.domain.model.KnowledgeSearchResult
import com.nocap.app.domain.model.PublicationFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeSearchScreen(
    onBackClick: () -> Unit,
    onNavigateToReader: (bookId: String, locatorJson: String?) -> Unit,
    onNavigateToBookDetails: (bookId: String) -> Unit,
    viewModel: KnowledgeSearchViewModel = viewModel(
        factory = KnowledgeSearchViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Tìm tri thức, tài liệu, ghi chú...", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearQuery() }) {
                                    Icon(Icons.Default.Clear, contentDescription = localize("Xóa tìm kiếm"))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                },
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
            // Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KnowledgeItemType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.typeFilter == type,
                        onClick = { viewModel.onTypeFilterChange(type) },
                        label = { Text(type.displayName) }
                    )
                }
            }

            // Format Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.formatFilter == null,
                    onClick = { viewModel.onFormatFilterChange(null) },
                    label = { Text("Mọi định dạng") }
                )
                val commonFormats = listOf(
                    PublicationFormat.EPUB,
                    PublicationFormat.PDF,
                    PublicationFormat.TXT,
                    PublicationFormat.MARKDOWN,
                    PublicationFormat.HTML,
                    PublicationFormat.DOCX,
                    PublicationFormat.CBZ
                )
                commonFormats.forEach { format ->
                    FilterChip(
                        selected = uiState.formatFilter == format,
                        onClick = { viewModel.onFormatFilterChange(format) },
                        label = { Text(format.name) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

            // Results / Loading / Empty State
            if (uiState.isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.hasSearched && uiState.results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Không tìm thấy kết quả nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hệ thống hỗ trợ tìm kiếm không dấu (VD: 'tri tue' tìm được 'Trí Tuệ'). Vui lòng thử từ khóa khác.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else if (!uiState.hasSearched) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Tìm kiếm trong toàn bộ tri thức của bạn",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tìm tài liệu, đoạn trích, ghi chú, đánh dấu trang hoặc nội dung",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.results) { result ->
                        SearchResultCard(
                            result = result,
                            onClick = {
                                if (result.type == KnowledgeItemType.DOCUMENT) {
                                    onNavigateToReader(result.bookId, null)
                                } else {
                                    onNavigateToReader(result.bookId, result.locatorJson)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    result: KnowledgeSearchResult,
    onClick: () -> Unit
) {
    val typeIcon = when (result.type) {
        KnowledgeItemType.DOCUMENT -> AppIcons.Book
        KnowledgeItemType.HIGHLIGHT -> Icons.Default.Edit
        KnowledgeItemType.NOTE -> Icons.Default.Edit
        KnowledgeItemType.BOOKMARK -> AppIcons.Bookmark
        KnowledgeItemType.CONTENT -> Icons.Default.Info
        KnowledgeItemType.ALL -> Icons.Default.Search
    }

    val typeColor = when (result.type) {
        KnowledgeItemType.DOCUMENT -> MaterialTheme.colorScheme.primary
        KnowledgeItemType.HIGHLIGHT -> Color(0xFFF57F17)
        KnowledgeItemType.NOTE -> Color(0xFF2E7D32)
        KnowledgeItemType.BOOKMARK -> Color(0xFF1565C0)
        KnowledgeItemType.CONTENT -> Color(0xFF6A1B9A)
        KnowledgeItemType.ALL -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                    Text(
                        text = result.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = result.format.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = result.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (result.snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.documentTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (result.timestamp > 0) {
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(result.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
