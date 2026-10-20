package com.example

import android.app.Application
import com.example.autoselect.AutoSelectController
import com.example.data.SettingsRepository
import com.example.handoff.HandoffNotificationManager
import com.example.telecom.BridgePhoneAccountManager
import com.example.util.BridgeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BridgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        BridgeLogger.i("BridgeApplication", "Initializing Brilliant Dialer Bridge...")

        HandoffNotificationManager.createChannels(this)
        AutoSelectController.init(this)

        val registered = BridgePhoneAccountManager.registerPhoneAccount(this)
        BridgeLogger.i("BridgeApplication", "Initial PhoneAccount registration success: $registered")

        val repository = SettingsRepository(this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.settingsFlow.collect { settings ->
                AutoSelectController.updateConfig(
                    targetPkg = settings.targetPackageName,
                    label = settings.targetOptionLabel,
                    autoPress = settings.autoPressCallButton,
                    autoSelect = settings.autoSelectBrilliantOut,
                    confirmFirst = settings.confirmBeforeCalling
                )
            }
        }
    }
}
