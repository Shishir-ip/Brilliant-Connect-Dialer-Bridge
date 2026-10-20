package com.example.autoselect

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.util.BridgeLogger

class BrilliantOutAccessibilityService : AccessibilityService(), AutoSelectController.AccessibilityBridge {

    companion object {
        private const val TAG = "BrilliantAccessibility"
        private var isBound = false

        fun isConnected(): Boolean = isBound
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isBound = true
        AutoSelectController.registerBridge(this)
        BridgeLogger.i(TAG, "BrilliantOutAccessibilityService connected and ready")
    }

    override fun onDestroy() {
        isBound = false
        AutoSelectController.unregisterBridge()
        super.onDestroy()
        BridgeLogger.i(TAG, "BrilliantOutAccessibilityService destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        AutoSelectController.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        BridgeLogger.w(TAG, "BrilliantOutAccessibilityService interrupted by system")
    }

    override fun getRootNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow
    }

    override fun dispatchTapGesture(x: Float, y: Float, onComplete: (Boolean) -> Unit) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    onComplete(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    onComplete(false)
                }
            },
            Handler(Looper.getMainLooper())
        )

        if (!dispatched) {
            onComplete(false)
        }
    }

    override fun isServiceBound(): Boolean {
        return isBound
    }
}
