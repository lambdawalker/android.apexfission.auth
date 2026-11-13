package com.apexfission.android.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.AppRegistration
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Widgets
import com.apexfission.android.democommon.CardTitle
import com.apexfission.android.democommon.DemoCard
import com.apexfission.android.democommon.DemoScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * Screen that acts as the main entry point for testing and launching the onboarding flows.
 */
@Composable
internal fun OnboardingEntryScreen(
    hasBackend: Boolean,
    resetting: Boolean,
    resetMessage: String?,
    onDeleteLocalUser: () -> Unit,
    onStart: () -> Unit,
    onCatalog: () -> Unit,
    onCardCapture: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    DemoScreen(
        title = "Attestra test menu",
        welcomeNote = "Choose a flow to test. Your saved progress stays on this device until you reset it.",
        eyebrow = "AUTHENTICATION",
    ) {
        item(key = "onboarding") {
            DemoCard {
                CardTitle(title = "Onboarding", icon = Icons.Outlined.AppRegistration)
                Text(
                    "In development · Start or resume email confirmation, passkey setup, and ID capture.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onStart,
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Open onboarding") }
                if (!hasBackend) Text(
                    "Environment settings are not ready. Open onboarding to check or retry. The UI catalog remains available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "ui_catalog") {
            DemoCard {
                CardTitle(title = "UI catalog", icon = Icons.Outlined.Widgets)
                Text(
                    "Preview authentication screens without calling the backend. The live ID capture entry requires a configured backend and sign-in.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onCatalog,
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Open UI catalog") }
            }
        }
        item(key = "local_card_capture") {
            DemoCard {
                CardTitle(title = "Card capture", icon = Icons.Outlined.PhotoCamera)
                Text(
                    "Try front/back capture, review, and retakes on this device. No sign-in or backend is needed. Images are discarded after confirmation or exit.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onCardCapture,
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Open card capture demo") }
            }
        }
        item(key = "account_actions") {
            DemoCard {
                CardTitle(title = "Account actions", icon = Icons.Outlined.DeleteOutline)
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text("Sign in · Not implemented")
                }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text("Log out · Not implemented")
                }
                Text(
                    "Local reset clears saved sign-in, onboarding progress, and identity test data. It does not delete a server account or a passkey in your credential manager.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !resetting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) { Text(if (resetting) "Deleting local data…" else "Delete local user") }
                resetMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
    if (confirmDelete && !resetting) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete local user?") },
        text = { Text("Clear this app’s saved session, email, onboarding progress, and identity test data? Your server account and credential-manager passkeys will remain.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteLocalUser() }, shape = MaterialTheme.shapes.small) { Text("Delete local user") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }, shape = MaterialTheme.shapes.small) { Text("Cancel") } },
    )
}

@Preview
@Composable
private fun OnboardingEntryScreenPreview() {
    MaterialTheme {
        OnboardingEntryScreen(
            hasBackend = true,
            resetting = false,
            resetMessage = null,
            onDeleteLocalUser = {},
            onStart = {},
            onCatalog = {},
            onCardCapture = {}
        )
    }
}

/**
 * Screen that displays the status of the environment configuration.
 */
@Composable
internal fun EnvironmentStatusScreen(loading: Boolean, message: String?, onRetry: () -> Unit, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (loading) "Loading environment…" else "Environment unavailable", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(message ?: "Retrieving the selected environment’s public configuration.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
            if (!loading) Button(onClick = onRetry) { Text("Retry") }
            OutlinedButton(onClick = onBack) { Text("Back to test menu") }
        }
    }
}

@Preview
@Composable
private fun EnvironmentStatusScreenPreview() {
    MaterialTheme {
        EnvironmentStatusScreen(
            loading = false,
            message = "Configuration loaded",
            onRetry = {},
            onBack = {}
        )
    }
}
