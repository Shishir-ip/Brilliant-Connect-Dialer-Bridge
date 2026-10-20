package com.example.handoff

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.util.BridgeLogger
import com.example.util.NumberMasker

object HandoffNotificationManager {

    private const val TAG = "HandoffNotification"
    const val CHANNEL_HANDOFF_ID = "channel_handoff_calls"
    const val CHANNEL_ALERTS_ID = "channel_bridge_alerts"

    const val NOTIFICATION_ID_HANDOFF = 1001
    const val NOTIFICATION_ID_CONFIRMATION = 1002
    const val NOTIFICATION_ID_ALERT = 1003
    const val NOTIFICATION_ID_AUTO_FAIL = 1004

    const val ACTION_CONFIRM_CALL = "com.example.ACTION_CONFIRM_CALL"
    const val EXTRA_PHONE_NUMBER = "extra_phone_number"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val handoffChannel = NotificationChannel(
            CHANNEL_HANDOFF_ID,
            context.getString(R.string.channel_handoff_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_handoff_desc)
            enableVibration(true)
            setShowBadge(true)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS_ID,
            context.getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alerts_desc)
        }

        nm.createNotificationChannel(handoffChannel)
        nm.createNotificationChannel(alertsChannel)
    }

    fun showHandoffNotification(context: Context, number: String, targetPackage: String) {
        val masked = NumberMasker.mask(number)
        BridgeLogger.i(TAG, "Posting high-priority handoff notification for $masked")

        val launchIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            setPackage(targetPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_HANDOFF,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            nm?.canUseFullScreenIntent() == true
        } else {
            true
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_HANDOFF_ID)
            .setSmallIcon(R.drawable.ic_bridge_account)
            .setContentTitle("Tap to Call via Brilliant Connect")
            .setContentText("Dialing $masked...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (canUseFullScreen) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_HANDOFF, builder.build())
        } catch (e: SecurityException) {
            BridgeLogger.e(TAG, "Failed to post handoff notification: missing POST_NOTIFICATIONS permission", e)
        }
    }

    fun showConfirmationNotification(context: Context, number: String) {
        val masked = NumberMasker.mask(number)
        BridgeLogger.i(TAG, "Posting confirmation notification for $masked")

        val confirmIntent = Intent(context, HandoffActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_CALL
            putExtra(EXTRA_PHONE_NUMBER, number)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_CONFIRMATION,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_HANDOFF_ID)
            .setSmallIcon(R.drawable.ic_bridge_account)
            .setContentTitle("Confirm Brilliant Call")
            .setContentText("Place paid Brilliant-OUT call to $masked?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_bridge_account, "Call Now", confirmPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CONFIRMATION, notification)
        } catch (e: SecurityException) {
            BridgeLogger.e(TAG, "Missing notification permission", e)
        }
    }

    fun showAlertNotification(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_bridge_account)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ALERT, notification)
        } catch (e: SecurityException) {
            BridgeLogger.e(TAG, "Missing notification permission for alert", e)
        }
    }

    fun showAutoSelectFailedNotification(context: Context, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_bridge_account)
            .setContentTitle("Manual Action Required")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_AUTO_FAIL, notification)
        } catch (e: SecurityException) {
            BridgeLogger.e(TAG, "Missing notification permission", e)
        }
    }
}
