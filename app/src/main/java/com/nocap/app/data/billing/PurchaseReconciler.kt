package com.nocap.app.data.billing

import kotlinx.coroutines.CancellationException

/** One foreign/invalid purchase must not hide other purchases or server-side renewals. */
suspend fun <T> reconcilePurchases(purchases: List<T>, verify: suspend (T)->Unit, reconcile: suspend ()->Unit): Boolean {
    var complete=true
    for(purchase in purchases) {
        try { verify(purchase) }
        catch(e: CancellationException){throw e}
        catch(_: Exception){complete=false}
    }
    reconcile()
    return complete
}
