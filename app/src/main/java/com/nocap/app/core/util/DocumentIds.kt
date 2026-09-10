package com.nocap.app.core.util

object DocumentIds {
    fun isSafe(id: String): Boolean = Regex("[A-Za-z0-9._-]{1,255}").matches(id) && !id.contains("..")
}
