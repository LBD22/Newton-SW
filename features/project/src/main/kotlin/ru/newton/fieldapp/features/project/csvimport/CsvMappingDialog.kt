package ru.newton.fieldapp.features.project.csvimport

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.common.csv.CsvColumn
import ru.newton.fieldapp.core.common.csv.CsvSerializer

/**
 * PRJ-013 — explicit CSV column-mapping wizard.
 *
 * Header auto-detection still works for "well-formed" CSVs — but when the
 * incoming file has Russian or otherwise non-standard headers (e.g.
 * «Имя; Северная; Восточная; Высота»), the user sees the preview and picks
 * each target column from a dropdown. Required: Name, N, E, H. The "Игнор"
 * option lets the surveyor skip extra columns the office added.
 *
 * After the user confirms, [onConfirm] is called with the full content +
 * mapping list — the caller then runs [CsvSerializer.parseWithMapping] and
 * commits the rows to the project.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvMappingDialog(
    content: String,
    onConfirm: (mapping: List<CsvColumn?>, delimiter: Char, decimalSeparator: Char) -> Unit,
    onDismiss: () -> Unit,
) {
    var delimiter by remember { mutableStateOf(';') }
    var decimalSep by remember { mutableStateOf('.') }
    val preview = remember(content, delimiter) {
        CsvSerializer.preview(content, delimiter, sampleRows = 3)
    }
    val mapping = remember(preview.headers) {
        mutableStateListOf<CsvColumn?>().also { list ->
            list.clear()
            // Preselect by header match where possible.
            val byHeader = CsvColumn.entries.associateBy { it.header.lowercase() }
            for (h in preview.headers) list.add(byHeader[h.trim().lowercase()])
        }
    }
    val canConfirm by remember(preview.headers) {
        derivedStateOf {
            val mapped = mapping.filterNotNull().toSet()
            CsvColumn.Name in mapped &&
                CsvColumn.N in mapped &&
                CsvColumn.E in mapped &&
                CsvColumn.H in mapped
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сопоставление колонок CSV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = delimiter.toString(),
                        onValueChange = { delimiter = it.firstOrNull() ?: ';' },
                        label = { Text("Разделитель") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = decimalSep.toString(),
                        onValueChange = { decimalSep = it.firstOrNull() ?: '.' },
                        label = { Text("Дробный") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (preview.headers.isEmpty()) {
                    Text(
                        "Файл пуст или не разобран — проверьте разделитель.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(preview.headers.size) { idx ->
                            HeaderRow(
                                header = preview.headers[idx],
                                sampleValues = preview.sampleRows.map { it.getOrNull(idx).orEmpty() },
                                selected = mapping.getOrNull(idx),
                                onSelect = { newCol ->
                                    if (newCol != null) {
                                        // Each target may appear at most once
                                        // — clear any other slot that already
                                        // has it.
                                        for (i in mapping.indices) {
                                            if (i != idx && mapping[i] == newCol) mapping[i] = null
                                        }
                                    }
                                    mapping[idx] = newCol
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = { onConfirm(mapping.toList(), delimiter, decimalSep) },
            ) { Text("Импорт") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun HeaderRow(
    header: String,
    sampleValues: List<String>,
    selected: CsvColumn?,
    onSelect: (CsvColumn?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(header.ifBlank { "(пустой заголовок)" }, style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    sampleValues.joinToString("  · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column {
            TextButton(onClick = { menuOpen = true }) {
                Text(selected?.header ?: "Игнор")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Игнор") },
                    onClick = { onSelect(null); menuOpen = false },
                )
                CsvColumn.entries.forEach { col ->
                    DropdownMenuItem(
                        text = { Text(col.header) },
                        onClick = { onSelect(col); menuOpen = false },
                    )
                }
            }
        }
    }
}
