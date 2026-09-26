package com.nocap.app.data.download

internal object DownloadIntegrity {
    const val MAX_DOWNLOAD_BYTES = 250L * 1024L * 1024L

    fun verifyDeclaredSize(declared: Long) {
        check(declared <= 0 || declared <= MAX_DOWNLOAD_BYTES) {
            "Tệp vượt quá giới hạn tải xuống 250 MB."
        }
    }

    fun verifyProgress(downloaded: Long) {
        check(downloaded <= MAX_DOWNLOAD_BYTES) {
            "Tệp vượt quá giới hạn tải xuống 250 MB."
        }
    }

    fun verifySize(downloaded: Long, declared: Long) {
        verifyProgress(downloaded)
        check(downloaded > 0) { "Máy chủ trả về nội dung trống" }
        check(declared <= 0 || downloaded == declared) { "Tệp tải về không đầy đủ. Vui lòng tải lại." }
    }
}
