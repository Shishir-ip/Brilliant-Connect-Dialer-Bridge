package com.example

import android.content.Intent
import com.example.handoff.HandoffLauncher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HandoffStrategyFallbackTest {

    class FakeIntentResolver(
        private val allowedActions: Set<String>
    ) : HandoffLauncher.IntentResolver {
        override fun resolveActivity(intent: Intent): Boolean {
            val action = intent.action ?: return false
            return allowedActions.contains(action)
        }
    }

    @Test
    fun testResolverDetectsSupportedAction() {
        val resolver = FakeIntentResolver(setOf(Intent.ACTION_DIAL))
        val dialIntent = Intent(Intent.ACTION_DIAL)
        val viewIntent = Intent(Intent.ACTION_VIEW)

        assertTrue(resolver.resolveActivity(dialIntent))
        assertFalse(resolver.resolveActivity(viewIntent))
    }

    @Test
    fun testFallbackToSecondStrategyWhenFirstUnsupported() {
        val resolver = FakeIntentResolver(setOf(Intent.ACTION_VIEW))
        val dialIntent = Intent(Intent.ACTION_DIAL)
        val viewIntent = Intent(Intent.ACTION_VIEW)

        assertFalse(resolver.resolveActivity(dialIntent))
        assertTrue(resolver.resolveActivity(viewIntent))
    }
}
