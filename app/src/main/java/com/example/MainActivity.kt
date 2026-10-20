package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.BridgeViewModel
import com.example.ui.screens.ChecklistScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestScreen
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.BrilliantBridgeTheme
import com.example.ui.theme.CyanPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: BridgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BrilliantBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BridgeAppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }
}

object NavRoutes {
    const val HOME = "home"
    const val CHECKLIST = "checklist"
    const val TEST = "test"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
fun BridgeAppNavigation(viewModel: BridgeViewModel) {
    val navController = rememberNavController()
    val showPaidWarning by viewModel.showPaidWarningDialog.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChecklist = { navController.navigate(NavRoutes.CHECKLIST) },
                onNavigateToTest = { navController.navigate(NavRoutes.TEST) },
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateToDiagnostics = { navController.navigate(NavRoutes.DIAGNOSTICS) }
            )
        }

        composable(NavRoutes.CHECKLIST) {
            ChecklistScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.TEST) {
            TestScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.DIAGNOSTICS) {
            DiagnosticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    if (showPaidWarning) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentWarning
                )
            },
            title = {
                Text(text = "Paid Calling Service Notice")
            },
            text = {
                Text(
                    text = "Brilliant-OUT is a paid call service provided by Brilliant Connect that connects to cellular and landline phone numbers.\n\n" +
                            "This bridge app will automatically open Brilliant Connect, verify dialed digits, tap the call button, and select 'Brilliant-OUT' on your behalf.\n\n" +
                            "Standard calling charges from your Brilliant Connect account balance apply."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.markPaidWarningAcknowledged() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.testTag("dialog_confirm_paid_button")
                ) {
                    Text("I Understand & Accept", color = Color.Black)
                }
            }
        )
    }
}
