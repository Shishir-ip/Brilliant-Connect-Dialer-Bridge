package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.SettingsRepository
import com.example.handoff.HandoffNotificationManager
import com.example.util.BridgeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        BridgeLogger.i(TAG, "Broadcast received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            HandoffNotificationManager.createChannels(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = SettingsRepository(context).settingsFlow.first()
                    if (settings.startOnBoot) {
                        val registered = BridgePhoneAccountManager.registerPhoneAccount(context)
                        BridgeLogger.i(TAG, "PhoneAccount re-registration status: $registered")
                    } else {
                        BridgeLogger.i(TAG, "Start on boot is disabled in settings; skipping auto-registration")
                    }
                } catch (e: Exception) {
                    BridgeLogger.e(TAG, "Error in BootReceiver", e)
                }
            }
        }
    }
}
