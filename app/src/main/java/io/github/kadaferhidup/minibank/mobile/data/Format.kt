package io.github.kadaferhidup.minibank.mobile.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Format angka dan tanggal gaya Indonesia, tanpa bergantung pada locale HP pengguna. */
object Format {
    private val idLocale: Locale = Locale.forLanguageTag("id-ID")
    private val symbols = DecimalFormatSymbols(idLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val jakarta: ZoneId = ZoneId.of("Asia/Jakarta")

    /** 1250000 -> "Rp1.250.000". Sen hanya ditampilkan kalau ada. */
    fun rupiah(amount: BigDecimal, withSign: Boolean = false, negative: Boolean = false): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        val pattern = if (scaled.stripTrailingZeros().scale() <= 0) "#,##0" else "#,##0.00"
        val body = DecimalFormat(pattern, symbols).format(scaled.abs())
        val sign = when {
            !withSign -> if (scaled.signum() < 0) "-" else ""
            negative -> "-"
            else -> "+"
        }
        return "${sign}Rp$body"
    }

    /** "1250000" -> "1.250.000" untuk kolom isian nominal. */
    fun groupDigits(digits: String): String {
        val clean = digits.filter(Char::isDigit).trimStart('0')
        if (clean.isEmpty()) return ""
        return clean.reversed().chunked(3).joinToString(".").reversed()
    }

    /** "1234567890" -> "1234 5678 90" supaya nomor rekening mudah dibaca. */
    fun accountNumber(number: String): String = number.chunked(4).joinToString(" ")

    /** Teks isian nominal -> rupiah bulat, atau null kalau kosong / terlalu besar. */
    fun parseAmount(text: String): Long? {
        val digits = text.filter(Char::isDigit)
        if (digits.isEmpty() || digits.length > 15) return null
        return digits.toLong()
    }

    private val dateTime = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", idLocale)
    private val dateOnly = DateTimeFormatter.ofPattern("d MMM yyyy", idLocale)
    private val dayHeader = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", idLocale)

    private fun zoned(iso: String) = runCatching { Instant.parse(iso).atZone(jakarta) }.getOrNull()

    fun dateTime(iso: String?): String = iso?.let { zoned(it)?.format(dateTime) } ?: "-"
    fun date(iso: String?): String = iso?.let { zoned(it)?.format(dateOnly) } ?: "-"
    fun dayHeader(iso: String): String = zoned(iso)?.format(dayHeader) ?: iso
    fun dayKey(iso: String): String = zoned(iso)?.toLocalDate()?.toString() ?: iso
}
