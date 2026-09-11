
package com.nocap.app.presentation.memory

import com.nocap.app.core.localization.Text
import com.nocap.app.core.localization.localize

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocap.app.core.designsystem.AppIcons
import com.nocap.app.domain.model.BookmarkWithBook
import com.nocap.app.domain.model.HighlightWithBook
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingMemoryScreen(
    onNavigateToReader: (bookId: String, locatorJson: String?) -> Unit,
    onNavigateToReviewQueue: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToGlobalAnnotations: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: ReadingMemoryViewModel = viewModel(
        factory = ReadingMemoryViewModel.provideFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isExporting by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val success = viewModel.exportAllKnowledge(outputStream)
                    isExporting = false
                    if (success) {
                        Toast.makeText(context, com.nocap.app.core.localization.AppLanguageManager.translate(context, "Đã xuất dữ liệu ghi chú thành công!"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, com.nocap.app.core.localization.AppLanguageManager.translate(context, "Lỗi khi xuất tệp Markdown"), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isExporting = false
                    Toast.makeText(context, com.nocap.app.core.localization.AppLanguageManager.translate(context, "Không thể mở tệp để ghi"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bộ nhớ đọc",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = localize("Tìm kiếm tri thức"))
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.Info, contentDescription = localize("Thống kê đọc"))
                    }
                    IconButton(
                        onClick = {
                            val defaultName = "NoCap_Knowledge_Export_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.md"
                            exportLauncher.launch(defaultName)
                        },
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = localize("Xuất Markdown"))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Start with the user's saved knowledge; statistics remain in the toolbar.
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Trở lại những điều bạn muốn nhớ", style = MaterialTheme.typography.titleMedium)
                        Text("Tìm đoạn tô sáng, ghi chú hoặc ôn lại những ý đã lưu khi đọc.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onNavigateToSearch, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tìm trong tài liệu và ghi chú")
                        }
                    }
                }

                // 2. Due Today Review Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.dueTodayCount > 0)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (uiState.dueTodayCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        AppIcons.Book,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ôn tập hàng ngày",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (uiState.dueTodayCount > 0)
                                            "${uiState.dueTodayCount} nội dung cần ôn tập hôm nay"
                                        else
                                            "Bạn đã hoàn thành tất cả mục ôn tập hôm nay!",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (uiState.dueTodayCount > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onNavigateToReviewQueue,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text("Bắt đầu ôn tập (${uiState.dueTodayCount})")
                                }
                            }
                        }
                    }
                }

                // 3. Knowledge Search Entry Button
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSearch() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Tìm kiếm tri thức trong toàn bộ thư viện...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Recent Notes & Highlights Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ghi chú & Đoạn trích gần đây",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToGlobalAnnotations) {
                            Text("Xem tất cả")
                        }
                    }
                }

                // Recent Notes & Highlights List
                if (uiState.recentNotes.isEmpty() && uiState.recentHighlights.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có ghi chú hoặc đoạn trích nào.\nHãy chọn văn bản khi đọc để lưu giữ tri thức!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                } else {
                    val combinedList = (uiState.recentNotes + uiState.recentHighlights).take(8)
                    items(combinedList) { item ->
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
                }

                // 5. Recent Bookmarks Section Header
                if (uiState.recentBookmarks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Đánh dấu gần đây",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.recentBookmarks.take(5)) { item ->
                        BookmarkCard(
                            item = item,
                            onClick = {
                                onNavigateToReader(item.book.id, item.bookmark.locatorJson)
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun StatColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HighlightCard(
    item: HighlightWithBook,
    onClick: () -> Unit,
    onToggleReview: () -> Unit
) {
    val highlightColor = when (item.highlight.color.uppercase()) {
        "YELLOW" -> Color(0xFFFFF176)
        "GREEN" -> Color(0xFFA5D6A7)
        "BLUE" -> Color(0xFF90CAF9)
        "PINK" -> Color(0xFFF48FB1)
        else -> Color(0xFFFFF176)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(highlightColor)
                    )
                    Text(
                        text = item.book.userTitleOverride ?: item.book.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.book.format.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleReview,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.isUnderReview) Icons.Default.CheckCircle else Icons.Default.Add,
                        contentDescription = localize("Ôn tập"),
                        tint = if (item.isUnderReview) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "“${item.highlight.text.trim()}”",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.highlight.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.highlight.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkCard(
    item: BookmarkWithBook,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(AppIcons.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.bookmark.chapterTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.bookmark.snippet.isNullOrBlank()) {
                    Text(
                        text = item.bookmark.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = item.book.userTitleOverride ?: item.book.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatDurationMinutes(ms: Long): String {
    val mins = ms / (60 * 1000)
    return if (mins < 60) "${mins}p" else "${mins / 60}h ${mins % 60}p"
}

private fun formatDurationHours(ms: Long): String {
    val mins = ms / (60 * 1000)
    val hours = mins / 60
    return "${hours}h ${mins % 60}p"
}
