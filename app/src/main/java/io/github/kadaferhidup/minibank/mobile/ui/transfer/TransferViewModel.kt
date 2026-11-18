package io.github.kadaferhidup.minibank.mobile.ui.transfer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kadaferhidup.minibank.mobile.data.ApiError
import io.github.kadaferhidup.minibank.mobile.data.BankRepository
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.data.Inquiry
import io.github.kadaferhidup.minibank.mobile.data.TransferRequest
import io.github.kadaferhidup.minibank.mobile.data.TransferResult
import io.github.kadaferhidup.minibank.mobile.data.Validation
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

enum class Step { Recipient, Amount, Confirm, Receipt }

/**
 * Transfer empat langkah: cek nama penerima, nominal, konfirmasi dengan PIN, struk.
 * Satu niat transfer = satu Idempotency-Key. Kunci dibuat saat masuk langkah konfirmasi dan dipakai ulang
 * setiap kali tombol kirim ditekan lagi (misalnya setelah koneksi putus), jadi saldo tidak terpotong dua kali.
 * Kunci dibuang kalau penerima atau nominal diubah.
 */
class TransferViewModel(private val repo: BankRepository) : ViewModel() {

    var step by mutableStateOf(Step.Recipient)
        private set

    var toAccount by mutableStateOf("")
        private set
    var recipient by mutableStateOf<Inquiry?>(null)
        private set
    var amountText by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var pin by mutableStateOf("")
        private set

    var busy by mutableStateOf(false)
        private set
    var fieldError by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var result by mutableStateOf<TransferResult?>(null)
        private set

    private var idempotencyKey: String? = null

    val amount: Long? get() = Format.parseAmount(amountText)

    fun onToAccount(v: String) {
        toAccount = v.filter(Char::isDigit).take(10)
        recipient = null
        fieldError = emptyMap()
        error = null
    }

    fun onAmount(v: String) {
        amountText = Format.groupDigits(v)
        fieldError = fieldError - "amount"
    }

    fun onDescription(v: String) {
        description = v.take(140)
    }

    fun onPin(v: String) {
        pin = v.filter(Char::isDigit).take(6)
        fieldError = fieldError - "pin"
        error = null
    }

    fun checkRecipient(fromAccount: String?) {
        Validation.accountNumber(toAccount)?.let {
            fieldError = mapOf("toAccount" to it)
            return
        }
        if (toAccount == fromAccount) {
            fieldError = mapOf("toAccount" to "Tidak bisa transfer ke rekening yang sama")
            return
        }
        busy = true
        error = null
        viewModelScope.launch {
            repo.inquiry(toAccount)
                .onSuccess {
                    recipient = it
                    step = Step.Amount
                }
                .onFailure { e ->
                    val api = e as ApiError
                    if (api.code in setOf("ACCOUNT_NOT_FOUND", "NOT_FOUND", "DESTINATION_INVALID")) {
                        fieldError = mapOf("toAccount" to api.message)
                    } else {
                        error = api.message
                    }
                }
            busy = false
        }
    }

    fun toConfirm(balance: BigDecimal?) {
        Validation.amount(amount, balance)?.let {
            fieldError = mapOf("amount" to it)
            return
        }
        idempotencyKey = UUID.randomUUID().toString()
        pin = ""
        error = null
        step = Step.Confirm
    }

    fun send(fromAccount: String, onDone: () -> Unit) {
        if (busy) return
        if (pin.length != 6) {
            fieldError = mapOf("pin" to "Masukkan 6 angka PIN")
            return
        }
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        val req = TransferRequest(
            fromAccount = fromAccount,
            toAccount = toAccount,
            amount = amount ?: return,
            description = description.trim().ifEmpty { null },
            pin = pin,
        )
        busy = true
        error = null
        viewModelScope.launch {
            repo.transfer(key, req)
                .onSuccess {
                    result = it
                    pin = ""
                    step = Step.Receipt
                    onDone()
                }
                .onFailure { e ->
                    val api = e as ApiError
                    pin = ""
                    when (api.code) {
                        "INVALID_PIN", "PIN_LOCKED" -> fieldError = mapOf("pin" to api.message)
                        // Gagal karena aturan bisnis: transaksi tidak terjadi, kunci baru untuk percobaan berikutnya.
                        "INSUFFICIENT_BALANCE", "LIMIT_EXCEEDED", "DAILY_LIMIT_EXCEEDED", "IDEMPOTENCY_KEY_REUSED" -> {
                            idempotencyKey = null
                            error = api.message
                        }
                        // Jaringan putus / timeout: hasilnya belum pasti, kunci yang sama dipakai lagi saat kirim ulang.
                        else -> error = api.message
                    }
                }
            busy = false
        }
    }

    fun back() {
        error = null
        fieldError = emptyMap()
        step = when (step) {
            Step.Amount -> Step.Recipient
            Step.Confirm -> {
                idempotencyKey = null
                Step.Amount
            }
            else -> step
        }
    }

    fun reset() {
        step = Step.Recipient
        toAccount = ""
        recipient = null
        amountText = ""
        description = ""
        pin = ""
        result = null
        error = null
        fieldError = emptyMap()
        idempotencyKey = null
    }
}
