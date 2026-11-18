package io.github.kadaferhidup.minibank.mobile.data

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Galat yang sudah siap ditampilkan ke pengguna.
 * [code] mengikuti field `code` dari respons RFC 7807 MiniBank, [fields] berisi pesan per kolom formulir.
 */
class ApiError(
    val code: String,
    override val message: String,
    val fields: Map<String, String> = emptyMap(),
    val httpStatus: Int? = null,
) : Exception(message) {

    val isUnauthorized: Boolean get() = httpStatus == 401 && code != "INVALID_CREDENTIALS"

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Pesan bahasa Indonesia untuk kode yang sering muncul; dipakai kalau server tidak mengirim `detail`. */
        private val messages = mapOf(
            "VALIDATION_FAILED" to "Periksa lagi isian yang ditandai.",
            "INVALID_CREDENTIALS" to "Email atau password salah.",
            "TOO_MANY_ATTEMPTS" to "Terlalu banyak percobaan masuk. Coba lagi beberapa menit lagi.",
            "EMAIL_TAKEN" to "Email ini sudah terdaftar.",
            "WEAK_PIN" to "PIN terlalu mudah ditebak. Pilih kombinasi lain.",
            "INVALID_PIN" to "PIN salah.",
            "PIN_LOCKED" to "PIN terkunci karena salah 3 kali. Coba lagi 15 menit lagi.",
            "INSUFFICIENT_BALANCE" to "Saldo tidak cukup.",
            "LIMIT_EXCEEDED" to "Nominal melebihi batas per transaksi.",
            "DAILY_LIMIT_EXCEEDED" to "Batas transfer harian sudah tercapai.",
            "INVALID_AMOUNT" to "Nominal tidak valid.",
            "SAME_ACCOUNT" to "Tidak bisa transfer ke rekening yang sama.",
            "ACCOUNT_NOT_ACTIVE" to "Rekening sedang tidak aktif.",
            "DESTINATION_INVALID" to "Nomor rekening tujuan tidak valid.",
            "ACCOUNT_NOT_FOUND" to "Rekening tidak ditemukan.",
            "ACCOUNT_LIMIT_REACHED" to "Kamu sudah punya jumlah rekening maksimal.",
            "IDEMPOTENCY_KEY_REUSED" to "Transaksi ini sudah pernah dikirim dengan isi berbeda. Mulai transfer baru.",
            "CONCURRENT_UPDATE" to "Ada transaksi lain yang sedang diproses. Coba lagi.",
        )

        fun messageFor(code: String): String? = messages[code]

        fun from(t: Throwable): ApiError = when (t) {
            is ApiError -> t
            is HttpException -> fromHttp(t.code(), t.response()?.errorBody()?.string())
            is SocketTimeoutException -> ApiError(
                "TIMEOUT",
                "Server lama merespons. Server demo mungkin sedang bangun dari tidur, coba lagi sebentar.",
            )
            is IOException -> ApiError("NETWORK", "Tidak ada koneksi ke server. Periksa internet kamu.")
            else -> ApiError("UNKNOWN", "Terjadi kesalahan. Coba lagi.")
        }

        fun fromHttp(status: Int, body: String?): ApiError {
            val problem = body?.takeIf { it.isNotBlank() }?.let {
                runCatching { json.decodeFromString(Problem.serializer(), it) }.getOrNull()
            }
            val code = problem?.code ?: when (status) {
                401 -> "UNAUTHORIZED"
                403 -> "FORBIDDEN"
                404 -> "NOT_FOUND"
                in 500..599 -> "SERVER_ERROR"
                else -> "HTTP_$status"
            }
            // Pesan `detail` dari server sudah berbahasa Indonesia, jadi diutamakan.
            val message = problem?.detail?.takeIf { it.isNotBlank() }
                ?: messages[code]
                ?: when (status) {
                    401 -> "Sesi kamu sudah berakhir. Silakan masuk lagi."
                    in 500..599 -> "Server sedang bermasalah. Coba lagi nanti."
                    else -> "Permintaan ditolak server ($status)."
                }
            return ApiError(code, message, problem?.fields.orEmpty(), status)
        }
    }
}
