package io.github.kadaferhidup.minibank.mobile.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/** Menguji lapisan data terhadap server tiruan yang meniru respons MiniBank API. */
class BankRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var session: Session
    private lateinit var repo: BankRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        session = Session()
        repo = BankRepository.create(server.url("/").toString(), session, debug = false)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(code: Int, body: String, vararg headers: Pair<String, String>) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body).apply {
            headers.forEach { (k, v) -> setHeader(k, v) }
        }

    @Test
    fun loginStartsSessionAndSendsBearerToken() = runTest {
        server.enqueue(json(200, """{"accessToken":"abc.def","tokenType":"Bearer","expiresIn":1800,"expiresAt":"2026-11-18T10:00:00Z"}"""))
        server.enqueue(json(200, """[{"accountNumber":"1012345672","type":"SAVINGS","currency":"IDR","status":"ACTIVE","balance":1250000.50,"openedAt":"2026-11-01T02:00:00Z"}]"""))

        assertTrue(repo.login("ahmad@contoh.id", "Rahasia123").isSuccess)
        assertEquals("abc.def", session.token.value)

        val accounts = repo.accounts().getOrThrow()
        assertEquals(0, BigDecimal("1250000.50").compareTo(accounts.single().balance))

        server.takeRequest() // login
        val req = server.takeRequest()
        assertEquals("Bearer abc.def", req.getHeader("Authorization"))
    }

    @Test
    fun problemJsonBecomesReadableErrorWithFields() = runTest {
        server.enqueue(
            json(
                400,
                """{"type":"x","title":"Bad Request","status":400,"detail":"Data tidak valid","code":"VALIDATION_FAILED","fields":{"phone":"format nomor HP tidak valid"}}""",
            ),
        )
        val err = repo.register(RegisterRequest("Ahmad", "a@b.id", "123", "Rahasia123", "246810")).exceptionOrNull() as ApiError
        assertEquals("VALIDATION_FAILED", err.code)
        assertEquals("format nomor HP tidak valid", err.fields["phone"])
        assertNull(session.token.value)
    }

    @Test
    fun expiredTokenEndsSession() = runTest {
        session.start("old-token")
        server.enqueue(json(401, """{"status":401,"code":"UNAUTHORIZED","detail":"Token tidak valid"}"""))

        val result = repo.accounts()
        assertTrue(result.isFailure)
        assertNull(session.token.value)
        assertEquals("Sesi kamu sudah berakhir. Silakan masuk lagi.", session.logoutReason.value)
    }

    @Test
    fun transferSendsIdempotencyKeyAndDetectsReplay() = runTest {
        session.start("t")
        val tx = """{"reference":"TRX123","type":"TRANSFER","status":"COMPLETED","amount":125000.00,"description":"Bayar kos","sourceAccount":"1012345672","destinationAccount":"1098765439","createdAt":"2026-11-18T09:00:00Z"}"""
        server.enqueue(json(201, tx, "Idempotent-Replayed" to "false"))
        server.enqueue(json(200, tx, "Idempotent-Replayed" to "true"))

        val req = TransferRequest("1012345672", "1098765439", 125_000, "Bayar kos", "246810")
        val first = repo.transfer("key-1", req).getOrThrow()
        val second = repo.transfer("key-1", req).getOrThrow()

        assertFalse(first.replayed)
        assertTrue(second.replayed)
        assertEquals(first.transaction.reference, second.transaction.reference)

        val sent = server.takeRequest()
        assertEquals("key-1", sent.getHeader("Idempotency-Key"))
        assertTrue(sent.body.readUtf8().contains("\"amount\":125000"))
    }

    @Test
    fun businessErrorKeepsServerMessage() = runTest {
        session.start("t")
        server.enqueue(json(422, """{"status":422,"code":"INSUFFICIENT_BALANCE","detail":"Saldo tidak cukup"}"""))
        val err = repo.transfer("k", TransferRequest("1012345672", "1098765439", 999_999, null, "246810")).exceptionOrNull() as ApiError
        assertEquals("INSUFFICIENT_BALANCE", err.code)
        assertEquals("Saldo tidak cukup", err.message)
        assertEquals("t", session.token.value) // galat bisnis tidak mengeluarkan pengguna
    }

    @Test
    fun wrongLoginDoesNotLookLikeExpiredSession() = runTest {
        server.enqueue(json(401, """{"status":401,"code":"INVALID_CREDENTIALS","detail":"Email atau password salah"}"""))
        val err = repo.login("a@b.id", "salah").exceptionOrNull() as ApiError
        assertEquals("INVALID_CREDENTIALS", err.code)
        assertNull(session.logoutReason.value)
    }
}
