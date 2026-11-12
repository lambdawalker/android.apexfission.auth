package com.apexfission.android.auth.capture

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.apexfission.android.auth.library.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentTypeField(value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = DocumentKind.entries.firstOrNull { it.wireValue == value }
    ExposedDropdownMenuBox(expanded, { if (enabled) expanded = it }) {
        OutlinedTextField(value = selected?.let { stringResource(it.label) } ?: value.ifBlank { stringResource(R.string.auth_select_document_type) },
            onValueChange = {}, readOnly = true, enabled = enabled, label = { Text(stringResource(R.string.auth_document_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DocumentKind.entries.forEach { kind -> DropdownMenuItem(text = { Text(stringResource(kind.label)) }, onClick = { onChange(kind.wireValue); expanded = false }) }
        }
    }
}
