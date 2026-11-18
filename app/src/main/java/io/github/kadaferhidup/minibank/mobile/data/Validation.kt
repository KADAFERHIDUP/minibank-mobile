package io.github.kadaferhidup.minibank.mobile.data

/**
 * Pengecekan di sisi aplikasi, sama dengan aturan server, supaya pengguna langsung tahu salahnya
 * tanpa menunggu jaringan. Server tetap memeriksa ulang.
 */
object Validation {
    private val email = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val phone = Regex("^(\\+62|0)8[0-9]{8,11}$")
    private val pin = Regex("^\\d{6}$")

    const val MIN_TRANSFER = 10_000L
    const val MAX_TRANSFER = 50_000_000L

    fun register(fullName: String, email: String, phone: String, password: String, pin: String): Map<String, String> {
        val e = linkedMapOf<String, String>()
        if (fullName.trim().length !in 3..100) e["fullName"] = "Nama 3 sampai 100 karakter"
        if (!this.email.matches(email.trim())) e["email"] = "Format email tidak valid"
        if (!this.phone.matches(phone.trim())) e["phone"] = "Format nomor HP tidak valid, contoh 081234567890"
        if (password.length < 8) e["password"] = "Password minimal 8 karakter"
        else if (!password.any(Char::isLetter) || !password.any(Char::isDigit)) e["password"] = "Password harus berisi huruf dan angka"
        if (!this.pin.matches(pin)) e["pin"] = "PIN harus 6 digit angka"
        else if (isWeakPin(pin)) e["pin"] = "PIN terlalu mudah ditebak"
        return e
    }

    /** Daftar yang sama dengan `PinService.WEAK` di server. */
    private val weakPins = setOf(
        "123456", "654321", "000000", "111111", "222222", "333333", "444444",
        "555555", "666666", "777777", "888888", "999999", "112233", "121212", "123123",
    )

    fun isWeakPin(p: String): Boolean = p in weakPins

    fun accountNumber(n: String): String? = when {
        n.length != 10 || !n.all(Char::isDigit) -> "Nomor rekening harus 10 digit"
        !luhnValid(n) -> "Nomor rekening tidak valid, periksa lagi angkanya"
        else -> null
    }

    /** Nomor rekening MiniBank memakai digit pemeriksa Luhn di posisi terakhir. */
    fun luhnValid(n: String): Boolean {
        if (n.isEmpty() || !n.all(Char::isDigit)) return false
        var sum = 0
        n.reversed().forEachIndexed { i, c ->
            var d = c - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }

    fun amount(value: Long?, balance: java.math.BigDecimal?): String? = when {
        value == null -> "Isi nominal transfer"
        value < MIN_TRANSFER -> "Minimal transfer ${Format.rupiah(java.math.BigDecimal(MIN_TRANSFER))}"
        value > MAX_TRANSFER -> "Maksimal ${Format.rupiah(java.math.BigDecimal(MAX_TRANSFER))} per transaksi"
        balance != null && java.math.BigDecimal(value) > balance -> "Saldo tidak cukup"
        else -> null
    }
}
