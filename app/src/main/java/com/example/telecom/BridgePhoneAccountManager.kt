package com.example.telecom

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.R
import com.example.util.BridgeLogger

object BridgePhoneAccountManager {

    private const val TAG = "PhoneAccountManager"
    const val ACCOUNT_ID = "brilliant_bridge_account"

    fun getPhoneAccountHandle(context: Context): PhoneAccountHandle {
        val componentName = ComponentName(context, BridgeConnectionService::class.java)
        return PhoneAccountHandle(componentName, ACCOUNT_ID)
    }

    fun registerPhoneAccount(context: Context): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecomManager == null) {
            BridgeLogger.e(TAG, "TelecomManager not available on this device")
            return false
        }

        val handle = getPhoneAccountHandle(context)
        val label = context.getString(R.string.phone_account_label)
        val icon = Icon.createWithResource(context, R.drawable.ic_bridge_account)

        val phoneAccount = PhoneAccount.builder(handle, label)
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setIcon(icon)
            .setShortDescription("Call via Brilliant Connect app")
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .build()

        return try {
            telecomManager.registerPhoneAccount(phoneAccount)
            BridgeLogger.i(TAG, "Successfully registered PhoneAccount: $ACCOUNT_ID with CAPABILITY_CALL_PROVIDER")
            true
        } catch (e: Exception) {
            BridgeLogger.e(TAG, "Failed to register PhoneAccount", e)
            false
        }
    }

    fun isAccountRegistered(context: Context): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        val handle = getPhoneAccountHandle(context)
        return try {
            val account = telecomManager.getPhoneAccount(handle)
            account != null
        } catch (e: Exception) {
            BridgeLogger.w(TAG, "Error checking if account registered: ${e.message}")
            false
        }
    }

    fun isAccountEnabled(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            BridgeLogger.w(TAG, "READ_PHONE_STATE not granted; cannot query call capable accounts")
            return false
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        val handle = getPhoneAccountHandle(context)

        return try {
            val accounts = telecomManager.callCapablePhoneAccounts
            accounts.any { it == handle }
        } catch (e: SecurityException) {
            BridgeLogger.w(TAG, "SecurityException querying callCapablePhoneAccounts: ${e.message}")
            false
        } catch (e: Exception) {
            BridgeLogger.e(TAG, "Error checking if account enabled", e)
            false
        }
    }

    fun openCallingAccountsSettings(context: Context) {
        val changeAccountsIntent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (context.packageManager.resolveActivity(changeAccountsIntent, 0) != null) {
                context.startActivity(changeAccountsIntent)
                return
            }
        } catch (e: Exception) {
            BridgeLogger.w(TAG, "ACTION_CHANGE_PHONE_ACCOUNTS failed: ${e.message}")
        }

        val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(settingsIntent)
        } catch (e2: Exception) {
            BridgeLogger.e(TAG, "Cannot open settings screen", e2)
        }
    }
}
