package com.apexfission.android.auth.capture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apexfission.android.auth.library.R
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext

/** Changes remain local to the sheet until explicitly applied. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IssuingJurisdictionField(
    value: IssuingJurisdiction,
    enabled: Boolean = true,
    onChange: (IssuingJurisdiction) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedCard(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.auth_issued_in), style = MaterialTheme.typography.labelMedium)
                Text(value.display().ifBlank { stringResource(R.string.auth_select_issuing_location) }, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (open) ModalBottomSheet(onDismissRequest = { open = false }) {
        IssuingJurisdictionEditor(value, enabled, onApply = { onChange(it); open = false }, onDismiss = { open = false })
    }
}

@Composable
internal fun IssuingJurisdictionEditor(value: IssuingJurisdiction, enabled: Boolean, onApply: (IssuingJurisdiction) -> Unit, onDismiss: () -> Unit) {
    // Snapshot once per open editor: late OCR must not replace uncommitted user edits.
    val original = remember { value }
    var draft by remember { mutableStateOf(original) }
    val context = LocalContext.current.applicationContext
    val catalogue by produceState<JurisdictionCatalogue?>(null, context) {
        this.value = withContext(Dispatchers.IO) {
            // Catalogue failure must not prevent manual document correction.
            runCatching { context.assets.open("issuing-locations.tsv").bufferedReader().use { JurisdictionCatalogue.read(it.lineSequence()) } }.getOrNull()
        }
    }
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.auth_issuing_jurisdiction), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.auth_jurisdiction_help), style = MaterialTheme.typography.bodyMedium)
            val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
            val countries = remember(locale) { Locale.getISOCountries().map { code -> Locale.Builder().setRegion(code).build().getDisplayCountry(locale) }.sorted() }
            JurisdictionSelector(stringResource(R.string.auth_issuing_country), draft.country, countries, enabled) { draft = draft.withCountry(it) }
            val regions = remember(catalogue, draft.country, locale) { catalogue?.regions(draft.country, locale).orEmpty() }
            val cities = remember(catalogue, draft.country, draft.region, locale) { catalogue?.cities(draft.country, draft.region, locale).orEmpty() }
            JurisdictionSelector(stringResource(R.string.auth_issuing_region), draft.region, (regions + listOf(original.region).filter { it.isNotBlank() && draft.country == original.country }).distinct(), enabled) { draft = draft.withRegion(it) }
            JurisdictionSelector(stringResource(R.string.auth_issuing_city), draft.city, (cities + listOf(original.city).filter { it.isNotBlank() && draft.country == original.country && draft.region == original.region }).distinct(), enabled) { draft = draft.copy(city = it) }
            Text("GeoNames · CC BY 4.0 · geonames.org", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.auth_jurisdiction_parent_change), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onApply(draft) }, enabled = enabled && listOf(draft.country, draft.region, draft.city).all { it.toByteArray().size <= 512 && '\r' !in it && '\u0000' !in it } && (draft.country.isNotBlank() || (draft.region.isBlank() && draft.city.isBlank())), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_use_location))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.auth_close_location)) }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JurisdictionSelector(label: String, value: String, suggestions: List<String>, enabled: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val matches = suggestions.filter { value.isBlank() || JurisdictionCatalogue.normalized(it).contains(JurisdictionCatalogue.normalized(value)) }.take(12)
    ExposedDropdownMenuBox(expanded = expanded && matches.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(value, { onChange(it); expanded = true }, label = { Text(label) }, enabled = enabled, singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        ExposedDropdownMenu(expanded = expanded && matches.isNotEmpty(), onDismissRequest = { expanded = false }) {
            matches.forEach { suggestion -> DropdownMenuItem(text = { Text(suggestion) }, onClick = { onChange(suggestion); expanded = false }) }
        }
    }
}
