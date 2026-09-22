package com.nocap.app.data.billing

import org.json.JSONObject

enum class Feature {
    LOCAL_IMPORT, LOCAL_READING, OFFLINE_LIBRARY, BOOKMARKS, HIGHLIGHTS, NOTES,
    TAGS_COLLECTIONS, READING_MEMORY, MULTI_DEVICE_SYNC, PRIVATE_CLOUD, CLOUD_BACKUP,
    ADVANCED_READING_MEMORY, KNOWLEDGE_EXPORT, ADVANCED_CLOUD
}
data class Entitlement(val userId: String, val plan: String = "FREE", val status: String = "NONE", val expiresAt: Long = 0,
    val updatedAt: Long = 0, val productId: String? = null, val accountId: String = "", val autoRenew: Boolean = false, val configured: Boolean = false) {
    fun toJson() = JSONObject().put("userId",userId).put("plan",plan).put("status",status).put("expiresAt",expiresAt)
        .put("updatedAt",updatedAt).put("productId",productId).put("billingAccountId",accountId).put("autoRenew",autoRenew).put("configured",configured).toString()
    companion object {
        fun parse(text: String, expectedUser: String): Entitlement {
            val j=JSONObject(text);require(j.getString("userId")==expectedUser)
            return Entitlement(expectedUser,j.getString("plan").also { require(it in setOf("FREE","PRO")) },j.getString("status"),j.optLong("expiresAt"),j.getLong("updatedAt"),
                j.optString("productId").takeIf { it.isNotBlank() && it!="null" },j.optString("billingAccountId"),j.optBoolean("autoRenew"),j.optBoolean("configured"))
        }
    }
}
object EntitlementPolicy {
    // Các tính năng Pro mới: Reading Memory nâng cao, Xuất tri thức và Cloud nâng cao
    private val proFeatures = setOf(
        Feature.ADVANCED_READING_MEMORY,
        Feature.KNOWLEDGE_EXPORT,
        Feature.ADVANCED_CLOUD
    )
    fun allows(feature: Feature, state: Entitlement?, userId: String?, now: Long = System.currentTimeMillis()): Boolean {
        if (feature !in proFeatures) return true
        return state != null && userId != null && state.userId == userId && state.plan == "PRO" &&
            state.status in setOf("ACTIVE", "IN_GRACE_PERIOD", "CANCELED") && state.expiresAt > now &&
            state.updatedAt <= now + 300_000 && now - state.updatedAt <= 24 * 60 * 60 * 1000L
    }
    inline fun <T> withAccess(feature: Feature, state: Entitlement?, userId: String?, now: Long = System.currentTimeMillis(), action: () -> T): T {
        if (!allows(feature, state, userId, now)) throw ProRequired()
        return action()
    }
}
class ProRequired : Exception("Tính năng đám mây cần Pro. Tài liệu và ghi chú trên máy vẫn dùng được.")
class UserEntitlementCache(private val read: (String)->String?, private val write: (String,String)->Unit) {
    private fun key(user: String)=com.nocap.app.data.sync.Profiles.key("ACCOUNT:$user")
    fun load(user: String): Entitlement? = runCatching { read(key(user))?.let { Entitlement.parse(it,user) } }.getOrNull()
    fun save(entitlement: Entitlement) = write(key(entitlement.userId),entitlement.toJson())
}
enum class PurchaseOutcome { VERIFY, PENDING, CANCELLED, RESTORE, ERROR }
object PurchasePolicy {
    fun outcome(response: Int, purchaseState: Int): PurchaseOutcome = when(response) {
        1 -> PurchaseOutcome.CANCELLED
        7 -> PurchaseOutcome.RESTORE
        0 -> if(purchaseState==2)PurchaseOutcome.PENDING else if(purchaseState==1)PurchaseOutcome.VERIFY else PurchaseOutcome.ERROR
        else -> PurchaseOutcome.ERROR
    }
}
