package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bridge_settings")

data class BridgeSettings(
    val autoSelectBrilliantOut: Boolean = true,
    val targetOptionLabel: String = "Brilliant-OUT",
    val autoPressCallButton: Boolean = true,
    val confirmBeforeCalling: Boolean = false,
    val numberFormat: String = "as_dialed",
    val handoffStrategy: String = "dial_intent",
    val routeAllOutgoingCalls: Boolean = false,
    val hasSeenPaidCallWarning: Boolean = false,
    val targetPackageName: String = "com.brilliant.connect.com.bd",
    val startOnBoot: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val AUTO_SELECT = booleanPreferencesKey("auto_select_brilliant_out")
        val TARGET_LABEL = stringPreferencesKey("target_option_label")
        val AUTO_PRESS_CALL = booleanPreferencesKey("auto_press_call_button")
        val CONFIRM_BEFORE_CALL = booleanPreferencesKey("confirm_before_calling")
        val NUMBER_FORMAT = stringPreferencesKey("number_format")
        val HANDOFF_STRATEGY = stringPreferencesKey("handoff_strategy")
        val ROUTE_ALL_CALLS = booleanPreferencesKey("route_all_outgoing_calls")
        val HAS_SEEN_PAID_WARNING = booleanPreferencesKey("has_seen_paid_call_warning")
        val TARGET_PACKAGE = stringPreferencesKey("target_package_name")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
    }

    val settingsFlow: Flow<BridgeSettings> = context.dataStore.data.map { preferences ->
        BridgeSettings(
            autoSelectBrilliantOut = preferences[PreferencesKeys.AUTO_SELECT] ?: true,
            targetOptionLabel = preferences[PreferencesKeys.TARGET_LABEL] ?: "Brilliant-OUT",
            autoPressCallButton = preferences[PreferencesKeys.AUTO_PRESS_CALL] ?: true,
            confirmBeforeCalling = preferences[PreferencesKeys.CONFIRM_BEFORE_CALL] ?: false,
            numberFormat = preferences[PreferencesKeys.NUMBER_FORMAT] ?: "as_dialed",
            handoffStrategy = preferences[PreferencesKeys.HANDOFF_STRATEGY] ?: "dial_intent",
            routeAllOutgoingCalls = preferences[PreferencesKeys.ROUTE_ALL_CALLS] ?: false,
            hasSeenPaidCallWarning = preferences[PreferencesKeys.HAS_SEEN_PAID_WARNING] ?: false,
            targetPackageName = preferences[PreferencesKeys.TARGET_PACKAGE] ?: "com.brilliant.connect.com.bd",
            startOnBoot = preferences[PreferencesKeys.START_ON_BOOT] ?: true
        )
    }

    suspend fun setAutoSelect(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_SELECT] = enabled }
    }

    suspend fun setTargetOptionLabel(label: String) {
        context.dataStore.edit { it[PreferencesKeys.TARGET_LABEL] = label }
    }

    suspend fun setAutoPressCallButton(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_PRESS_CALL] = enabled }
    }

    suspend fun setConfirmBeforeCalling(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CONFIRM_BEFORE_CALL] = enabled }
    }

    suspend fun setNumberFormat(format: String) {
        context.dataStore.edit { it[PreferencesKeys.NUMBER_FORMAT] = format }
    }

    suspend fun setHandoffStrategy(strategy: String) {
        context.dataStore.edit { it[PreferencesKeys.HANDOFF_STRATEGY] = strategy }
    }

    suspend fun setRouteAllOutgoingCalls(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ROUTE_ALL_CALLS] = enabled }
    }

    suspend fun setHasSeenPaidCallWarning(seen: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAS_SEEN_PAID_WARNING] = seen }
    }

    suspend fun setTargetPackageName(pkg: String) {
        context.dataStore.edit { it[PreferencesKeys.TARGET_PACKAGE] = pkg }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.START_ON_BOOT] = enabled }
    }
}
