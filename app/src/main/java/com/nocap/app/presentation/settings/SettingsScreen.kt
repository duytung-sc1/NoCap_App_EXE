package com.nocap.app.presentation.settings

import androidx.compose.foundation.layout.widthIn

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import com.nocap.app.core.localization.Text
import com.nocap.app.core.localization.localize
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nocap.app.core.datastore.ReaderFontFamily
import com.nocap.app.core.datastore.ReaderTextAlignment
import com.nocap.app.core.datastore.ReaderTheme
import com.nocap.app.core.localization.AppLanguage
import com.nocap.app.core.localization.AppLanguageManager
import com.nocap.app.domain.model.AuthState
import com.nocap.app.presentation.auth.EditProfileDialog
import com.nocap.app.presentation.auth.ForgotPasswordDialog
import com.nocap.app.presentation.auth.LoginDialog
import com.nocap.app.presentation.auth.RegisterDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val cloudBusy by viewModel.cloudBusy.collectAsStateWithLifecycle()
    val cloudMessage by viewModel.cloudMessage.collectAsStateWithLifecycle()
    var cloudAction by remember { mutableStateOf<String?>(null) }
    val selectedLanguage = remember(context) { AppLanguageManager.selected(context) }

    if (cloudAction != null) {
        AlertDialog(onDismissRequest = { cloudAction = null },
            title = { Text(if (cloudAction == "restore") "Khôi phục thư viện?" else if (cloudAction == "delete") "Xóa bản sao lưu?" else "Sao lưu thư viện?") },
            text = { Text(if (cloudAction == "restore") "Thư viện hiện tại sẽ được thay bằng bản sao lưu trên tài khoản này. Hãy sao lưu dữ liệu cần giữ trước khi tiếp tục."
                else if (cloudAction == "delete") "Bản sao lưu trên đám mây sẽ bị xóa. Dữ liệu trong máy vẫn được giữ."
                else "Sách, ghi chú, tiến độ và bộ sưu tập trên máy sẽ được tải lên tài khoản đang đăng nhập. Bản sao lưu gần nhất sẽ được thay thế.") },
            confirmButton = { TextButton(onClick = {
                when (cloudAction) { "restore" -> viewModel.restoreLibrary(); "delete" -> viewModel.deleteCloudBackup(); else -> viewModel.backupLibrary() }
                cloudAction = null
            }) { Text("Tiếp tục") } },
            dismissButton = { TextButton(onClick = { cloudAction = null }) { Text("Hủy") } })
    }

    val showLogin by viewModel.showLoginDialog.collectAsStateWithLifecycle()
    val showRegister by viewModel.showRegisterDialog.collectAsStateWithLifecycle()
    val showForgotPassword by viewModel.showForgotPasswordDialog.collectAsStateWithLifecycle()
    val showEditProfile by viewModel.showEditProfileDialog.collectAsStateWithLifecycle()
    val showDeleteAccount by viewModel.showDeleteAccountDialog.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }

    val packageInfo: PackageInfo? = remember(context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài đặt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Ngôn ngữ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ngôn ngữ ứng dụng",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Thay đổi sẽ áp dụng ngay cho toàn bộ giao diện.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = selectedLanguage == language,
                            onClick = {
                                if (selectedLanguage != language && AppLanguageManager.select(context, language)) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        context.findActivity()?.recreate()
                                    }
                                }
                            },
                            label = { Text(language.nativeName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            PlanCard()
            // SECTION 0: TÀI KHOẢN (ACCOUNT / AUTH - M8B)
            if (authState is AuthState.Authenticated) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Đồng bộ nhiều thiết bị", fontWeight = FontWeight.Bold)
                        val syncStatus by com.nocap.app.data.sync.SyncScheduler.status.collectAsStateWithLifecycle()
                        val syncContext = androidx.compose.ui.platform.LocalContext.current
                        Text(syncStatus)
                        Button(onClick = { com.nocap.app.data.sync.SyncScheduler.now(syncContext) }) { Text("Đồng bộ ngay") }
                        Text("Bản sao lưu thư viện", fontWeight = FontWeight.Bold)
                        Text("Sao lưu và khôi phục sách, ghi chú, tiến độ, thẻ và bộ sưu tập.")
                        if (cloudBusy) CircularProgressIndicator(Modifier.padding(8.dp))
                        cloudMessage?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !cloudBusy, onClick = { cloudAction = "backup" }) { Text("Sao lưu") }
                            OutlinedButton(enabled = !cloudBusy, onClick = { cloudAction = "restore" }) { Text("Khôi phục") }
                        }
                        TextButton(enabled = !cloudBusy, onClick = { cloudAction = "delete" }) { Text("Xóa bản sao lưu") }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = "Tài khoản",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = authState) {
                is AuthState.Loading -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                        }
                    }
                }

                is AuthState.Guest -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Bạn đang dùng chế độ khách",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Đăng nhập để sao lưu và khôi phục thư viện của bạn",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.openLogin() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Đăng nhập")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.openRegister() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Tạo tài khoản")
                                }
                            }
                        }
                    }
                }

                is AuthState.RequiresEmailVerification -> {
                    val user = state.user
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Yêu cầu xác thực Email",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Kiểm tra hộp thư ${user.email.orEmpty()} hoặc nhấn Gửi lại link.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            if (!authUiState.errorMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = authUiState.errorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!authUiState.successMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = authUiState.successMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.reloadVerification() },
                                    enabled = !authUiState.isLoading,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Đã xác thực", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.sendEmailVerification() },
                                    enabled = !authUiState.isLoading,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Gửi lại link", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            TextButton(
                                onClick = { viewModel.signOut() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Đăng xuất", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                is AuthState.Authenticated -> {
                    val user = state.user
                    val effectiveDisplayName = userProfile?.displayName?.takeIf { it != "null" && it.isNotBlank() } ?: user.displayName?.takeIf { it != "null" && it.isNotBlank() } ?: "Người dùng NoCap"
                    val effectivePhotoUrl = userProfile?.photoUrl ?: user.photoUrl

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Server unavailable banner
                            if (authUiState.isServerUnavailable) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Máy chủ ngoại tuyến. Tính năng ngoại tuyến vẫn hoạt động bình thường.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        TextButton(onClick = { viewModel.retryBackendSync() }) {
                                            Text("Thử lại", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!effectivePhotoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = effectivePhotoUrl,
                                        contentDescription = localize("Ảnh đại diện"),
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = effectiveDisplayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = effectiveDisplayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = user.email.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (user.providerId == "google.com") "Tài khoản Google" else "Email đã xác thực",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Đăng xuất chuyển sang thư viện Khách riêng biệt. Dữ liệu tài khoản vẫn được giữ để dùng tiếp khi đăng nhập lại.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.openEditProfile() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Đổi tên")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.signOut() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Đăng xuất")
                                }

                                TextButton(
                                    onClick = { viewModel.openDeleteAccount() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Xóa tài khoản")
                                }
                            }
                        }
                    }
                }

                is AuthState.Error -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Lỗi xác thực: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.continueAsGuest() }) {
                                Text("Tiếp tục với tư cách khách")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 1: TÙY CHỈNH ĐỌC SÁCH
            Text(
                text = "Tùy chỉnh đọc sách",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Theme
                    Text(
                        text = "Giao diện đọc sách",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsThemeCard(
                            name = "Sáng",
                            bgColor = Color(0xFFFFFFFF),
                            textColor = Color(0xFF000000),
                            isSelected = preferences.theme == ReaderTheme.LIGHT,
                            onClick = { viewModel.updateTheme(ReaderTheme.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsThemeCard(
                            name = "Vàng giấy",
                            bgColor = Color(0xFFF4ECD8),
                            textColor = Color(0xFF5B4636),
                            isSelected = preferences.theme == ReaderTheme.SEPIA,
                            onClick = { viewModel.updateTheme(ReaderTheme.SEPIA) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsThemeCard(
                            name = "Tối",
                            bgColor = Color(0xFF1E1E1E),
                            textColor = Color(0xFFE0E0E0),
                            isSelected = preferences.theme == ReaderTheme.DARK,
                            onClick = { viewModel.updateTheme(ReaderTheme.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cỡ chữ mặc định",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { viewModel.updateFontSize((preferences.fontSizeMultiplier - 0.1f).coerceIn(0.8f, 2.0f)) },
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("A-", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${(preferences.fontSizeMultiplier * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 50.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.updateFontSize((preferences.fontSizeMultiplier + 0.1f).coerceIn(0.8f, 2.0f)) },
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("A+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Family
                    Text(
                        text = "Kiểu chữ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.SYSTEM_DEFAULT,
                            onClick = { viewModel.updateFontFamily(ReaderFontFamily.SYSTEM_DEFAULT) },
                            label = { Text("Mặc định") }
                        )
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.SERIF,
                            onClick = { viewModel.updateFontFamily(ReaderFontFamily.SERIF) },
                            label = { Text("Có chân", fontFamily = FontFamily.Serif) }
                        )
                        FilterChip(
                            selected = preferences.fontFamily == ReaderFontFamily.SANS_SERIF,
                            onClick = { viewModel.updateFontFamily(ReaderFontFamily.SANS_SERIF) },
                            label = { Text("Không chân", fontFamily = FontFamily.SansSerif) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Alignment & Scroll Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Căn lề văn bản",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = preferences.textAlignment == ReaderTextAlignment.START,
                                    onClick = { viewModel.updateTextAlignment(ReaderTextAlignment.START) },
                                    label = { Text("Trái") }
                                )
                                FilterChip(
                                    selected = preferences.textAlignment == ReaderTextAlignment.JUSTIFY,
                                    onClick = { viewModel.updateTextAlignment(ReaderTextAlignment.JUSTIFY) },
                                    label = { Text("Đều") }
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Cuộn dọc",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Switch(
                                checked = preferences.isScrollMode,
                                onCheckedChange = viewModel::updateScrollMode
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: KHÔI PHỤC CÀI ĐẶT
            Text(
                text = "Quản lý cài đặt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Khôi phục cài đặt đọc mặc định",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Đặt lại cỡ chữ 100%, giao diện sáng và kiểu chữ mặc định",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = localize("Khôi phục"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: THÔNG TIN ỨNG DỤNG
            Text(
                text = "Thông tin ứng dụng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tên ứng dụng",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "NoCap",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Phiên bản",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "v$versionName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bộ đọc sách",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Readium Kotlin 3.3.0",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showLogin) {
        LoginDialog(
            isLoading = authUiState.isLoading,
            errorMessage = authUiState.errorMessage,
            onDismiss = { viewModel.dismissLogin() },
            onLogin = { email, pass -> viewModel.loginWithEmail(email, pass) },
            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
            onNavigateToRegister = { viewModel.openRegister() },
            onNavigateToForgotPassword = { viewModel.openForgotPassword() },
            onContinueAsGuest = { viewModel.continueAsGuest() }
        )
    }

    if (showRegister) {
        RegisterDialog(
            isLoading = authUiState.isLoading,
            errorMessage = authUiState.errorMessage,
            onDismiss = { viewModel.dismissRegister() },
            onRegister = { email, pass, confirm -> viewModel.registerWithEmail(email, pass, confirm) },
            onNavigateToLogin = { viewModel.openLogin() }
        )
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            isLoading = authUiState.isLoading,
            errorMessage = authUiState.errorMessage,
            successMessage = authUiState.successMessage,
            onDismiss = { viewModel.dismissForgotPassword() },
            onSendReset = { email -> viewModel.sendPasswordReset(email) }
        )
    }

    if (showEditProfile) {
        val currentName = (userProfile?.displayName ?: (authState as? AuthState.Authenticated)?.user?.displayName)?.takeIf { it != "null" && it.isNotBlank() }
        EditProfileDialog(
            initialDisplayName = currentName,
            isLoading = authUiState.isLoading,
            onDismiss = { viewModel.dismissEditProfile() },
            onConfirm = { newName -> viewModel.updateDisplayName(newName) }
        )
    }

    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { if (!authUiState.isLoading) viewModel.dismissDeleteAccount() },
            title = { Text("Xác nhận xóa tài khoản?") },
            text = {
                Column {
                    Text("Hành động này sẽ xóa vĩnh viễn tài khoản của bạn và toàn bộ dữ liệu hồ sơ liên kết trên máy chủ. Sách đã tải về trên máy và tiến trình đọc cục bộ sẽ được giữ lại.")
                    if (!authUiState.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authUiState.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAccount() },
                    enabled = !authUiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (authUiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Xác nhận xóa")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAccount() }, enabled = !authUiState.isLoading) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Khôi phục cài đặt?") },
            text = { Text("Bạn có chắc chắn muốn đặt lại toàn bộ tùy chỉnh giao diện và phông chữ đọc sách về mặc định?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Khôi phục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun SettingsThemeCard(
    name: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
