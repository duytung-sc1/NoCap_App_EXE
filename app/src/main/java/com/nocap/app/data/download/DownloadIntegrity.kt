package com.nocap.app.data.download

internal object DownloadIntegrity {
    fun verifySize(downloaded: Long, declared: Long) {
        check(downloaded > 0) { "Máy chủ trả về nội dung trống" }
        check(declared <= 0 || downloaded == declared) { "Tệp tải về không đầy đủ. Vui lòng tải lại." }
    }
}
