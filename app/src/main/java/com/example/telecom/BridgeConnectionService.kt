package com.example.telecom

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import com.example.handoff.HandoffLauncher
import com.example.handoff.HandoffNotificationManager
import com.example.util.BridgeLogger
import com.example.util.NumberMasker
import com.example.util.SafetyChecker

class BridgeConnectionService : ConnectionService() {

    companion object {
        private const val TAG = "BridgeConnectionService"
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        BridgeLogger.i(TAG, "onCreateOutgoingConnection invoked by Telecom framework")

        val connection = BridgeConnection()

        if (request == null || request.address == null) {
            BridgeLogger.e(TAG, "Outgoing connection request or address is null")
            connection.setDisconnected(DisconnectCause(DisconnectCause.ERROR, "Invalid call address"))
            connection.destroy()
            return connection
        }

        try {
            val rawNumber = request.address.schemeSpecificPart ?: ""
            val masked = NumberMasker.mask(rawNumber)
            BridgeLogger.i(TAG, "Outgoing call requested for dialed number: $masked")

            when (val safety = SafetyChecker.check(rawNumber)) {
                is SafetyChecker.SafetyResult.Emergency -> {
                    BridgeLogger.w(TAG, "Blocked emergency call attempt: ${safety.reason}")
                    HandoffNotificationManager.showAlertNotification(
                        applicationContext,
                        "Emergency Call Rejected",
                        safety.reason
                    )
                    connection.setDisconnected(DisconnectCause(DisconnectCause.REJECTED, safety.reason))
                    connection.destroy()
                    return connection
                }
                is SafetyChecker.SafetyResult.Ussd -> {
                    BridgeLogger.w(TAG, "Blocked USSD call attempt: ${safety.reason}")
                    HandoffNotificationManager.showAlertNotification(
                        applicationContext,
                        "USSD Code Blocked",
                        safety.reason
                    )
                    connection.setDisconnected(DisconnectCause(DisconnectCause.REJECTED, safety.reason))
                    connection.destroy()
                    return connection
                }
                is SafetyChecker.SafetyResult.TooShort -> {
                    BridgeLogger.w(TAG, "Blocked invalid short number: ${safety.reason}")
                    HandoffNotificationManager.showAlertNotification(
                        applicationContext,
                        "Invalid Number",
                        safety.reason
                    )
                    connection.setDisconnected(DisconnectCause(DisconnectCause.REJECTED, safety.reason))
                    connection.destroy()
                    return connection
                }
                is SafetyChecker.SafetyResult.Valid -> {
                    BridgeLogger.i(TAG, "Number safety checks passed. Initiating handoff to Brilliant Connect")
                    HandoffLauncher.performHandoff(applicationContext, rawNumber)

                    connection.setDisconnected(
                        DisconnectCause(DisconnectCause.LOCAL, "Handoff to Brilliant Connect complete")
                    )
                    connection.destroy()
                    return connection
                }
            }
        } catch (e: Exception) {
            BridgeLogger.e(TAG, "Unexpected error in onCreateOutgoingConnection", e)
            HandoffNotificationManager.showAlertNotification(
                applicationContext,
                "Bridge Connection Error",
                "Failed to route call: ${e.message}"
            )
            connection.setDisconnected(DisconnectCause(DisconnectCause.ERROR, e.message))
            connection.destroy()
            return connection
        }
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
        BridgeLogger.e(TAG, "onCreateOutgoingConnectionFailed called by Telecom framework")
        HandoffNotificationManager.showAlertNotification(
            applicationContext,
            "Call Failed",
            "Telecom framework could not create outgoing connection."
        )
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        BridgeLogger.w(TAG, "Incoming connection attempted but not supported by bridge")
        return Connection.createFailedConnection(DisconnectCause(DisconnectCause.REJECTED, "Incoming calls not supported"))
    }
}

class BridgeConnection : Connection() {
    init {
        connectionCapabilities = CAPABILITY_MUTE
    }
}
