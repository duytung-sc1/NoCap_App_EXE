package com.nocap.app.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nocap.app.core.localization.Text
import com.nocap.app.data.billing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

private fun paymentError(code: String) = when (code) {
    "SIGN_IN_REQUIRED", "UNAUTHORIZED" -> "Đăng nhập để nâng cấp Pro."
    "SEPAY_PAYMENT_NOT_CONFIGURED" -> "Thanh toán chuyển khoản chưa sẵn sàng. Vui lòng thử lại sau."
    else -> "Chưa tạo được yêu cầu thanh toán. Vui lòng kiểm tra mạng và thử lại."
}

@Composable
fun PlanCard() {
    val context = LocalContext.current
    val repo = remember { EntitlementRepository.get(context) }
    val transferRepo = remember { BankTransferRepository.get(context) }
    val state by repo.state.collectAsStateWithLifecycle()
    val play = remember { PlayBilling.get(context) }
    val billingMessage by play.message.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var paymentLoading by remember { mutableStateOf(false) }
    var paymentMessage by remember { mutableStateOf<String?>(null) }
    var order by remember { mutableStateOf<BankTransferOrder?>(null) }
    val pro = state.entitlement?.let {
        it.userId == repo.activeUser() && it.plan == "PRO" &&
            it.status in setOf("ACTIVE", "IN_GRACE_PERIOD", "CANCELED") &&
            (it.expiresAt == 0L || it.expiresAt > System.currentTimeMillis())
    } == true
    val purchaseAvailable = com.nocap.app.BuildConfig.PLAY_PRO_PRODUCT_ID.isNotBlank() &&
        state.entitlement?.let { it.userId == repo.activeUser() && it.configured } == true
    LaunchedEffect(play, purchaseAvailable) { if (purchaseAvailable) play.connect() }

    LaunchedEffect(order?.id) {
        var current = order ?: return@LaunchedEffect
        while (current.status in setOf("PENDING", "PROCESSING") && current.expiresAt > System.currentTimeMillis()) {
            delay(5_000)
            current = runCatching { transferRepo.status(current.id) }.getOrElse { continue }
            order = current
            if (current.status == "PAID") {
                repo.refresh()
                paymentMessage = "Thanh toán thành công. Gói Pro đã được kích hoạt."
                order = null
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (pro) "Gói hiện tại: Pro" else "Gói hiện tại: Free",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (pro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = if (pro) "PRO" else "FREE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (pro) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (state.loading || paymentLoading) Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }

            Text(
                text = if (pro) "Bạn đang sử dụng gói Pro: Đã mở khóa Bộ nhớ đọc nâng cao, Xuất PDF/Anki và Cloud nâng cao."
                else "Gói Pro mở khóa: Bộ nhớ đọc nâng cao (Spaced Repetition & phiên ôn nhanh), Xuất tài liệu PDF & bộ thẻ Anki, và Cloud nâng cao (Lịch sử ghi chú, khôi phục theo thời điểm, 10 bản sao lưu).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.entitlement?.takeIf { it.userId == repo.activeUser() }?.let { entitlement ->
                if (entitlement.expiresAt > 0) Text(
                    text = "${if (entitlement.autoRenew) "Kỳ hiện tại đến" else "Hạn sử dụng"}: ${DateFormat.getDateTimeInstance().format(Date(entitlement.expiresAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                if (entitlement.status == "PENDING") Text("Thanh toán đang chờ xác nhận", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                if (entitlement.status in setOf("EXPIRED", "ON_HOLD", "PAUSED", "INVALID")) Text(
                    "Pro không hoạt động. Tài liệu và thay đổi chưa đồng bộ vẫn được giữ trên máy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            listOfNotNull(state.message, billingMessage, paymentMessage).filter { it.isNotBlank() }.distinct().forEach { message ->
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
            }

            if (!pro && repo.activeUser() == null) Text(
                "Đăng nhập để nâng cấp Pro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!pro && repo.activeUser() != null) Button(
                    enabled = !state.loading && !paymentLoading,
                    onClick = {
                        scope.launch {
                            paymentLoading = true; paymentMessage = null
                            try { order = transferRepo.create() }
                            catch (error: BankPaymentException) { paymentMessage = paymentError(error.code) }
                            catch (_: Exception) { paymentMessage = paymentError("PAYMENT_UNAVAILABLE") }
                            finally { paymentLoading = false }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Nâng cấp Pro qua ngân hàng") }

                if (!pro && purchaseAvailable) Button(
                    enabled = !state.loading,
                    onClick = { context.activity()?.let(play::purchase) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Nâng cấp Pro") }

                if (purchaseAvailable) OutlinedButton(
                    enabled = !state.loading,
                    onClick = { play.restore() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Khôi phục quyền mua Google Play") }
            }

            TextButton(onClick = { scope.launch { repo.refresh() } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cập nhật trạng thái gói", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    order?.let { current ->
        BankTransferDialog(
            order = current,
            onDismiss = { order = null },
            onPaymentSuccess = {
                scope.launch { repo.refresh() }
                paymentMessage = "Thanh toán thành công. Gói Pro đã được kích hoạt."
                order = null
            }
        )
    }
}

@Composable
private fun BankTransferDialog(
    order: BankTransferOrder,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val transferRepo = remember { BankTransferRepository.get(context) }
    val scope = rememberCoroutineScope()

    var checkingStatus by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var currentQrUrl by remember(order.id) { mutableStateOf(order.qrUrl) }

    val formattedAmount = remember(order.amount) {
        String.format(Locale.US, "%,d đ", order.amount)
    }

    val formattedExpiry = remember(order.expiresAt) {
        val remainingMs = order.expiresAt - System.currentTimeMillis()
        if (remainingMs > 0) {
            val minutes = remainingMs / 60_000
            val seconds = (remainingMs % 60_000) / 1_000
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        } else {
            "Đã hết hạn"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quét mã QR để thanh toán",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quét QR hoặc dùng thông tin bên dưới. Chuyển đúng số tiền và nội dung để hệ thống tự xác nhận.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // QR Code Card with white surface for guaranteed contrast
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(240.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val imageRequest = remember(currentQrUrl) {
                            ImageRequest.Builder(context)
                                .data(currentQrUrl)
                                .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                                .crossfade(true)
                                .build()
                        }
                        SubcomposeAsyncImage(
                            model = imageRequest,
                            contentDescription = "Mã QR thanh toán Pro",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                                }
                            },
                            error = {
                                if (currentQrUrl != order.fallbackQrUrl) {
                                    LaunchedEffect(Unit) {
                                        currentQrUrl = order.fallbackQrUrl
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Không tải được mã QR",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                currentQrUrl = if (currentQrUrl == order.qrUrl) order.fallbackQrUrl else order.qrUrl
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Thử lại", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // Transfer Details Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bank name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ngân hàng", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${order.bank.name} (${order.bank.code})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        // Account holder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chủ tài khoản", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(order.bank.accountHolder, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        // Account number + Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Số tài khoản", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.bank.accountNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(order.bank.accountNumber))
                                    actionMessage = "Đã sao chép số tài khoản."
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sao chép", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Amount + Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Số tiền", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formattedAmount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            FilledTonalButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(order.amount.toString()))
                                    actionMessage = "Đã sao chép số tiền."
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sao chép", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Transfer Memo / Payment Content - CRITICAL
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Nội dung chuyển khoản",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        order.paymentContent,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(order.paymentContent))
                                        actionMessage = "Đã sao chép nội dung chuyển khoản."
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sao chép", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Expiry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thời gian còn lại", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formattedExpiry, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Action feedback message
                actionMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action button: Check Payment Status manually
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            checkingStatus = true
                            actionMessage = null
                            try {
                                val updated = transferRepo.status(order.id)
                                if (updated.status == "PAID") {
                                    onPaymentSuccess()
                                } else {
                                    actionMessage = "Chưa nhận được thanh toán. Vui lòng thử lại sau ít phút."
                                }
                            } catch (_: Exception) {
                                actionMessage = "Chưa kiểm tra được giao dịch. Vui lòng thử lại."
                            } finally {
                                checkingStatus = false
                            }
                        }
                    },
                    enabled = !checkingStatus,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (checkingStatus) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Kiểm tra thanh toán")
                }

                // Open banking app button
                Button(
                    onClick = {
                        scope.launch {
                            clipboardManager.setText(AnnotatedString(order.paymentContent))
                            try {
                                val apps = transferRepo.bankApps()
                                val topApp = apps.firstOrNull { it.autoFill } ?: apps.firstOrNull()
                                if (topApp != null) {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(topApp.paymentUrl(order))
                                    ).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } else {
                                    actionMessage = "Không mở được ứng dụng ngân hàng. Nội dung chuyển khoản đã được sao chép."
                                }
                            } catch (_: Exception) {
                                actionMessage = "Không mở được ứng dụng ngân hàng. Nội dung chuyển khoản đã được sao chép."
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mở ứng dụng ngân hàng")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
