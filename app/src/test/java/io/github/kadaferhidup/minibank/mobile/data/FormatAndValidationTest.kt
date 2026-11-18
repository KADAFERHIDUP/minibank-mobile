package io.github.kadaferhidup.minibank.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FormatAndValidationTest {

    @Test
    fun rupiahUsesIndonesianGroupingAndHidesZeroCents() {
        assertEquals("Rp1.250.000", Format.rupiah(BigDecimal("1250000.00")))
        assertEquals("Rp0", Format.rupiah(BigDecimal.ZERO))
        assertEquals("Rp10.000,50", Format.rupiah(BigDecimal("10000.5")))
    }

    @Test
    fun rupiahWithSign() {
        assertEquals("+Rp125.000", Format.rupiah(BigDecimal("125000"), withSign = true))
        assertEquals("-Rp125.000", Format.rupiah(BigDecimal("125000"), withSign = true, negative = true))
    }

    @Test
    fun amountInputIsGroupedAndParsed() {
        assertEquals("1.250.000", Format.groupDigits("1250000"))
        assertEquals("10.000", Format.groupDigits("0010.000"))
        assertEquals("", Format.groupDigits("abc"))
        assertEquals(1_250_000L, Format.parseAmount("1.250.000"))
        assertNull(Format.parseAmount(""))
        assertNull(Format.parseAmount("1234567890123456")) // terlalu panjang
    }

    @Test
    fun accountNumberIsSplitForReading() {
        assertEquals("1012 3456 72", Format.accountNumber("1012345672"))
    }

    @Test
    fun luhnMatchesServerGenerator() {
        // Dihitung dengan algoritma AccountNumberGenerator di backend.
        assertTrue(Validation.luhnValid("1012345672"))
        assertTrue(Validation.luhnValid("1098765439"))
        assertTrue(Validation.luhnValid("1000000016"))
        // Salah ketik satu angka harus ketahuan.
        assertFalse(Validation.luhnValid("1012345673"))
        assertFalse(Validation.luhnValid("1012345772"))
    }

    @Test
    fun accountNumberValidation() {
        assertNull(Validation.accountNumber("1012345672"))
        assertEquals("Nomor rekening harus 10 digit", Validation.accountNumber("12345"))
        assertNotNull(Validation.accountNumber("1012345673"))
    }

    @Test
    fun amountRulesFollowServerLimits() {
        assertNotNull(Validation.amount(null, null))
        assertNotNull(Validation.amount(9_999, null))
        assertNull(Validation.amount(10_000, null))
        assertNotNull(Validation.amount(50_000_001, null))
        assertEquals("Saldo tidak cukup", Validation.amount(200_000, BigDecimal("150000")))
        assertNull(Validation.amount(150_000, BigDecimal("150000.00")))
    }

    @Test
    fun registerValidation() {
        assertTrue(Validation.register("Ahmad Umraithi", "ahmad@contoh.id", "081234567890", "Rahasia123", "246810").isEmpty())
        val errs = Validation.register("A", "bukan-email", "12345", "pendek", "123456")
        assertEquals(setOf("fullName", "email", "phone", "password", "pin"), errs.keys)
        assertEquals("PIN terlalu mudah ditebak", errs["pin"])
    }

    @Test
    fun weakPinListMatchesServer() {
        assertTrue(Validation.isWeakPin("123456"))
        assertTrue(Validation.isWeakPin("111111"))
        assertFalse(Validation.isWeakPin("246810"))
        assertFalse(Validation.isWeakPin("234567")) // diterima server, jadi diterima juga di aplikasi
    }
}
