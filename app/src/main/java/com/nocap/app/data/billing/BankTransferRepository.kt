package com.nocap.app.data.billing

import android.content.Context
import com.nocap.app.BuildConfig
import com.nocap.app.data.auth.CloudAuthRepository
import com.nocap.app.data.sync.Profiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class BankRecipient(
    val code: String,
    val name: String,
    val accountNumber: String,
    val accountHolder: String,
    val vietQrBankId: String
)

data class BankTransferOrder(
    val id: String,
    val status: String,
    val amount: Long,
    val currency: String,
    val paymentContent: String,
    val expiresAt: Long,
    val paidAt: Long?,
    val entitlementExpiresAt: Long?,
    val bank: BankRecipient,
    val qrUrl: String
) {
    companion object {
        fun parse(raw: String): BankTransferOrder {
            val data = JSONObject(raw)
            val bank = data.getJSONObject("bank")
            return BankTransferOrder(
                id = data.getString("id").also { require(Regex("[0-9a-fA-F-]{36}").matches(it)) },
                status = data.getString("status").also { require(it in setOf("PENDING", "PROCESSING", "PAID", "EXPIRED", "CANCELLED")) },
                amount = data.getLong("amount").also { require(it > 0) },
                currency = data.getString("currency").also { require(it == "VND") },
                paymentContent = data.getString("paymentContent").also { require(Regex("NC[A-F0-9]{16}").matches(it)) },
                expiresAt = data.getLong("expiresAt"),
                paidAt = data.optLongOrNull("paidAt"),
                entitlementExpiresAt = data.optLongOrNull("entitlementExpiresAt"),
                bank = BankRecipient(
                    code = bank.getString("code"),
                    name = bank.getString("name"),
                    accountNumber = bank.getString("accountNumber"),
                    accountHolder = bank.getString("accountHolder"),
                    vietQrBankId = bank.getString("vietQrBankId").also { require(Regex("[a-z0-9-]{2,20}").matches(it)) }
                ),
                qrUrl = data.getString("qrUrl").also { require(it.toHttpUrlOrNull()?.let { url -> url.isHttps && url.host == "vietqr.app" } == true) }
            )
        }
    }
}

data class BankApp(val id: String, val name: String, val deepLink: String, val autoFill: Boolean, val popularity: Int) {
    fun paymentUrl(order: BankTransferOrder): String {
        val base = deepLink.toHttpUrlOrNull() ?: error("Invalid bank app link")
        require(base.isHttps && base.host == "dl.vietqr.io")
        return base.newBuilder()
            .addQueryParameter("ba", "${order.bank.accountNumber}@${order.bank.vietQrBankId}")
            .addQueryParameter("am", order.amount.toString())
            .addQueryParameter("tn", order.paymentContent)
            .addQueryParameter("bn", order.bank.accountHolder)
            .build().toString()
    }
}

class BankPaymentException(val code: String) : Exception(code)

private fun JSONObject.optLongOrNull(name: String): Long? = if (isNull(name) || !has(name)) null else getLong(name)

class BankTransferRepository private constructor(private val context: Context, private val client: OkHttpClient = OkHttpClient()) {
    suspend fun create(): BankTransferOrder = request("/api/v1/billing/sepay/order", true)
    suspend fun status(id: String): BankTransferOrder {
        require(Regex("[0-9a-fA-F-]{36}").matches(id))
        return request("/api/v1/billing/sepay/orders/$id", false)
    }

    private suspend fun request(path: String, post: Boolean): BankTransferOrder = withContext(Dispatchers.IO) {
        val expectedUser = Profiles.active.value.takeIf { it.startsWith("ACCOUNT:") } ?: throw BankPaymentException("SIGN_IN_REQUIRED")
        val token = CloudAuthRepository.getInstance(context).getIdToken(false) ?: throw BankPaymentException("SIGN_IN_REQUIRED")
        val builder = Request.Builder().url(BuildConfig.BACKEND_BASE_URL + path).header("Authorization", "Bearer $token")
        if (post) builder.post("{}".toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val code = runCatching { JSONObject(raw).getJSONObject("error").getString("code") }.getOrDefault("PAYMENT_UNAVAILABLE")
                throw BankPaymentException(code)
            }
            check(Profiles.active.value == expectedUser)
            BankTransferOrder.parse(raw)
        }
    }

    suspend fun bankApps(): List<BankApp> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.vietqr.io/v2/android-app-deeplinks").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw BankPaymentException("BANK_APPS_UNAVAILABLE")
            val raw = response.body?.string().orEmpty()
            if (raw.length > 512_000) throw BankPaymentException("BANK_APPS_UNAVAILABLE")
            val apps = JSONObject(raw).getJSONArray("apps")
            buildList {
                for (index in 0 until apps.length()) {
                    val app = apps.getJSONObject(index)
                    val id = app.getString("appId")
                    val link = app.getString("deeplink")
                    if (!Regex("[a-z0-9-]{1,30}").matches(id)) continue
                    val url = link.toHttpUrlOrNull() ?: continue
                    if (!url.isHttps || url.host != "dl.vietqr.io") continue
                    add(BankApp(id, app.getString("appName").take(100), link, app.optInt("autofill") == 1, app.optInt("monthlyInstall")))
                }
            }.sortedWith(compareByDescending<BankApp> { it.autoFill }.thenByDescending { it.popularity }.thenBy { it.name })
        }
    }

    companion object {
        @Volatile private var instance: BankTransferRepository? = null
        fun get(context: Context) = instance ?: synchronized(this) {
            instance ?: BankTransferRepository(context.applicationContext).also { instance = it }
        }
    }
}
