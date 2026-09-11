package com.nocap.app.presentation.settings

import com.nocap.app.core.localization.Text

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocap.app.data.billing.*
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private fun Context.activity(): Activity? = when(this){is Activity -> this;is ContextWrapper -> baseContext.activity();else -> null}
@Composable fun PlanCard(){
    val context=LocalContext.current
    val repo=remember { EntitlementRepository.get(context) }
    val state by repo.state.collectAsStateWithLifecycle()
    val play=remember { PlayBilling.get(context) }
    val billingMessage by play.message.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    LaunchedEffect(play){play.connect()}
    val pro=EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC,state.entitlement,repo.activeUser())
    val purchaseAvailable = com.nocap.app.BuildConfig.PLAY_PRO_PRODUCT_ID.isNotBlank() &&
        state.entitlement?.let { it.userId == repo.activeUser() && it.configured } == true
    Card(Modifier.fillMaxWidth().padding(bottom=16.dp)){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Gói: ${if(pro) "Pro" else "Free"}",style=MaterialTheme.typography.titleMedium)
            if(state.loading)CircularProgressIndicator()
            Text("Free luôn giữ quyền đọc, nhập tài liệu, dấu trang, ghi chú và thư viện offline. Pro thêm đồng bộ và lưu trữ đám mây.")
            state.entitlement?.takeIf { it.userId==repo.activeUser() }?.let { e ->
                if(e.expiresAt>0)Text("${if(e.autoRenew) "Kỳ hiện tại đến" else "Hạn sử dụng"}: ${DateFormat.getDateTimeInstance().format(Date(e.expiresAt))}")
                if(e.status=="PENDING")Text("Thanh toán đang chờ xác nhận")
                if(e.status in setOf("EXPIRED","ON_HOLD","PAUSED","INVALID"))Text("Pro không hoạt động. Tài liệu và thay đổi chưa đồng bộ vẫn được giữ trên máy.")
            }
            state.message?.let { Text(it) };billingMessage?.let { Text(it) }
            if (!purchaseAvailable && !pro) Text("Pro chưa mở bán. Bạn vẫn có thể đọc và quản lý tài liệu trên máy với Free.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled=!state.loading && !pro && purchaseAvailable,onClick={context.activity()?.let(play::purchase)}){Text("Nâng cấp Pro")}
            OutlinedButton(enabled = !state.loading && repo.activeUser() != null, onClick={play.restore()}){Text("Khôi phục giao dịch")}
            TextButton(onClick={scope.launch { repo.refresh() }}){Text("Cập nhật trạng thái gói")}
        }
    }
}
