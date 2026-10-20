package com.example

import com.example.util.NumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberNormalizerTest {

    @Test
    fun test1_emptyNumber() {
        assertEquals("", NumberNormalizer.clean(""))
        assertEquals("", NumberNormalizer.toE164(""))
        assertEquals("", NumberNormalizer.toLocal(""))
    }

    @Test
    fun test2_whitespaceAndPunctuation() {
        val input = " (017) 123-456 78 "
        assertEquals("01712345678", NumberNormalizer.clean(input))
    }

    @Test
    fun test3_standardBangladeshMobile() {
        val input = "01712345678"
        assertEquals("+8801712345678", NumberNormalizer.toE164(input))
        assertEquals("01712345678", NumberNormalizer.toLocal(input))
    }

    @Test
    fun test4_bangladeshMobileWithPlus88() {
        val input = "+8801812345678"
        assertEquals("+8801812345678", NumberNormalizer.toE164(input))
        assertEquals("01812345678", NumberNormalizer.toLocal(input))
    }

    @Test
    fun test5_bangladeshMobileWith88NoPlus() {
        val input = "8801912345678"
        assertEquals("+8801912345678", NumberNormalizer.toE164(input))
        assertEquals("01912345678", NumberNormalizer.toLocal(input))
    }

    @Test
    fun test6_bangladeshMobileWith0088() {
        val input = "008801612345678"
        assertEquals("+8801612345678", NumberNormalizer.toE164(input))
        assertEquals("01612345678", NumberNormalizer.toLocal(input))
    }

    @Test
    fun test7_ussdCodeWithHashAndStar() {
        val input = "*123#"
        assertEquals("*123#", NumberNormalizer.clean(input))
    }

    @Test
    fun test8_ussdSubMenu() {
        val input = "*121*1*2#"
        assertEquals("*121*1*2#", NumberNormalizer.clean(input))
    }

    @Test
    fun test9_emergencyShortCode() {
        val input = "999"
        assertEquals("999", NumberNormalizer.clean(input))
    }

    @Test
    fun test10_internationalUSNumber() {
        val input = "+1 (415) 555-2671"
        assertEquals("+14155552671", NumberNormalizer.toE164(input))
    }

    @Test
    fun test11_internationalUKNumber() {
        val input = "+44 7911 123456"
        assertEquals("+447911123456", NumberNormalizer.toE164(input))
    }

    @Test
    fun test12_international00Prefix() {
        val input = "00447911123456"
        assertEquals("+447911123456", NumberNormalizer.toE164(input))
    }

    @Test
    fun test13_last10DigitsExtraction() {
        assertEquals("1712345678", NumberNormalizer.extractLast10Digits("01712345678"))
        assertEquals("1712345678", NumberNormalizer.extractLast10Digits("+8801712345678"))
        assertEquals("1712345678", NumberNormalizer.extractLast10Digits("8801712345678"))
    }

    @Test
    fun test14_numbersMatchAcrossPrefixVariations() {
        assertTrue(NumberNormalizer.numbersMatch("01712345678", "+8801712345678"))
        assertTrue(NumberNormalizer.numbersMatch("8801712345678", "01712345678"))
        assertTrue(NumberNormalizer.numbersMatch("+8801712345678", "008801712345678"))
    }

    @Test
    fun test15_numbersMatchRejectsDifferentNumbers() {
        assertFalse(NumberNormalizer.numbersMatch("01712345678", "01799999999"))
        assertFalse(NumberNormalizer.numbersMatch("", "01712345678"))
    }

    @Test
    fun test16_formatOptionAsDialed() {
        val input = "01712345678"
        assertEquals("01712345678", NumberNormalizer.format(input, NumberNormalizer.FormatOption.AS_DIALED))
    }
}
