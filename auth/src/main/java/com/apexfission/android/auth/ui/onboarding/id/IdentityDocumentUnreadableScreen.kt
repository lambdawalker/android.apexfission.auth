package com.apexfission.android.auth.ui.onboarding.id

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that prompts the user to rescan or cancel if their ID document could not be read.
 */
@Composable
fun IdentityDocumentUnreadableScreen(onBack: () -> Unit, onRetake: () -> Unit, onDashboard: () -> Unit) {
    OnboardingFrame(
        title = stringResource(R.string.auth_document_images_need_another_try), icon = Icons.Default.PhotoCamera,
        step = stringResource(R.string.auth_id_check), onBack = onBack,
        primaryLabel = stringResource(R.string.auth_try_again), onPrimary = onRetake,
        secondaryLabel = stringResource(R.string.auth_go_to_dashboard), onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_we_couldn_t_read_the_document_images_clearly_retake_them_with_goo))
            BodyText(stringResource(R.string.auth_check_for_glare_missing_corners_and_blurry_text_the_capture_provi))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityDocumentUnreadableScreenPreview() {
    AttestraAuthTheme {
        IdentityDocumentUnreadableScreen({}, {}, {})
    }
}
