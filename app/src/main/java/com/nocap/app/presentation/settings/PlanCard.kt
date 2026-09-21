package com.nocap.app.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nocap.app.core.localization.Text
import com.nocap.app.data.billing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

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
    LaunchedEffect(play) { play.connect() }

    val pro = EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC, state.entitlement, repo.activeUser())
    val purchaseAvailable = com.nocap.app.BuildConfig.PLAY_PRO_PRODUCT_ID.isNotBlank() &&
        state.entitlement?.let { it.userId == repo.activeUser() && it.configured } == true

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
                text = "Free luôn giữ quyền đọc, nhập tài liệu, dấu trang, ghi chú và thư viện offline. Pro thêm đồng bộ và lưu trữ đám mây.",
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

                if (repo.activeUser() != null) OutlinedButton(
                    enabled = !state.loading,
                    onClick = { play.restore() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Khôi phục giao dịch") }
            }

            TextButton(onClick = { scope.launch { repo.refresh() } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cập nhật trạng thái gói", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    order?.let { current ->
        BankTransferDialog(
            order = current,
            onDismiss = { order = null }
        )
    }
}

@Composable
private fun BankTransferDialog(
    order: BankTransferOrder,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quét mã QR để thanh toán") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = order.qrUrl,
                    contentDescription = "Mã QR thanh toán Pro",
                    modifier = Modifier.size(280.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}
