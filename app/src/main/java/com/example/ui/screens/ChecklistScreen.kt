package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.handoff.HandoffLauncher
import com.example.telecom.BridgePhoneAccountManager
import com.example.ui.BridgeViewModel
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.CyanPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    viewModel: BridgeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val setupStatus by viewModel.setupStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshStatus()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Setup Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${8 - setupStatus.remainingCount} of 8 Requirements Met",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("checklist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshStatus() },
                        modifier = Modifier.testTag("checklist_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
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
                .testTag("checklist_scroll_container"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Android security requires manual user approval for Telecom accounts and Accessibility services. Complete each item below once.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ChecklistItemCard(
                    stepNumber = 1,
                    title = "Phone & Calling Permissions",
                    description = "Required to verify available calling accounts and manage phone state transitions during call routing.",
                    isComplete = setupStatus.phonePermissionsGranted,
                    actionText = "Grant Permissions",
                    onAction = {
                        phonePermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.CALL_PHONE
                            )
                        )
                    }
                )
            }

            item {
                ChecklistItemCard(
                    stepNumber = 2,
                    title = "Notifications Permission",
                    description = "Required to display high-priority call launch prompts, call confirmation dialogs, and error alerts.",
                    isComplete = setupStatus.notificationPermissionGranted,
                    actionText = "Allow Notifications",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.refreshStatus()
                        }
                    }
                )
            }

            item {
                ChecklistItemCard(
                    stepNumber = 3,
                    title = "Display Over Other Apps",
                    description = "Crucial for Android 10+. Lifts background start restrictions so Brilliant Connect opens instantly when you dial.",
                    isComplete = setupStatus.overlayPermissionGranted,
                    actionText = "Grant Overlay Access",
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            openAppSettings(context)
                        }
                    }
                )
            }

            item {
                ChecklistItemCard(
                    stepNumber = 4,
                    title = "Full-Screen Call Prompts",
                    description = "Allows high-priority incoming handoff dialogs when the screen is locked or in full-screen applications.",
                    isComplete = setupStatus.fullScreenIntentGranted,
                    actionText = "Allow Full Screen",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:${context.packageName}")
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                openAppSettings(context)
                            }
                        } else {
                            viewModel.refreshStatus()
                        }
                    }
                )
            }

            item {
                ChecklistItemCard(
                    stepNumber = 5,
                    title = "Ignore Battery Optimizations",
                    description = "Prevents OEM battery killers from terminating the telecom connection service while dialing.",
                    isComplete = setupStatus.batteryOptimizationIgnored,
                    actionText = "Exempt from Battery Saver",
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            openAppSettings(context)
                        }
                    }
                )
            }

            item {
                ChecklistItemCard(
                    stepNumber = 6,
                    title = "Confirm Brilliant Connect Installed",
                    description = "Target calling app (com.brilliant.connect.com.bd) must be installed to handle outbound VoIP calls.",
                    isComplete = setupStatus.targetAppInstalled,
                    actionText = "Install Brilliant Connect",
                    onAction = {
                        HandoffLauncher.openPlayStore(context, settings.targetPackageName)
                    }
                )
            }

            item {
                ChecklistItemCard(
                    stepNumber = 7,
                    title = "Enable Accessibility Automation",
                    description = "Automates pressing the green call button and selecting 'Brilliant-OUT' inside Brilliant Connect.",
                    isComplete = setupStatus.accessibilityServiceEnabled,
                    actionText = "Open Accessibility Settings",
                    onAction = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            openAppSettings(context)
                        }
                    },
                    secondaryActionText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Allow Restricted Settings" else null,
                    onSecondaryAction = {
                        openAppSettings(context)
                    },
                    note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        "Android 13+ Note: If the toggle is grayed out, tap 'Allow Restricted Settings' in App Info (three dots top-right), then return to toggle it on."
                    else null
                )
            }

            item {
                ChecklistItemCard(
                    stepNumber = 8,
                    title = "Enable 'Brilliant Connect' Calling Account",
                    description = "Activates Brilliant Connect as a selectable calling account option in Google Phone and OEM dialers.",
                    isComplete = setupStatus.callingAccountEnabled,
                    actionText = "Open Calling Accounts",
                    onAction = {
                        BridgePhoneAccountManager.openCallingAccountsSettings(context)
                    }
                )
            }
        }
    }
}

@Composable
fun ChecklistItemCard(
    stepNumber: Int,
    title: String,
    description: String,
    isComplete: Boolean,
    actionText: String,
    onAction: () -> Unit,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    note: String? = null
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checklist_step_$stepNumber"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isComplete) Color(0xFF0F201B) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isComplete) AccentSuccess else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isComplete) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "$stepNumber",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isComplete) Color(0xFF1B4332) else Color(0xFF3E2723)
                ) {
                    Text(
                        text = if (isComplete) "Ready" else "Action Needed",
                        color = if (isComplete) AccentSuccess else AccentWarning,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (note != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF1F2937),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (!isComplete) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAction,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fix_step_${stepNumber}_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text(actionText, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (secondaryActionText != null && onSecondaryAction != null) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            modifier = Modifier.testTag("secondary_step_${stepNumber}_button")
                        ) {
                            Text(secondaryActionText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallback = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(fallback)
    }
}
