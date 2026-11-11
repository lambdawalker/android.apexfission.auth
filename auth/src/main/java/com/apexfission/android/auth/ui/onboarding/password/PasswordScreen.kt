package com.apexfission.android.auth.ui.onboarding.password

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.apexfission.android.auth.discovery.PasswordPolicy
import com.apexfission.android.auth.ui.onboarding.common.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Component providing an outlined text field configured for password input with policy validation feedback.
 */
@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, policy: PasswordPolicy? = null, optional: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onChange,
        label = { Text(if (optional) stringResource(R.string.auth_password_optional) else stringResource(R.string.auth_password)) },
        singleLine = true, visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
    if (policy != null) {
        policy.requirements().forEach { BodyText(it) }
        if (value.isNotEmpty()) {
            BodyText(stringResource(R.string.auth_strength_estimate_1_s, authText(policy.strength(value))))
            BodyText(if (policy.problems(value).isEmpty()) stringResource(R.string.auth_meets_the_configured_requirements) else stringResource(R.string.auth_does_not_yet_meet_the_configured_requirements))
            policy.problems(value).forEach { BodyText(it) }
        }
    }
}

/**
 * Screen used for both entering an existing password or setting a new one during onboarding.
 */
@Composable
fun PasswordScreen(value: String, onChange: (String) -> Unit, onSubmit: () -> Unit,
    onBack: () -> Unit, error: String?, policy: PasswordPolicy? = null,
    optional: Boolean = false, onSkip: () -> Unit = {}, signIn: Boolean = false) {
    OnboardingFrame(title = if (signIn) stringResource(R.string.auth_sign_in_with_password) else stringResource(R.string.auth_set_your_password), icon = Icons.Default.Lock,
        step = stringResource(R.string.auth_account), onBack = onBack,
        primaryLabel = if (signIn) stringResource(R.string.auth_sign_in) else stringResource(R.string.auth_save_password),
        primaryEnabled = value.isNotEmpty() && (policy == null || policy.problems(value).isEmpty()), onPrimary = onSubmit,
        secondaryLabel = if (optional) stringResource(R.string.auth_skip_for_now) else null, onSecondary = onSkip) {
        OnboardingCard {
            PasswordField(value, onChange, policy)
            if (error != null) BodyText(error)
        }
    }
}

@Preview
@Composable
private fun PasswordScreenPreview() {
    AttestraAuthTheme {
        PasswordScreen(
            value = "",
            onChange = {},
            onSubmit = {},
            onBack = {},
            error = null
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "PasswordField", showBackground = true)
@Composable
private fun PasswordFieldPreview() {
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                PasswordField(value = "SamplePassword42!", onChange = {}, optional = true)
            }
        }
    }
}
