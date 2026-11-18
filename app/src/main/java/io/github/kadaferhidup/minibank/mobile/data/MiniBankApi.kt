package io.github.kadaferhidup.minibank.mobile.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoint MiniBank API yang dipakai aplikasi nasabah. Token dipasang oleh [AuthInterceptor]. */
interface MiniBankApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @GET("api/me")
    suspend fun me(): Profile

    @GET("api/accounts")
    suspend fun accounts(): List<Account>

    @POST("api/accounts")
    suspend fun openAccount(): Account

    @GET("api/accounts/{number}/inquiry")
    suspend fun inquiry(@Path("number") number: String): Inquiry

    @GET("api/accounts/{number}/statement")
    suspend fun statement(
        @Path("number") number: String,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100, // batas maksimal server per halaman
    ): Statement

    @POST("api/transfers")
    suspend fun transfer(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: TransferRequest,
    ): Response<Transaction>
}
