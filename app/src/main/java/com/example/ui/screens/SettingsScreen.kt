package com.example.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.BridgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BridgeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bridge Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_scroll_container"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Accessibility Automation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SettingToggleRow(
                            title = "Auto-select Brilliant-OUT",
                            subtitle = "Automatically select the target row from the bottom sheet after pressing call.",
                            checked = settings.autoSelectBrilliantOut,
                            onCheckedChange = { viewModel.updateAutoSelect(it) },
                            tag = "toggle_auto_select"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = settings.targetOptionLabel,
                            onValueChange = { viewModel.updateTargetLabel(it) },
                            label = { Text("Sheet Target Label") },
                            placeholder = { Text("Brilliant-OUT") },
                            supportingText = { Text("Must match the exact text inside Brilliant Connect's bottom sheet.") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_target_label")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingToggleRow(
                            title = "Auto-press Call Button",
                            subtitle = "Automatically taps the green call button once digits match. If disabled, stops at the keypad.",
                            checked = settings.autoPressCallButton,
                            onCheckedChange = { viewModel.updateAutoPressCallButton(it) },
                            tag = "toggle_auto_press"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingToggleRow(
                            title = "Confirm Before Calling",
                            subtitle = "Shows a high-priority prompt requiring you to confirm before placing paid Brilliant-OUT calls.",
                            checked = settings.confirmBeforeCalling,
                            onCheckedChange = { viewModel.updateConfirmBeforeCall(it) },
                            tag = "toggle_confirm_before_call"
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Number Formatting",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Controls how dialed numbers are delivered to the Brilliant Connect keypad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FormatOptionRow(
                            label = "As Dialed (Recommended)",
                            description = "Passes the dialed string directly. Brilliant Connect handles local conversion.",
                            selected = settings.numberFormat == "as_dialed",
                            onSelect = { viewModel.updateNumberFormat("as_dialed") }
                        )

                        FormatOptionRow(
                            label = "International E.164 (+8801...)",
                            description = "Converts Bangladesh numbers to international standard with +88 prefix.",
                            selected = settings.numberFormat == "e164",
                            onSelect = { viewModel.updateNumberFormat("e164") }
                        )

                        FormatOptionRow(
                            label = "Local Format (01...)",
                            description = "Strips international prefix down to local 11-digit Bangladesh subscriber format.",
                            selected = settings.numberFormat == "local",
                            onSelect = { viewModel.updateNumberFormat("local") }
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Handoff Launch Strategy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StrategyOptionRow(
                            name = "ACTION_DIAL (Recommended)",
                            description = "Sends Intent(ACTION_DIAL, tel:<number>) directly to Brilliant Connect.",
                            selected = settings.handoffStrategy == "dial_intent",
                            onSelect = { viewModel.updateHandoffStrategy("dial_intent") }
                        )

                        StrategyOptionRow(
                            name = "ACTION_VIEW",
                            description = "Sends Intent(ACTION_VIEW, tel:<number>) as an alternate intent resolver.",
                            selected = settings.handoffStrategy == "view_intent",
                            onSelect = { viewModel.updateHandoffStrategy("view_intent") }
                        )

                        StrategyOptionRow(
                            name = "Launcher + Clipboard Copy",
                            description = "Copies number to clipboard and opens Brilliant Connect's home activity.",
                            selected = settings.handoffStrategy == "launcher_copy",
                            onSelect = { viewModel.updateHandoffStrategy("launcher_copy") }
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Optional Fallback: Call Redirection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If your device's default dialer (e.g. certain Samsung, Xiaomi, or Oppo ROMs) does not display third-party calling accounts in the dialer, you can enable Call Redirection. This intercepts cellular calls and reroutes them to Brilliant Connect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SettingToggleRow(
                            title = "Route ALL Outgoing Calls via Brilliant",
                            subtitle = "Cancels normal SIM calls and hands them over to Brilliant Connect automatically.",
                            checked = settings.routeAllOutgoingCalls,
                            onCheckedChange = { isChecked ->
                                viewModel.updateRouteAllCalls(isChecked)
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                                    if (rm != null && !rm.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)) {
                                        val roleIntent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
                                        roleLauncher.launch(roleIntent)
                                    }
                                }
                            },
                            tag = "toggle_route_all"
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            title = "Register on Boot",
                            subtitle = "Re-registers the PhoneAccount with Android Telecom after device restart or app update.",
                            checked = settings.startOnBoot,
                            onCheckedChange = { viewModel.updateStartOnBoot(it) },
                            tag = "toggle_boot"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun FormatOptionRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StrategyOptionRow(
    name: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
