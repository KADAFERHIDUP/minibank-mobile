package io.github.kadaferhidup.minibank.mobile.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

/**
 * Nominal uang dari API dikirim sebagai angka JSON (BigDecimal di server).
 * Dibaca sebagai teks lalu diubah ke BigDecimal supaya tidak ada pembulatan Double.
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val text = if (decoder is JsonDecoder) decoder.decodeJsonElement().jsonPrimitive.content else decoder.decodeString()
        return BigDecimal(text)
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())
}

typealias Money = @Serializable(with = BigDecimalSerializer::class) BigDecimal

// ---------- auth ----------

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val pin: String,
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 0,
    val expiresAt: String? = null,
)

@Serializable
data class Profile(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    val createdAt: String? = null,
)

@Serializable
data class RegisterResponse(val profile: Profile, val account: Account, val token: TokenResponse)

// ---------- rekening ----------

@Serializable
data class Account(
    val accountNumber: String,
    val type: String,
    val currency: String = "IDR",
    val status: String,
    val balance: Money,
    val openedAt: String? = null,
) {
    val isActive: Boolean get() = status == "ACTIVE"
}

@Serializable
data class Inquiry(val accountNumber: String, val holderName: String, val currency: String = "IDR")

@Serializable
data class StatementLine(
    val date: String,
    val reference: String,
    val type: String,
    val direction: String,
    val amount: Money,
    val balanceAfter: Money,
    val counterparty: String? = null,
    val counterpartyName: String? = null,
    val description: String? = null,
) {
    val isCredit: Boolean get() = direction == "CREDIT"
}

@Serializable
data class Statement(
    val accountNumber: String,
    val currentBalance: Money,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
    val items: List<StatementLine>,
)

// ---------- transfer ----------

/** Nominal dikirim dalam rupiah bulat; server menerimanya sebagai BigDecimal. */
@Serializable
data class TransferRequest(
    val fromAccount: String,
    val toAccount: String,
    val amount: Long,
    val description: String? = null,
    val pin: String,
)

@Serializable
data class Transaction(
    val reference: String,
    val type: String,
    val status: String,
    val amount: Money,
    val description: String? = null,
    val sourceAccount: String? = null,
    val destinationAccount: String? = null,
    val createdAt: String,
)

// ---------- error RFC 7807 ----------

@Serializable
data class Problem(
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null,
    val fields: Map<String, String> = emptyMap(),
)
