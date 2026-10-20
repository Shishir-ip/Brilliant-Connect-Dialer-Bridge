package com.example.handoff

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.example.autoselect.AutoSelectController
import com.example.util.BridgeLogger
import com.example.util.NumberMasker
import com.example.util.NumberNormalizer

object HandoffLauncher {

    private const val TAG = "HandoffLauncher"
    const val DEFAULT_TARGET_PACKAGE = "com.brilliant.connect.com.bd"

    interface IntentResolver {
        fun resolveActivity(intent: Intent): Boolean
    }

    class DefaultIntentResolver(private val context: Context) : IntentResolver {
        override fun resolveActivity(intent: Intent): Boolean {
            return try {
                context.packageManager.resolveActivity(intent, 0) != null
            } catch (e: Exception) {
                false
            }
        }
    }

    fun isTargetInstalled(context: Context, targetPackage: String = DEFAULT_TARGET_PACKAGE): Boolean {
        return try {
            context.packageManager.getPackageInfo(targetPackage, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openPlayStore(context: Context, targetPackage: String = DEFAULT_TARGET_PACKAGE) {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPackage")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$targetPackage")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                BridgeLogger.e(TAG, "Cannot open browser for Play Store link", e2)
            }
        }
    }

    fun copyToClipboard(context: Context, number: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Dialed Number", number)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Number copied, open Brilliant Connect and paste", Toast.LENGTH_LONG).show()
        HandoffNotificationManager.showAlertNotification(
            context,
            "Number Copied",
            "Number copied to clipboard. Open Brilliant Connect and paste."
        )
    }

    fun performHandoff(
        context: Context,
        rawNumber: String,
        targetPackage: String = DEFAULT_TARGET_PACKAGE,
        strategy: String = "dial_intent",
        formatOption: NumberNormalizer.FormatOption = NumberNormalizer.FormatOption.AS_DIALED,
        resolver: IntentResolver = DefaultIntentResolver(context)
    ): Boolean {
        val normalizedNumber = NumberNormalizer.format(rawNumber, formatOption)
        val masked = NumberMasker.mask(normalizedNumber)
        BridgeLogger.i(TAG, "Starting handoff for $masked to $targetPackage with strategy: $strategy")

        if (!isTargetInstalled(context, targetPackage)) {
            BridgeLogger.w(TAG, "Target app $targetPackage is not installed! Redirecting to Play Store")
            HandoffNotificationManager.showAlertNotification(
                context,
                "Brilliant Connect Not Installed",
                "Please install Brilliant Connect to place calls through the bridge."
            )
            openPlayStore(context, targetPackage)
            return false
        }

        var startedSuccessfully = false
        val canDrawOverlays = Settings.canDrawOverlays(context)

        val intentsToTry = mutableListOf<Pair<String, Intent>>()

        when (strategy) {
            "view_intent" -> {
                intentsToTry.add(
                    "ACTION_VIEW" to Intent(Intent.ACTION_VIEW, Uri.parse("tel:$normalizedNumber")).apply {
                        setPackage(targetPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
                intentsToTry.add(
                    "ACTION_DIAL" to Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalizedNumber")).apply {
                        setPackage(targetPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            }
            "launcher_copy" -> {
                copyToClipboard(context, normalizedNumber)
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    intentsToTry.add("LAUNCHER" to launchIntent)
                }
            }
            else -> {
                intentsToTry.add(
                    "ACTION_DIAL" to Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalizedNumber")).apply {
                        setPackage(targetPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
                intentsToTry.add(
                    "ACTION_VIEW" to Intent(Intent.ACTION_VIEW, Uri.parse("tel:$normalizedNumber")).apply {
                        setPackage(targetPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            }
        }

        if (canDrawOverlays) {
            for ((name, intent) in intentsToTry) {
                if (resolver.resolveActivity(intent)) {
                    try {
                        context.startActivity(intent)
                        BridgeLogger.i(TAG, "Successfully started activity via direct start with $name")
                        startedSuccessfully = true
                        break
                    } catch (e: Exception) {
                        BridgeLogger.w(TAG, "Direct launch with $name failed: ${e.message}")
                    }
                }
            }
        }

        if (!startedSuccessfully) {
            BridgeLogger.i(TAG, "Display over other apps missing or direct launch failed. Posting high-priority notification")
            try {
                HandoffNotificationManager.showHandoffNotification(context, normalizedNumber, targetPackage)
                startedSuccessfully = true
            } catch (e: Exception) {
                BridgeLogger.e(TAG, "Failed to post handoff notification", e)
            }
        }

        if (!startedSuccessfully) {
            BridgeLogger.w(TAG, "All launch methods failed. Falling back to clipboard copy")
            copyToClipboard(context, normalizedNumber)
            return false
        }

        AutoSelectController.arm(normalizedNumber)
        return true
    }
}
