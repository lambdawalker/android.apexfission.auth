package com.apexfission.android.auth.capture

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kotlin.reflect.KProperty1
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

/**
 * Component of the identity review screen containing the editable form fields for card details.
 */
@Composable internal fun CardReviewForm(
    ui: CaptureUi,
    onEdit: ((CardData) -> CardData) -> Unit,
    onReset: () -> Unit,
    onOrigin: (String) -> Unit,
    onSave: () -> Unit,
    onFocus: (KProperty1<CardData, String>, Boolean) -> Unit = { _, _ -> },
    onRescan: (() -> Unit)? = null,
) {
    val data = ui.draft ?: return
    Text(stringResource(R.string.auth_review_id_details), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
    ReviewSection(null) {
        Text(stringResource(R.string.auth_check_each_value_against_your_card_and_correct_any_mistakes_leave), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (ui.record?.reviewed != null && !ui.reviewDirty) Text(stringResource(R.string.auth_your_reviewed_details_are_saved), style = MaterialTheme.typography.titleMedium)
    val kind = DocumentKind.forValue(data.documentType)
    val primaryAddress = kind != DocumentKind.PASSPORT
    val primaryIdentifier = kind == DocumentKind.NATIONAL_ID || kind == DocumentKind.VOTER_ID
    ReviewSection(stringResource(R.string.auth_personal_information)) {
        CardField(stringResource(R.string.auth_full_legal_name), data.fullName, ui.busy, focus = { onFocus(CardData::fullName, it) }) { text -> onEdit { it.copy(fullName = text) } }
        CardField(stringResource(R.string.auth_date_of_birth), data.birthDate, ui.busy, focus = { onFocus(CardData::birthDate, it) }) { text -> onEdit { it.copy(birthDate = text) } }
        if (primaryAddress) CardField(stringResource(R.string.auth_residential_address), data.address, ui.busy, multiline = true, focus = { onFocus(CardData::address, it) }) { text -> onEdit { it.copy(address = text) } }
    }
    ReviewSection(stringResource(R.string.auth_document_information)) {
        DocumentTypeField(data.documentType, !ui.busy) { text -> onEdit {
            val customName = it.documentName.ifBlank { it.documentType.takeIf { value -> value.isNotBlank() && DocumentKind.entries.none { preset -> preset.wireValue == value } }.orEmpty() }
            it.copy(documentType = text, documentName = customName)
        } }
        if (kind == DocumentKind.OTHER || data.documentName.isNotBlank()) CardField(stringResource(R.string.auth_document_name), data.documentName, ui.busy, focus = { onFocus(CardData::documentName, it) }) { text -> onEdit { it.copy(documentName = text) } }
        CardField(stringResource(R.string.auth_document_number), data.documentNumber, ui.busy, focus = { onFocus(CardData::documentNumber, it) }) { text -> onEdit { it.copy(documentNumber = text) } }
        if (primaryIdentifier) CardField(stringResource(R.string.auth_personal_identifier), data.personalNumber, ui.busy, focus = { onFocus(CardData::personalNumber, it) }) { text -> onEdit { it.copy(personalNumber = text) } }
        IssuingJurisdictionField(IssuingJurisdiction(data.issuingCountry, data.issuingRegion, data.issuingCity), !ui.busy) { location ->
            onEdit { it.copy(issuingCountry = location.country, issuingRegion = location.region, issuingCity = location.city) }
        }
        CardField(stringResource(R.string.auth_expiry_date), data.expiryDate, ui.busy, focus = { onFocus(CardData::expiryDate, it) }) { text -> onEdit { it.copy(expiryDate = text) } }
    }
    var additionalOverride by remember { mutableStateOf<Boolean?>(null) }
    val additional = additionalOverride ?: listOf(data.givenNames, data.surnames, data.issueDate, data.issuingAuthority,
        if (primaryAddress) "" else data.address, if (primaryIdentifier) "" else data.personalNumber).any(String::isNotBlank)
    ReviewSection(stringResource(R.string.auth_additional_details)) {
        TextButton(onClick = { additionalOverride = !additional }) { Text(stringResource(if (additional) R.string.auth_hide_additional else R.string.auth_show_additional)) }
        if (additional) {
            CardField(stringResource(R.string.auth_given_names), data.givenNames, ui.busy, focus = { onFocus(CardData::givenNames, it) }) { text -> onEdit { it.copy(givenNames = text) } }
            CardField(stringResource(R.string.auth_surnames), data.surnames, ui.busy, focus = { onFocus(CardData::surnames, it) }) { text -> onEdit { it.copy(surnames = text) } }
            if (!primaryIdentifier) CardField(stringResource(R.string.auth_personal_identifier), data.personalNumber, ui.busy, focus = { onFocus(CardData::personalNumber, it) }) { text -> onEdit { it.copy(personalNumber = text) } }
            if (!primaryAddress) CardField(stringResource(R.string.auth_residential_address), data.address, ui.busy, multiline = true, focus = { onFocus(CardData::address, it) }) { text -> onEdit { it.copy(address = text) } }
            CardField(stringResource(R.string.auth_issue_date), data.issueDate, ui.busy, focus = { onFocus(CardData::issueDate, it) }) { text -> onEdit { it.copy(issueDate = text) } }
            CardField(stringResource(R.string.auth_issuing_authority), data.issuingAuthority, ui.busy, focus = { onFocus(CardData::issuingAuthority, it) }) { text -> onEdit { it.copy(issuingAuthority = text) } }
        }
    }
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
    Button(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, onClick = onSave, enabled = !ui.busy && data.isValid() && !data.isEmpty() && ui.originCountry in countries.keys) { Text(stringResource(R.string.auth_save_and_continue)) }
    if (onRescan != null) OutlinedButton(onClick = onRescan, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.auth_rescan_document))
    }
    if (ui.reviewDirty) {
        Text(stringResource(R.string.auth_unsaved_edits_are_kept_only_while_this_screen_is_open), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(shape = MaterialTheme.shapes.small, onClick = onReset, enabled = !ui.busy) { Text(stringResource(R.string.auth_discard_edits_and_use_latest_details)) }
    }
}

/**
 * Component of the review form representing a single input field.
 */
@Composable private fun CardField(label: String, value: String, busy: Boolean, multiline: Boolean = false, focus: (Boolean) -> Unit = {}, change: (String) -> Unit) {
    androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = value, onValueChange = change, enabled = !busy,
            trailingIcon = { Icon(androidx.compose.material.icons.Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focus(it.isFocused) }.semantics { contentDescription = label },
            singleLine = !multiline, maxLines = if (multiline) 4 else 1)
    }
}

@Preview
@Composable
private fun CardReviewFormPreview() {
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
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
    com.apexfission.android.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                CardField("Full name", "Sample Person", busy = false, change = {})
            }
        }
    }
}

@Composable
private fun ReviewSection(title: String?, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            content()
        }
    }
}
