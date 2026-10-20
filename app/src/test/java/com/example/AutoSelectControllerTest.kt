package com.example

import com.example.autoselect.AutoSelectController
import com.example.util.NumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoSelectControllerTest {

    @Before
    fun setup() {
        AutoSelectController.resetForTesting()
    }

    @Test
    fun testArmingMovesToWaitKeypad() {
        val armed = AutoSelectController.arm("01712345678")
        assertTrue(armed)
        assertEquals(AutoSelectController.State.WAIT_KEYPAD, AutoSelectController.stateFlow.value)
    }

    @Test
    fun testArmingThrottlingWithinInterval() {
        val firstArm = AutoSelectController.arm("01712345678")
        assertTrue(firstArm)

        val secondArm = AutoSelectController.arm("01712345678")
        assertFalse(secondArm)
    }

    @Test
    fun testDisarmResetsToIdle() {
        AutoSelectController.arm("01712345678")
        AutoSelectController.disarm()
        assertEquals(AutoSelectController.State.IDLE, AutoSelectController.stateFlow.value)
    }

    @Test
    fun testDigitsMatchingLogicPreventsWrongNumber() {
        val dialed = "01712345678"
        val dialedDigits = NumberNormalizer.digitsOnly(dialed)

        val matchingKeypad = "+8801712345678"
        val matchingDigits = NumberNormalizer.digitsOnly(matchingKeypad)
        assertTrue(NumberNormalizer.numbersMatch(dialedDigits, matchingDigits))

        val mismatchedKeypad = "01799999999"
        val mismatchedDigits = NumberNormalizer.digitsOnly(mismatchedKeypad)
        assertFalse(NumberNormalizer.numbersMatch(dialedDigits, mismatchedDigits))
    }
}
