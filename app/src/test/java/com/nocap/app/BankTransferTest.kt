package com.nocap.app

import com.nocap.app.data.billing.BankApp
import com.nocap.app.data.billing.BankTransferOrder
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class BankTransferTest {
    private val json = """{
      "id":"123e4567-e89b-12d3-a456-426614174000","status":"PENDING","amount":99000,"currency":"VND",
      "paymentContent":"NC0123456789ABCDEF","expiresAt":1900000000000,"paidAt":null,"entitlementExpiresAt":null,
      "bank":{"code":"Vietcombank","name":"Vietcombank","accountNumber":"1017588888","accountHolder":"NOCAP","vietQrBankId":"vcb"},
      "qrUrl":"https://vietqr.app/img?acc=1017588888&bank=Vietcombank&amount=99000&des=NC0123456789ABCDEF"
    }""".trimIndent()

    @Test
    fun `payment response and bank deeplink preserve server owned values`() {
        val order = BankTransferOrder.parse(json)
        assertEquals(99_000, order.amount)
        assertEquals("NC0123456789ABCDEF", order.paymentContent)
        val url = BankApp("mb", "MB Bank", "https://dl.vietqr.io/pay?app=mb", true, 1).paymentUrl(order).toHttpUrl()
        assertEquals("1017588888@vcb", url.queryParameter("ba"))
        assertEquals("99000", url.queryParameter("am"))
        assertEquals(order.paymentContent, url.queryParameter("tn"))
        assertEquals("NOCAP", url.queryParameter("bn"))
    }

    @Test
    fun `payment response rejects unsafe links and invalid order codes`() {
        assertThrows(IllegalArgumentException::class.java) { BankTransferOrder.parse(json.replace("vietqr.app", "evil.example")) }
        assertThrows(IllegalArgumentException::class.java) { BankTransferOrder.parse(json.replace("NC0123456789ABCDEF", "FREEPRO")) }
        val order = BankTransferOrder.parse(json)
        assertTrue(runCatching { BankApp("x", "Bad", "https://evil.example/pay", false, 0).paymentUrl(order) }.isFailure)
    }

    @Test
    fun `payment response supports fallbackQrUrl and img vietqr io`() {
        val order = BankTransferOrder.parse(json)
        assertTrue(order.fallbackQrUrl.startsWith("https://img.vietqr.io/image/vcb-1017588888-qr_only.png"))
        assertTrue(order.fallbackQrUrl.contains("amount=99000"))
        assertTrue(order.fallbackQrUrl.contains("addInfo=NC0123456789ABCDEF"))

        val parsedAlternate = BankTransferOrder.parse(json.replace("https://vietqr.app/img", "https://img.vietqr.io/image/vcb-1017588888-qr_only.png"))
        assertEquals("https://img.vietqr.io/image/vcb-1017588888-qr_only.png?acc=1017588888&bank=Vietcombank&amount=99000&des=NC0123456789ABCDEF", parsedAlternate.qrUrl)
    }
}
