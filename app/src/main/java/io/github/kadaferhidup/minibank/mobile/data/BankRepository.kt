package io.github.kadaferhidup.minibank.mobile.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Hasil transfer beserta tanda apakah server mengembalikan transaksi lama (permintaan kembar). */
data class TransferResult(val transaction: Transaction, val replayed: Boolean)

/** Satu pintu ke MiniBank API. Semua galat diubah menjadi [ApiError]. */
class BankRepository(private val api: MiniBankApi, private val session: Session) {

    suspend fun login(email: String, password: String) = call {
        val token = api.login(LoginRequest(email.trim(), password))
        session.start(token.accessToken)
    }

    suspend fun register(req: RegisterRequest) = call {
        val res = api.register(req.copy(email = req.email.trim(), fullName = req.fullName.trim(), phone = req.phone.trim()))
        session.start(res.token.accessToken)
        res
    }

    fun logout() = session.end()

    suspend fun profile() = call { api.me() }

    suspend fun accounts() = call { api.accounts() }

    suspend fun openAccount() = call { api.openAccount() }

    suspend fun inquiry(number: String) = call { api.inquiry(number.trim()) }

    suspend fun statement(number: String, from: LocalDate?, to: LocalDate?) =
        call { api.statement(number, from?.toString(), to?.toString()) }

    /**
     * Satu niat transfer memakai satu [idempotencyKey]. Kalau koneksi putus lalu pengguna menekan kirim lagi,
     * kunci yang sama dipakai ulang sehingga saldo tetap terpotong sekali.
     */
    suspend fun transfer(idempotencyKey: String, req: TransferRequest) = call {
        val res = api.transfer(idempotencyKey, req)
        val body = res.body()
        if (!res.isSuccessful || body == null) {
            throw ApiError.fromHttp(res.code(), res.errorBody()?.string())
        }
        TransferResult(body, replayed = res.headers()["Idempotent-Replayed"] == "true")
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Result.failure(ApiError.from(t))
        }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
        }

        fun create(baseUrl: String, session: Session, debug: Boolean): BankRepository {
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(session))
                .apply {
                    if (debug) addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
                // Server demo tidur setelah 15 menit dan butuh sekitar satu menit untuk bangun.
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build()
            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(MiniBankApi::class.java)
            return BankRepository(api, session)
        }
    }
}
