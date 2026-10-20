package com.example.autoselect

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.handoff.HandoffNotificationManager
import com.example.util.BridgeLogger
import com.example.util.NumberMasker
import com.example.util.NumberNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AutoSelectController {

    private const val TAG = "AutoSelectController"
    private const val OVERALL_TIMEOUT_MS = 12000L
    private const val MIN_ARM_INTERVAL_MS = 3000L

    const val ID_DIALER_EDIT_TEXT = "com.brilliant.connect.com.bd:id/screen_tab_dialer_editText_number"
    const val ID_DIALER_BUTTON = "com.brilliant.connect.com.bd:id/dialler_button"
    const val ID_CALL_TYPE_LIST = "com.brilliant.connect.com.bd:id/call_type_list"
    const val ID_SHEET_ITEM_TEXT = "com.brilliant.connect.com.bd:id/sheet_item_text"
    const val DEFAULT_TARGET_LABEL = "Brilliant-OUT"

    enum class State {
        IDLE,
        WAIT_KEYPAD,
        WAIT_CONFIRMATION,
        WAIT_SHEET,
        DONE,
        TIMEOUT,
        ERROR
    }

    interface AccessibilityBridge {
        fun getRootNode(): AccessibilityNodeInfo?
        fun dispatchTapGesture(x: Float, y: Float, onComplete: (Boolean) -> Unit)
        fun isServiceBound(): Boolean
    }

    private var activeBridge: AccessibilityBridge? = null
    private var appContext: Context? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentState = State.IDLE
    private var armedNumber = ""
    private var armedDigits = ""
    private var lastArmedTimestamp = 0L
    private var callButtonPressed = false
    private var targetLabel = DEFAULT_TARGET_LABEL
    private var autoPressCall = true
    private var autoSelectSheet = true
    private var confirmBeforeCall = false
    private var targetPackage = "com.brilliant.connect.com.bd"

    private val _stateFlow = MutableStateFlow(State.IDLE)
    val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

    private val _lastResultFlow = MutableStateFlow("None")
    val lastResultFlow: StateFlow<String> = _lastResultFlow.asStateFlow()

    private val timeoutRunnable = Runnable {
        if (currentState != State.IDLE && currentState != State.DONE) {
            val stopState = currentState
            BridgeLogger.w(TAG, "Timeout of ${OVERALL_TIMEOUT_MS}ms expired in state $stopState")
            _lastResultFlow.value = "Timeout in $stopState"
            setState(State.TIMEOUT)
            appContext?.let {
                HandoffNotificationManager.showAutoSelectFailedNotification(
                    it,
                    "Couldn't auto-select $targetLabel, tap it manually"
                )
            }
            disarm()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun registerBridge(bridge: AccessibilityBridge) {
        activeBridge = bridge
        BridgeLogger.i(TAG, "Accessibility bridge registered")
    }

    fun unregisterBridge() {
        activeBridge = null
        BridgeLogger.i(TAG, "Accessibility bridge unregistered")
    }

    fun isAccessibilityServiceConnected(): Boolean {
        return activeBridge?.isServiceBound() == true
    }

    fun updateConfig(
        targetPkg: String = "com.brilliant.connect.com.bd",
        label: String = DEFAULT_TARGET_LABEL,
        autoPress: Boolean = true,
        autoSelect: Boolean = true,
        confirmFirst: Boolean = false
    ) {
        targetPackage = targetPkg
        targetLabel = label.trim()
        autoPressCall = autoPress
        autoSelectSheet = autoSelect
        confirmBeforeCall = confirmFirst
    }

    @Synchronized
    fun arm(number: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastArmedTimestamp < MIN_ARM_INTERVAL_MS) {
            BridgeLogger.w(TAG, "arm() called too soon (${now - lastArmedTimestamp}ms ago), ignoring")
            return false
        }

        lastArmedTimestamp = now
        armedNumber = number
        armedDigits = NumberNormalizer.digitsOnly(number)
        callButtonPressed = false

        if (armedDigits.isEmpty()) {
            BridgeLogger.w(TAG, "arm() called with empty number digits")
            return false
        }

        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, OVERALL_TIMEOUT_MS)

        setState(State.WAIT_KEYPAD)
        val masked = NumberMasker.mask(number)
        BridgeLogger.i(TAG, "Armed for dialed number: $masked, overall timeout 12s")
        _lastResultFlow.value = "Armed ($masked)"
        return true
    }

    @Synchronized
    fun disarm() {
        mainHandler.removeCallbacks(timeoutRunnable)
        armedNumber = ""
        armedDigits = ""
        callButtonPressed = false
        if (currentState != State.TIMEOUT && currentState != State.ERROR && currentState != State.DONE) {
            setState(State.IDLE)
        }
    }

    @Synchronized
    fun resetForTesting() {
        mainHandler.removeCallbacks(timeoutRunnable)
        lastArmedTimestamp = 0L
        armedNumber = ""
        armedDigits = ""
        callButtonPressed = false
        setState(State.IDLE)
    }

    private fun setState(newState: State) {
        currentState = newState
        _stateFlow.value = newState
    }

    fun userConfirmedCall() {
        if (currentState == State.WAIT_CONFIRMATION) {
            BridgeLogger.i(TAG, "User confirmed call via notification action")
            setState(State.WAIT_KEYPAD)
            pressCallButtonIfReady()
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventPkg = event.packageName?.toString() ?: ""

        if (eventPkg.isNotEmpty() && eventPkg != targetPackage) {
            if (currentState == State.WAIT_KEYPAD || currentState == State.WAIT_SHEET) {
                BridgeLogger.w(TAG, "Window switched to $eventPkg while armed; disarming for safety")
                _lastResultFlow.value = "Left target app ($eventPkg)"
                disarm()
            }
            return
        }

        if (currentState == State.IDLE || currentState == State.DONE || currentState == State.TIMEOUT || currentState == State.ERROR) {
            return
        }

        val bridge = activeBridge ?: return
        val rootNode = bridge.getRootNode() ?: return

        try {
            when (currentState) {
                State.WAIT_KEYPAD -> processWaitKeypad(rootNode)
                State.WAIT_SHEET -> processWaitSheet(rootNode, bridge)
                else -> { }
            }
        } catch (e: Exception) {
            BridgeLogger.e(TAG, "Error in onAccessibilityEvent", e)
            _lastResultFlow.value = "Error: ${e.message}"
            setState(State.ERROR)
            disarm()
        }
    }

    private fun processWaitKeypad(rootNode: AccessibilityNodeInfo) {
        val numberNodes = rootNode.findAccessibilityNodeInfosByViewId(ID_DIALER_EDIT_TEXT)
        if (numberNodes.isNullOrEmpty()) {
            return
        }

        val numberNode = numberNodes[0]
        val keypadText = numberNode.text?.toString() ?: ""
        val keypadDigits = NumberNormalizer.digitsOnly(keypadText)

        if (!NumberNormalizer.numbersMatch(armedDigits, keypadDigits)) {
            val maskedKeypad = NumberMasker.mask(keypadText)
            val maskedArmed = NumberMasker.mask(armedNumber)
            BridgeLogger.w(
                TAG,
                "Keypad number ($maskedKeypad) does NOT match dialed number ($maskedArmed). Aborting call to prevent wrong number."
            )
            _lastResultFlow.value = "Mismatch ($maskedKeypad vs $maskedArmed)"
            return
        }

        BridgeLogger.i(TAG, "Keypad number verified successfully: ${NumberMasker.mask(keypadText)}")
        _lastResultFlow.value = "Keypad verified"

        if (!autoPressCall) {
            BridgeLogger.i(TAG, "Auto-press call button is OFF; stopped at keypad")
            _lastResultFlow.value = "Stopped at keypad (setting OFF)"
            setState(State.DONE)
            disarm()
            return
        }

        if (confirmBeforeCall) {
            BridgeLogger.i(TAG, "Confirm before calling is ON; awaiting user confirmation")
            setState(State.WAIT_CONFIRMATION)
            appContext?.let {
                HandoffNotificationManager.showConfirmationNotification(it, armedNumber)
            }
            return
        }

        pressCallButtonIfReady(rootNode)
    }

    private fun pressCallButtonIfReady(root: AccessibilityNodeInfo? = null) {
        if (callButtonPressed) return

        val bridge = activeBridge ?: return
        val rootNode = root ?: bridge.getRootNode() ?: return

        val callButtons = rootNode.findAccessibilityNodeInfosByViewId(ID_DIALER_BUTTON)
        if (callButtons.isNullOrEmpty()) {
            BridgeLogger.w(TAG, "Dialer call button ($ID_DIALER_BUTTON) not found")
            return
        }

        val button = callButtons[0]
        val clicked = performClickOnNodeOrAncestor(button)
        if (clicked) {
            callButtonPressed = true
            BridgeLogger.i(TAG, "Call button clicked successfully. Moving to WAIT_SHEET")
            _lastResultFlow.value = "Call button pressed"
            setState(State.WAIT_SHEET)
        } else {
            BridgeLogger.w(TAG, "Failed to click dialer button")
        }
    }

    private fun processWaitSheet(rootNode: AccessibilityNodeInfo, bridge: AccessibilityBridge) {
        val listNodes = rootNode.findAccessibilityNodeInfosByViewId(ID_CALL_TYPE_LIST)
        if (listNodes.isNullOrEmpty()) {
            return
        }

        val itemTextNodes = rootNode.findAccessibilityNodeInfosByViewId(ID_SHEET_ITEM_TEXT)
        if (itemTextNodes.isNullOrEmpty()) {
            return
        }

        var matchingNode: AccessibilityNodeInfo? = null
        for (node in itemTextNodes) {
            val text = node.text?.toString()?.trim() ?: ""
            if (text.equals(targetLabel, ignoreCase = true)) {
                matchingNode = node
                break
            }
        }

        if (matchingNode == null) {
            return
        }

        BridgeLogger.i(TAG, "Found target row with text: '$targetLabel'")

        val parentRow = matchingNode.parent
        var success = false

        if (parentRow != null) {
            success = parentRow.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                BridgeLogger.i(TAG, "Successfully clicked parent row via ACTION_CLICK")
            }
        }

        if (!success) {
            success = performClickOnNodeOrAncestor(matchingNode)
            if (success) {
                BridgeLogger.i(TAG, "Successfully clicked ancestor via ACTION_CLICK")
            }
        }

        if (!success) {
            val bounds = Rect()
            if (parentRow != null) {
                parentRow.getBoundsInScreen(bounds)
            } else {
                matchingNode.getBoundsInScreen(bounds)
            }

            if (bounds.width() > 0 && bounds.height() > 0) {
                val centerX = bounds.centerX().toFloat()
                val centerY = bounds.centerY().toFloat()
                BridgeLogger.i(TAG, "Attempting gesture tap on row bounds center: ($centerX, $centerY)")

                bridge.dispatchTapGesture(centerX, centerY) { gestureSucceeded ->
                    if (gestureSucceeded) {
                        BridgeLogger.i(TAG, "Gesture tap dispatched successfully")
                        _lastResultFlow.value = "Call placed ($targetLabel via tap)"
                    } else {
                        BridgeLogger.w(TAG, "Gesture tap dispatch returned false")
                        _lastResultFlow.value = "Gesture tap failed"
                    }
                    setState(State.DONE)
                    disarm()
                }
                return
            } else {
                BridgeLogger.w(TAG, "Bounds for target row are empty; unable to gesture tap")
            }
        } else {
            _lastResultFlow.value = "Call placed ($targetLabel via click)"
            setState(State.DONE)
            disarm()
        }
    }

    private fun performClickOnNodeOrAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val result = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (result) return true
            }
            current = current.parent
        }
        return false
    }
}
