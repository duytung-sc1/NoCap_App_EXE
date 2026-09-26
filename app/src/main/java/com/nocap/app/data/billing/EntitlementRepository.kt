package com.nocap.app.data.billing

import android.content.Context
import com.nocap.app.BuildConfig
import com.nocap.app.data.auth.CloudAuthRepository
import com.nocap.app.data.sync.Profiles
import com.nocap.app.domain.model.AuthState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class PlanState(val entitlement: Entitlement? = null, val loading: Boolean = false, val message: String? = null, val checkedAt: Long = System.currentTimeMillis())
class EntitlementRepository private constructor(private val context: Context) {
    private val prefs=context.getSharedPreferences("entitlements-v1",Context.MODE_PRIVATE)
    private val cache=UserEntitlementCache({prefs.getString(it,null)},{key,value -> prefs.edit().putString(key,value).apply()})
    private val client=OkHttpClient()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val mutex=Mutex()
    private val mutable=MutableStateFlow(PlanState())
    val state: StateFlow<PlanState> = mutable
    init { scope.launch {
        CloudAuthRepository.getInstance(context).authState.collectLatest { auth ->
            if(auth==AuthState.Loading)return@collectLatest
            val id=activeUser()
            mutable.value=PlanState(id?.let { cached(it) })
            if(id!=null){ refresh(); while(isActive){delay(60_000);mutable.value=mutable.value.copy(checkedAt=System.currentTimeMillis());if(state.value.entitlement?.expiresAt?.let { it<=System.currentTimeMillis() }==true)mutable.value=mutable.value.copy(message="Pro đã hết hạn. Dữ liệu trên máy vẫn được giữ.") } }
        }
    } }
    fun activeUser(): String? = Profiles.active.value.takeIf { it.startsWith("ACCOUNT:") }?.removePrefix("ACCOUNT:")
    private fun cached(id: String)=cache.load(id)
    suspend fun refresh(): Result<Entitlement> = try { Result.success(request("/api/v1/entitlement")) } catch(e: CancellationException){throw e} catch(e: Exception){Result.failure(e)}
    suspend fun verify(token: String, expectedUser: String) = request("/api/v1/billing/google/verify",JSONObject().put("purchaseToken",token),expectedUser)
    suspend fun restore() = request("/api/v1/billing/google/restore",JSONObject())
    private suspend fun request(path: String, data: JSONObject?=null, expectedUser: String?=activeUser()): Entitlement = withContext(Dispatchers.IO) { mutex.withLock {
        val id=expectedUser ?: throw ProRequired();check(activeUser()==id)
        mutable.value=mutable.value.copy(loading=true,message=null)
        try {
            val token=CloudAuthRepository.getInstance(context).getIdToken(false) ?: throw ProRequired()
            val me=client.newCall(Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/me").header("Authorization","Bearer $token").build()).execute().use {
                check(it.isSuccessful);JSONObject(it.body!!.string()).getString("id")
            };check(me==id && activeUser()==id)
            val request=Request.Builder().url(BuildConfig.BACKEND_BASE_URL+path).header("Authorization","Bearer $token")
            if(data!=null)request.post(data.toString().toRequestBody("application/json".toMediaType()))
            val entitlement=client.newCall(request.build()).execute().use {
                if(!it.isSuccessful)error(if(it.code==503)"Google Play chưa sẵn sàng; thử lại hoặc khôi phục sau." else "Chưa xác minh được gói (HTTP ${it.code})")
                Entitlement.parse(it.body!!.string(),id)
            }
            check(activeUser()==id)
            cache.save(entitlement)
            mutable.value=PlanState(entitlement)
            entitlement
        } catch(e: Exception) {
            if(activeUser()==id)mutable.value=PlanState(cached(id),message="Chưa cập nhật được gói. Dữ liệu cục bộ vẫn dùng được.")
            throw e
        }
    } }
    suspend fun require(feature: Feature, profile: String): Entitlement {
        val id=profile.removePrefix("ACCOUNT:")
        if(profile!=Profiles.active.value || activeUser()!=id)throw ProRequired()
        // Base cloud features are part of the Free plan. They must keep working when
        // Google Play verification or the entitlement endpoint is temporarily down.
        if(!EntitlementPolicy.requiresServerEntitlement(feature)) return cached(id) ?: Entitlement(id)
        val fresh=refresh().getOrThrow()
        if(!EntitlementPolicy.allows(feature,fresh,id))throw ProRequired()
        return fresh
    }
    companion object {
        @Volatile private var instance: EntitlementRepository?=null
        fun get(context: Context)=instance ?: synchronized(this){instance ?: EntitlementRepository(context.applicationContext).also { instance=it }}
    }
}
