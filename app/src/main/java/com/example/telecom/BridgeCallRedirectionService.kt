package com.example.telecom

import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import com.example.data.SettingsRepository
import com.example.handoff.HandoffLauncher
import com.example.util.BridgeLogger
import com.example.util.NumberMasker
import com.example.util.SafetyChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BridgeCallRedirectionService : CallRedirectionService() {

    companion object {
        private const val TAG = "CallRedirectionService"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        val rawNumber = handle.schemeSpecificPart ?: ""
        val masked = NumberMasker.mask(rawNumber)
        BridgeLogger.i(TAG, "onPlaceCall intercepted call to $masked")

        serviceScope.launch {
            try {
                val repository = SettingsRepository(applicationContext)
                val settings = repository.settingsFlow.first()

                if (!settings.routeAllOutgoingCalls) {
                    placeCallUnmodified()
                    return@launch
                }

                when (val safety = SafetyChecker.check(rawNumber)) {
                    is SafetyChecker.SafetyResult.Emergency -> {
                        BridgeLogger.w(TAG, "Emergency number detected in redirection; placing unmodified")
                        placeCallUnmodified()
                        return@launch
                    }
                    is SafetyChecker.SafetyResult.Ussd -> {
                        BridgeLogger.w(TAG, "USSD code detected in redirection; placing unmodified")
                        placeCallUnmodified()
                        return@launch
                    }
                    is SafetyChecker.SafetyResult.TooShort -> {
                        BridgeLogger.w(TAG, "Short number detected in redirection; placing unmodified")
                        placeCallUnmodified()
                        return@launch
                    }
                    is SafetyChecker.SafetyResult.Valid -> {
                        BridgeLogger.i(TAG, "Canceling outgoing cellular call and redirecting to Brilliant Connect")
                        cancelCall()

                        HandoffLauncher.performHandoff(
                            context = applicationContext,
                            rawNumber = rawNumber,
                            targetPackage = settings.targetPackageName,
                            strategy = settings.handoffStrategy
                        )
                    }
                }
            } catch (e: Exception) {
                BridgeLogger.e(TAG, "Error during call redirection", e)
                placeCallUnmodified()
            }
        }
    }
}
