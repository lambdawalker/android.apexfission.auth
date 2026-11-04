package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier

/**
 * Component of the identity review screen containing the editable form fields for card details.
 */
@Composable internal fun CardReviewForm(
    ui: CaptureUi,
    onEdit: (CardData) -> Unit,
    onReset: () -> Unit,
    onOrigin: (String) -> Unit,
    onSave: () -> Unit,
) {
    val data = ui.draft ?: return
    Text(stringResource(R.string.auth_review_your_card_details), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    Text(stringResource(R.string.auth_check_each_value_against_your_card_and_correct_any_mistakes_leave), color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (ui.record?.reviewed != null && !ui.reviewDirty) Text(stringResource(R.string.auth_your_reviewed_details_are_saved), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    CardField(stringResource(R.string.auth_full_name), data.fullName, ui.busy) { onEdit(data.copy(fullName = it)) }
    CardField(stringResource(R.string.auth_given_names), data.givenNames, ui.busy) { onEdit(data.copy(givenNames = it)) }
    CardField(stringResource(R.string.auth_surnames), data.surnames, ui.busy) { onEdit(data.copy(surnames = it)) }
    CardField(stringResource(R.string.auth_date_of_birth), data.birthDate, ui.busy) { onEdit(data.copy(birthDate = it)) }
    CardField(stringResource(R.string.auth_document_number), data.documentNumber, ui.busy) { onEdit(data.copy(documentNumber = it)) }
    CardField(stringResource(R.string.auth_personal_number_e_g_curp), data.personalNumber, ui.busy) { onEdit(data.copy(personalNumber = it)) }
    CardField(stringResource(R.string.auth_address), data.address, ui.busy, multiline = true) { onEdit(data.copy(address = it)) }
    CardField(stringResource(R.string.auth_issue_date), data.issueDate, ui.busy) { onEdit(data.copy(issueDate = it)) }
    CardField(stringResource(R.string.auth_expiry_date), data.expiryDate, ui.busy) { onEdit(data.copy(expiryDate = it)) }
    CardField(stringResource(R.string.auth_issuing_country), data.issuingCountry, ui.busy) { onEdit(data.copy(issuingCountry = it)) }
    var countriesOpen by remember { mutableStateOf(false) }
    val countries = mapOf("CA" to stringResource(R.string.auth_canada), "US" to stringResource(R.string.auth_united_states), "MX" to stringResource(R.string.auth_mexico), "GT" to stringResource(R.string.auth_guatemala), "BZ" to stringResource(R.string.auth_belize))
    Text(stringResource(R.string.auth_country_of_origin), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Text(stringResource(R.string.auth_select_your_country_of_origin_it_may_differ_from_the_country_that), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box {
        OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { countriesOpen = true }, enabled = !ui.busy) { Text(countries[ui.originCountry] ?: stringResource(R.string.auth_select_country)) }
        DropdownMenu(expanded = countriesOpen, onDismissRequest = { countriesOpen = false }) {
            countries.forEach { (code, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onOrigin(code); countriesOpen = false }) }
        }
    }
    if (!data.isValid()) Text(stringResource(R.string.auth_a_field_is_too_long_shorten_it_before_saving), color = MaterialTheme.colorScheme.error)
    Button(shape = MaterialTheme.shapes.small, onClick = onSave, enabled = !ui.busy && data.isValid() && !data.isEmpty() && ui.originCountry in countries.keys) { Text(stringResource(R.string.auth_save_and_continue)) }
    if (ui.reviewDirty) {
        Text(stringResource(R.string.auth_unsaved_edits_are_kept_only_while_this_screen_is_open), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(shape = MaterialTheme.shapes.small, onClick = onReset, enabled = !ui.busy) { Text(stringResource(R.string.auth_discard_edits_and_use_latest_details)) }
    }
}

/**
 * Component of the review form representing a single input field.
 */
@Composable private fun CardField(label: String, value: String, busy: Boolean, multiline: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, enabled = !busy,
        modifier = Modifier.fillMaxWidth(), singleLine = !multiline, maxLines = if (multiline) 4 else 1)
}

@Preview
@Composable
private fun CardReviewFormPreview() {
    com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.foundation.layout.Column {
        CardReviewForm(
            ui = CaptureUi(draft = CardData(fullName = "Sample Person"), originCountry = "MX"),
            onEdit = {},
            onReset = {},
            onOrigin = {},
            onSave = {}
        )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "CardField", showBackground = true)
@Composable
private fun CardFieldPreview() {
    com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                CardField("Full name", "Sample Person", busy = false, change = {})
            }
        }
    }
}
