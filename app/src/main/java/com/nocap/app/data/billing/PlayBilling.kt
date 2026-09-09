package com.nocap.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.nocap.app.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

class PlayBilling(context: Context) : PurchasesUpdatedListener {
    private val repository=EntitlementRepository.get(context)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
    val message=MutableStateFlow<String?>(null)
    private val client=BillingClient.newBuilder(context.applicationContext).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
        .enableAutoServiceReconnection().build()
    private var launchUser: String?=null
    companion object {
        @Volatile private var instance: PlayBilling?=null
        fun get(context: Context)=instance ?: synchronized(this){instance ?: PlayBilling(context.applicationContext).also { instance=it }}
    }
    private var connecting=false
    init { scope.launch {
        com.nocap.app.data.auth.CloudAuthRepository.getInstance(context).authState.collectLatest { auth ->
            when(auth){
                is com.nocap.app.domain.model.AuthState.Authenticated,
                is com.nocap.app.domain.model.AuthState.RequiresEmailVerification -> connect()
                com.nocap.app.domain.model.AuthState.Guest -> {launchUser=null;message.value=null}
                else -> Unit
            }
        }
    } }
    fun connect(){
        if(client.isReady){restore();return}
        if(connecting)return
        connecting=true
        client.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult){connecting=false;if(result.responseCode==BillingClient.BillingResponseCode.OK)restore() else message.value="Không kết nối được Google Play"}
            override fun onBillingServiceDisconnected(){connecting=false;message.value="Google Play mất kết nối; hãy thử khôi phục lại."}
        })
    }
    fun restore(){
        if(repository.activeUser()==null){message.value="Đăng nhập để khôi phục giao dịch";return}
        if(!client.isReady){connect();return}
        message.value="Đang khôi phục giao dịch…"
        val id=repository.activeUser()!!
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()){result,purchases ->
            if(repository.activeUser()!=id)return@queryPurchasesAsync
            if(result.responseCode!=BillingClient.BillingResponseCode.OK){message.value="Chưa tải được giao dịch Google Play";return@queryPurchasesAsync}
            scope.launch {
                try {
                    val complete=reconcilePurchases(purchases,{process(it,id)},{
                        check(repository.activeUser()==id)
                        repository.restore()
                    })
                    if(repository.activeUser()==id) {
                        if(!complete)message.value="Đã đối soát gói; một số giao dịch chưa khôi phục được hoặc thuộc tài khoản khác."
                        else if(purchases.none { it.purchaseState==Purchase.PurchaseState.PENDING })message.value="Đã đối soát giao dịch với máy chủ"
                    }
                }catch(e: Exception){if(e is CancellationException)throw e;if(repository.activeUser()==id)message.value="Chưa khôi phục được. Kiểm tra tài khoản NoCap rồi thử lại."}
            }
        }
    }
    fun purchase(activity: Activity){
        val state=repository.state.value.entitlement
        val id=repository.activeUser()
        if(id==null || state?.userId!=id){message.value="Vui lòng đăng nhập và cập nhật gói trước";return}
        if(BuildConfig.PLAY_PRO_PRODUCT_ID.isBlank() || !state.configured){message.value="Gói Pro chưa được cấu hình trên Google Play";return}
        if(!client.isReady){message.value="Đang kết nối Google Play; vui lòng thử lại";connect();return}
        val product=QueryProductDetailsParams.Product.newBuilder().setProductId(BuildConfig.PLAY_PRO_PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()){result,details ->
            val item=details.productDetailsList.firstOrNull()
            val offer=item?.subscriptionOfferDetails?.firstOrNull { it.offerId==null }
            if(result.responseCode!=BillingClient.BillingResponseCode.OK || item==null || offer==null){message.value="Gói Pro chưa có sẵn cho tài khoản Google Play này";return@queryProductDetailsAsync}
            scope.launch {
                if(repository.activeUser()!=id)return@launch
                launchUser=id
                val params=BillingFlowParams.newBuilder().setObfuscatedAccountId(state.accountId)
                    .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(item).setOfferToken(offer.offerToken).build())).build()
                handleResult(client.launchBillingFlow(activity,params),null)
            }
        }
    }
    override fun onPurchasesUpdated(result: BillingResult,purchases: MutableList<Purchase>?){handleResult(result,purchases)}
    private fun handleResult(result: BillingResult,purchases: List<Purchase>?){
        when(PurchasePolicy.outcome(result.responseCode,Purchase.PurchaseState.PURCHASED)){
            PurchaseOutcome.CANCELLED -> {launchUser=null;message.value="Đã hủy thanh toán; gói hiện tại không đổi"}
            PurchaseOutcome.RESTORE -> restore()
            PurchaseOutcome.VERIFY -> if(purchases!=null)scope.launch {
                val id=launchUser ?: repository.activeUser() ?: return@launch
                launchUser=null
                try {for(p in purchases)process(p,id)}catch(e: Exception){if(e is CancellationException)throw e;message.value="Chưa xác minh được. Dùng Khôi phục giao dịch để thử lại."}
            }
            else -> {launchUser=null;message.value="Thanh toán chưa thành công; vui lòng thử lại"}
        }
    }
    private suspend fun process(purchase: Purchase,id: String){
        if(BuildConfig.PLAY_PRO_PRODUCT_ID !in purchase.products)return
        check(repository.activeUser()==id)
        when(PurchasePolicy.outcome(0,purchase.purchaseState)){
            PurchaseOutcome.PENDING -> message.value="Thanh toán đang chờ; Pro sẽ bật sau khi Google xác nhận"
            PurchaseOutcome.VERIFY -> {repository.verify(purchase.purchaseToken,id);message.value="Đã xác minh giao dịch"}
            else -> Unit
        }
        // Server acknowledges only after Google verification. Tokens are never logged or persisted here.
    }
}
