package com.apexfission.android.auth.ui.onboarding.email

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Legacy catalog/preview entry for a code-only scenario. Live onboarding now
 * uses EmailWaitScreen for both waiting and code entry.
 */
@Composable
fun EmailCodeScreen(
    email: String,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    error: String? = null,
    attemptsRemaining: Int? = null,
    canResend: Boolean = true,
) {
    var code by remember(email) { mutableStateOf("") }
    EmailWaitScreen(
        email = email,
        code = code,
        onCodeChange = { code = it },
        onVerify = { onVerify(code) },
        onBack = onBack,
        onResend = onResend,
        onChangeEmail = onBack,
        error = error,
        attemptsRemaining = attemptsRemaining,
        canResend = canResend,
        autoFocus = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailCodeScreenPreview() {
    AttestraAuthTheme {
        EmailCodeScreen("name@example.invalid", {}, {}, {})
    }
}
