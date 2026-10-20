package com.example.handoff

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.autoselect.AutoSelectController
import com.example.util.BridgeLogger

class HandoffActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            HandoffNotificationManager.ACTION_CONFIRM_CALL -> {
                BridgeLogger.i("HandoffActionReceiver", "ACTION_CONFIRM_CALL received")
                AutoSelectController.userConfirmedCall()
            }
        }
    }
}
