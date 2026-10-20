package com.example

import com.example.util.SafetyChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyCheckerTest {

    @Test
    fun testEmergencyNumbersBlocked() {
        assertTrue(SafetyChecker.isEmergency("999"))
        assertTrue(SafetyChecker.isEmergency("112"))
        assertTrue(SafetyChecker.isEmergency("911"))
        assertTrue(SafetyChecker.isEmergency("100"))
        assertTrue(SafetyChecker.isEmergency("101"))
        assertTrue(SafetyChecker.isEmergency("102"))
        assertTrue(SafetyChecker.isEmergency("108"))
        assertTrue(SafetyChecker.isEmergency("000"))
        assertTrue(SafetyChecker.isEmergency("110"))
        assertTrue(SafetyChecker.isEmergency("119"))

        val result999 = SafetyChecker.check("999")
        assertTrue(result999 is SafetyChecker.SafetyResult.Emergency)

        val result911 = SafetyChecker.check("911")
        assertTrue(result911 is SafetyChecker.SafetyResult.Emergency)
    }

    @Test
    fun testNormalNumbersNotEmergency() {
        assertFalse(SafetyChecker.isEmergency("01712345678"))
        assertFalse(SafetyChecker.isEmergency("+8801712345678"))
        assertFalse(SafetyChecker.isEmergency("029876543"))
    }

    @Test
    fun testUssdCodesBlocked() {
        assertTrue(SafetyChecker.isUssd("*123#"))
        assertTrue(SafetyChecker.isUssd("*121*1#"))
        assertTrue(SafetyChecker.isUssd("#123#"))

        val checkUssd = SafetyChecker.check("*123#")
        assertTrue(checkUssd is SafetyChecker.SafetyResult.Ussd)
    }

    @Test
    fun testTooShortNumbersBlocked() {
        assertTrue(SafetyChecker.isTooShort("12"))
        assertTrue(SafetyChecker.isTooShort(""))
        assertFalse(SafetyChecker.isTooShort("123"))

        val checkEmpty = SafetyChecker.check("")
        assertTrue(checkEmpty is SafetyChecker.SafetyResult.TooShort)

        val checkShort = SafetyChecker.check("45")
        assertTrue(checkShort is SafetyChecker.SafetyResult.TooShort)
    }

    @Test
    fun testValidNumbersPassSafety() {
        val checkNormal = SafetyChecker.check("01712345678")
        assertTrue(checkNormal is SafetyChecker.SafetyResult.Valid)

        val checkInternational = SafetyChecker.check("+8801812345678")
        assertTrue(checkInternational is SafetyChecker.SafetyResult.Valid)
    }
}
