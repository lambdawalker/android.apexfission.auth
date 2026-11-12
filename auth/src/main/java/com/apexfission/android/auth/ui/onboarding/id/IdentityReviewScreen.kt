package com.apexfission.android.auth.ui.onboarding.id

import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.capture.DocumentTypeField
import com.apexfission.android.auth.capture.IssuingJurisdiction
import com.apexfission.android.auth.capture.IssuingJurisdictionField
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@kotlinx.serialization.Serializable
data class IdentityDetails(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val documentNumber: String = "",
    val issuingCountry: String = "",
    val documentType: String = "",
    val expirationDate: String = "",
    val issuingRegion: String = "",
    val issuingCity: String = "",
    val documentName: String = "",
    val issuingAuthority: String = "",
)

/**
 * Screen that shows the extracted ID details for the user to confirm before submission.
 */
@Composable
fun IdentityReviewScreen(
    initialDetails: IdentityDetails, onBack: () -> Unit,
    onSubmit: (IdentityDetails) -> Unit, onRescan: () -> Unit,
    onDetailsChanged: (IdentityDetails) -> Unit = {},
    extractedDetails: IdentityDetails = initialDetails,
    error: String? = null,
) {
    var fullName by remember(initialDetails) { mutableStateOf(initialDetails.fullName) }
    var dateOfBirth by remember(initialDetails) { mutableStateOf(initialDetails.dateOfBirth) }
    var address by remember(initialDetails) { mutableStateOf(initialDetails.address) }
    var documentNumber by remember(initialDetails) { mutableStateOf(initialDetails.documentNumber) }
    var documentType by remember(initialDetails) { mutableStateOf(initialDetails.documentType) }
    var expirationDate by remember(initialDetails) { mutableStateOf(initialDetails.expirationDate) }

    var location by remember(initialDetails) { mutableStateOf(IssuingJurisdiction(initialDetails.issuingCountry, initialDetails.issuingRegion, initialDetails.issuingCity)) }
    var documentName by remember(initialDetails) { mutableStateOf(initialDetails.documentName) }
    var issuingAuthority by remember(initialDetails) { mutableStateOf(initialDetails.issuingAuthority) }

    fun notifyChanged() = onDetailsChanged(IdentityDetails(fullName, dateOfBirth, address, documentNumber, location.country, documentType, expirationDate, location.region, location.city, documentName, issuingAuthority))

    OnboardingFrame(
        title = stringResource(R.string.auth_review_id_details), icon = Icons.Default.Badge, step = stringResource(R.string.auth_id_check), onBack = onBack,
        primaryLabel = stringResource(R.string.auth_submit_details_for_review),
        onPrimary = {
            onSubmit(
                IdentityDetails(fullName, dateOfBirth, address, documentNumber, location.country, documentType, expirationDate, location.region, location.city, documentName, issuingAuthority)
            )
        },
        secondaryLabel = stringResource(R.string.auth_rescan_document), onSecondary = onRescan,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_review_the_details_read_from_your_document_correct_mistakes_befor))
            if (error != null) Text(authText(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (initialDetails != extractedDetails) BodyText(stringResource(R.string.auth_your_edits_will_be_submitted_as_corrections_the_original_extracti))
            Text(stringResource(R.string.auth_personal_information), style = MaterialTheme.typography.titleSmall)
            DetailField(stringResource(R.string.auth_full_legal_name), fullName) { fullName = it; notifyChanged() }
            DetailField(stringResource(R.string.auth_date_of_birth), dateOfBirth) { dateOfBirth = it; notifyChanged() }
            DetailField(stringResource(R.string.auth_residential_address), address) { address = it; notifyChanged() }
            Text(stringResource(R.string.auth_document_information), style = MaterialTheme.typography.titleSmall)
            DetailField(stringResource(R.string.auth_document_number), documentNumber) { documentNumber = it; notifyChanged() }
            DocumentTypeField(documentType) { selected ->
                if (documentName.isBlank() && documentType.isNotBlank() && com.apexfission.android.auth.capture.DocumentKind.entries.none { it.wireValue == documentType }) documentName = documentType
                documentType = selected; notifyChanged()
            }
            if (com.apexfission.android.auth.capture.DocumentKind.forValue(documentType) == com.apexfission.android.auth.capture.DocumentKind.OTHER || documentName.isNotBlank()) {
                DetailField(stringResource(R.string.auth_document_name), documentName) { documentName = it; notifyChanged() }
            }
            IssuingJurisdictionField(location) { location = it; notifyChanged() }
            DetailField(stringResource(R.string.auth_issuing_authority), issuingAuthority) { issuingAuthority = it; notifyChanged() }
            DetailField(stringResource(R.string.auth_expiration_date), expirationDate) { expirationDate = it; notifyChanged() }
            BodyText(stringResource(R.string.auth_correcting_extracted_text_does_not_verify_your_identity_a_separat))
        }
    }
}

/**
 * Component used in IdentityReviewScreen to represent an editable detail field.
 */
@Composable
private fun DetailField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = label != stringResource(R.string.auth_residential_address),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityReviewScreenPreview() {
    AttestraAuthTheme {
        IdentityReviewScreen(IdentityDetails(), {}, {}, {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "DetailField", showBackground = true)
@Composable
private fun DetailFieldPreview() {
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                DetailField("Residential address", "123 Sample Street", {})
            }
        }
    }
}
