package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.telecom.TelecomManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.autoselect.AutoSelectController
import com.example.data.BridgeSettings
import com.example.data.SettingsRepository
import com.example.handoff.HandoffLauncher
import com.example.util.BridgeLogger
import com.example.util.LogEntry
import com.example.util.NumberMasker
import com.example.util.NumberNormalizer
import com.example.util.PermissionHelper
import com.example.util.SafetyChecker
import com.example.util.SetupStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val context: Context get() = getApplication<Application>().applicationContext

    val settings: StateFlow<BridgeSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BridgeSettings()
    )

    private val _setupStatus = MutableStateFlow(
        PermissionHelper.getFullSetupStatus(context, "com.brilliant.connect.com.bd")
    )
    val setupStatus: StateFlow<SetupStatus> = _setupStatus.asStateFlow()

    val autoSelectState: StateFlow<AutoSelectController.State> = AutoSelectController.stateFlow
    val lastResult: StateFlow<String> = AutoSelectController.lastResultFlow
    val logs: StateFlow<List<LogEntry>> = BridgeLogger.logsFlow

    private val _defaultDialerPkg = MutableStateFlow("Unknown")
    val defaultDialerPkg: StateFlow<String> = _defaultDialerPkg.asStateFlow()

    private val _callingAccountsList = MutableStateFlow<List<String>>(emptyList())
    val callingAccountsList: StateFlow<List<String>> = _callingAccountsList.asStateFlow()

    private val _testLogEntries = MutableStateFlow<List<String>>(emptyList())
    val testLogEntries: StateFlow<List<String>> = _testLogEntries.asStateFlow()

    private val _showPaidWarningDialog = MutableStateFlow(false)
    val showPaidWarningDialog: StateFlow<Boolean> = _showPaidWarningDialog.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val currentTargetPkg = settings.value.targetPackageName
        _setupStatus.value = PermissionHelper.getFullSetupStatus(context, currentTargetPkg)

        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (tm != null) {
            try {
                _defaultDialerPkg.value = tm.defaultDialerPackage ?: "None / System Default"
            } catch (e: Exception) {
                _defaultDialerPkg.value = "Error reading dialer"
            }

            if (PermissionHelper.hasPhonePermissions(context)) {
                try {
                    val accounts = tm.callCapablePhoneAccounts
                    _callingAccountsList.value = accounts.map { "${it.componentName.packageName} [${it.id}]" }
                } catch (e: Exception) {
                    _callingAccountsList.value = listOf("Error: ${e.message}")
                }
            } else {
                _callingAccountsList.value = listOf("Grant READ_PHONE_STATE to view registered accounts")
            }
        }
    }

    fun updateAutoSelect(enabled: Boolean) = viewModelScope.launch { repository.setAutoSelect(enabled) }
    fun updateTargetLabel(label: String) = viewModelScope.launch { repository.setTargetOptionLabel(label) }
    fun updateAutoPressCallButton(enabled: Boolean) = viewModelScope.launch { repository.setAutoPressCallButton(enabled) }
    fun updateConfirmBeforeCall(enabled: Boolean) = viewModelScope.launch { repository.setConfirmBeforeCalling(enabled) }
    fun updateNumberFormat(format: String) = viewModelScope.launch { repository.setNumberFormat(format) }
    fun updateHandoffStrategy(strategy: String) = viewModelScope.launch { repository.setHandoffStrategy(strategy) }
    fun updateRouteAllCalls(enabled: Boolean) = viewModelScope.launch { repository.setRouteAllOutgoingCalls(enabled) }
    fun updateStartOnBoot(enabled: Boolean) = viewModelScope.launch { repository.setStartOnBoot(enabled) }

    fun markPaidWarningAcknowledged() = viewModelScope.launch {
        repository.setHasSeenPaidCallWarning(true)
        _showPaidWarningDialog.value = false
    }

    fun triggerPaidWarningCheck(onAcknowledged: () -> Unit) {
        if (!settings.value.hasSeenPaidCallWarning) {
            _showPaidWarningDialog.value = true
        } else {
            onAcknowledged()
        }
    }

    fun addTestLog(entry: String) {
        _testLogEntries.value = _testLogEntries.value + entry
    }

    fun clearTestLogs() {
        _testLogEntries.value = emptyList()
    }

    fun executeTestHandoff(number: String, fullAuto: Boolean) {
        clearTestLogs()
        val masked = NumberMasker.mask(number)
        addTestLog("Starting test for $masked (Full auto: $fullAuto)...")

        val safety = SafetyChecker.check(number)
        if (safety !is SafetyChecker.SafetyResult.Valid) {
            addTestLog("Test halted by safety check: $safety")
            return
        }

        AutoSelectController.updateConfig(
            targetPkg = settings.value.targetPackageName,
            label = settings.value.targetOptionLabel,
            autoPress = fullAuto && settings.value.autoPressCallButton,
            autoSelect = fullAuto && settings.value.autoSelectBrilliantOut,
            confirmFirst = settings.value.confirmBeforeCalling
        )

        val formatOpt = when (settings.value.numberFormat) {
            "e164" -> NumberNormalizer.FormatOption.E164
            "local" -> NumberNormalizer.FormatOption.LOCAL
            else -> NumberNormalizer.FormatOption.AS_DIALED
        }

        addTestLog("Initiating handoff via ${settings.value.handoffStrategy}...")
        val success = HandoffLauncher.performHandoff(
            context = context,
            rawNumber = number,
            targetPackage = settings.value.targetPackageName,
            strategy = settings.value.handoffStrategy,
            formatOption = formatOpt
        )

        if (success) {
            addTestLog("Handoff intent sent. Brilliant Connect keypad opening...")
            if (fullAuto) {
                addTestLog("Accessibility controller armed (12s timeout). Watching for keypad and call button...")
            } else {
                addTestLog("Test handoff only mode: Stopping at keypad without pressing call button.")
            }
        } else {
            addTestLog("Handoff launch returned false. Check target installation or logs.")
        }
    }

    fun executeSimulatedTest(number: String) {
        clearTestLogs()
        val masked = NumberMasker.mask(number)
        viewModelScope.launch {
            addTestLog("[Step 1/6] Validating safety rules for $masked...")
            delay(400)
            val safety = SafetyChecker.check(number)
            if (safety !is SafetyChecker.SafetyResult.Valid) {
                addTestLog("Simulation failed: $safety")
                return@launch
            }
            addTestLog("[Step 1/6] Passed: not emergency, not USSD, length valid.")

            delay(600)
            addTestLog("[Step 2/6] Formatting number (${settings.value.numberFormat}): ${NumberNormalizer.clean(number)}")

            delay(600)
            addTestLog("[Step 3/6] Simulating Intent launch to ${settings.value.targetPackageName}...")
            addTestLog("[Step 3/6] Resolved Keypad screen (${AutoSelectController.ID_DIALER_EDIT_TEXT})")

            delay(700)
            addTestLog("[Step 4/6] Comparing dialed digits against Keypad text...")
            addTestLog("[Step 4/6] Match verified on last 10 digits!")

            delay(700)
            addTestLog("[Step 5/6] Auto-pressing dialer button (${AutoSelectController.ID_DIALER_BUTTON})...")
            addTestLog("[Step 5/6] Bottom sheet opened (${AutoSelectController.ID_CALL_TYPE_LIST})")

            delay(800)
            addTestLog("[Step 6/6] Locating row with label: '${settings.value.targetOptionLabel}'...")
            addTestLog("[Step 6/6] Target row clicked! Paid Brilliant-OUT call triggered successfully.")

            delay(300)
            addTestLog("Simulation complete. All state transitions succeeded.")
        }
    }

    fun copyLogsToClipboard() {
        val allLogs = BridgeLogger.getAllLogsFormatted()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Brilliant Bridge Logs", allLogs)
        clipboard?.setPrimaryClip(clip)
    }

    fun clearLogs() {
        BridgeLogger.clear()
    }
}
