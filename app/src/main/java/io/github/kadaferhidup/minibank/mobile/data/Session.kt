package io.github.kadaferhidup.minibank.mobile.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token hanya disimpan di memori, sama seperti versi web yang memakai sessionStorage:
 * aplikasi ditutup = harus masuk lagi. Tidak ada token yang tertinggal di penyimpanan HP.
 */
class Session {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    /** Alasan keluar terakhir, ditampilkan sekali di layar masuk. */
    private val _logoutReason = MutableStateFlow<String?>(null)
    val logoutReason: StateFlow<String?> = _logoutReason.asStateFlow()

    val isLoggedIn: Boolean get() = _token.value != null

    fun start(token: String) {
        _logoutReason.value = null
        _token.value = token
    }

    fun end(reason: String? = null) {
        if (_token.value == null) return
        _logoutReason.value = reason
        _token.value = null
    }

    fun consumeLogoutReason() {
        _logoutReason.value = null
    }
}

/** Memasang header Authorization dan langsung mengeluarkan pengguna kalau token ditolak (401). */
class AuthInterceptor(private val session: Session) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.token.value
        val request = chain.request().newBuilder().apply {
            header("Accept", "application/json")
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        val response = chain.proceed(request)
        if (response.code == 401 && token != null) {
            session.end("Sesi kamu sudah berakhir. Silakan masuk lagi.")
        }
        return response
    }
}
