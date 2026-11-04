package com.apexfission.android.attestra.auth.ui.onboarding.common

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Component providing the standard layout frame and navigation bar for onboarding screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFrame(
    title: String,
    icon: ImageVector,
    step: String,
    onBack: () -> Unit,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    secondaryEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Attestra", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.auth_back), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Text(
                        authText(step),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 20.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (primaryLabel != null || secondaryLabel != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (primaryLabel != null) {
                            Button(
                                onClick = onPrimary,
                                enabled = primaryEnabled,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                            ) { Text(authText(primaryLabel)) }
                        }
                        if (secondaryLabel != null) {
                            OutlinedButton(
                                onClick = onSecondary,
                                enabled = secondaryEnabled,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                            ) { Text(authText(secondaryLabel)) }
                        }
                    }
                }
            }
        }
    ) { inset ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(authText(title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                content()
            }
        }
    }
}

/**
 * Component providing a standard styled card for onboarding content.
 */
@Composable
fun OnboardingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

/**
 * Component providing a standard styled text for body paragraphs in onboarding screens.
 */
@Composable
fun BodyText(text: String) {
    Text(authText(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * Component providing a row to display the status of a specific item or requirement.
 */
@Composable
fun StatusRow(label: String, detail: String, complete: Boolean = true) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (complete) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                tint = if (complete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

/**
 * Component providing a styled circular progress indicator and description text.
 */
@Composable
fun WaitingIndicator(description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        BodyText(description)
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingFramePreview() {
    AttestraAuthTheme {
        OnboardingFrame(
            title = "Test Frame",
            icon = Icons.Default.Shield,
            step = "1/1",
            onBack = {},
            primaryLabel = "Continue",
            secondaryLabel = "Cancel"
        ) {
            OnboardingCard {
                BodyText("This is body text inside an onboarding card.")
                StatusRow("Feature", "Detail text", complete = true)
                WaitingIndicator("Please wait...")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "OnboardingCard", showBackground = true)
@Composable
private fun OnboardingCardPreview() {
    com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                OnboardingCard { BodyText("Review your account details.") }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "BodyText", showBackground = true)
@Composable
private fun BodyTextPreview() {
    com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                BodyText("Your progress is saved on this device.")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "StatusRow", showBackground = true)
@Composable
private fun StatusRowPreview() {
    com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                StatusRow("Email confirmed", "Continue with your account", complete = true)
                StatusRow("ID capture", "Waiting for your document", complete = false)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "WaitingIndicator", showBackground = true)
@Composable
private fun WaitingIndicatorPreview() {
    com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                WaitingIndicator("Preparing your document…")
            }
        }
    }
}
