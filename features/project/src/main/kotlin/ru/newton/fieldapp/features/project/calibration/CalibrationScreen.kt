package ru.newton.fieldapp.features.project.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.crs.LocalCalibration

/**
 * Site-calibration calculator (PRJ-006B). The surveyor enters pairs of
 * `(measured, known)` triples and presses «Рассчитать» — the screen runs
 * [LocalCalibration.solve] and shows the resulting transform plus per-pair
 * residuals and overall RMS.
 *
 * Right now the result is informational: the surveyor uses it to validate
 * their setup or to decide whether to manually offset the project. A future
 * iteration will persist the params per project and apply them on every
 * fix automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(onBack: () -> Unit) {
    val pairs = remember { mutableStateListOf<LocalCalibration.Pair2D>() }
    var result by remember { mutableStateOf<Result<LocalCalibration.Result>?>(null) }
    var addOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Локальная привязка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NewtonSuccessButton(
                    onClick = { addOpen = true },
                    text = "Добавить пару",
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f),
                )
                NewtonPrimaryButton(
                    onClick = {
                        result = runCatching { LocalCalibration.solve(pairs.toList()) }
                    },
                    text = "Рассчитать",
                    icon = Icons.Default.Calculate,
                    enabled = pairs.size >= 2,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NewtonSectionLabel("Контрольные пары (${pairs.size})")
                    Text(
                        "Введите пары «измерено / эталон» в N, E, H. Минимум 2 пары — " +
                            "программа найдёт сдвиг + поворот + масштаб (4 параметра) " +
                            "и сдвиг по высоте (1 параметр).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (pairs.isEmpty()) {
                        Text(
                            "Пока пусто. Нажмите «Добавить пару».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        pairs.forEachIndexed { index, pair ->
                            PairRow(
                                index = index,
                                pair = pair,
                                onRemove = {
                                    pairs.removeAt(index)
                                    result = null
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            result?.let { r ->
                r.fold(
                    onSuccess = { ResultCard(it) },
                    onFailure = {
                        NewtonCard {
                            Text(
                                it.message ?: "Не удалось рассчитать",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    },
                )
            }
        }
    }

    if (addOpen) {
        AddPairDialog(
            onAdd = { p ->
                pairs += p
                result = null
                addOpen = false
            },
            onDismiss = { addOpen = false },
        )
    }
}

@Composable
private fun PairRow(
    index: Int,
    pair: LocalCalibration.Pair2D,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Пара ${index + 1}", style = MaterialTheme.typography.titleSmall)
            Text(
                "измерено: N=${"%.3f".format(pair.measuredN)}  E=${"%.3f".format(pair.measuredE)}  H=${"%.3f".format(pair.measuredH)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "эталон: N=${"%.3f".format(pair.knownN)}  E=${"%.3f".format(pair.knownE)}  H=${"%.3f".format(pair.knownH)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить пару")
        }
    }
}

@Composable
private fun ResultCard(r: LocalCalibration.Result) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel("Параметры трансформации")
            Field("Масштаб", "%.7f".format(r.params.scale))
            Field("Поворот", "%.4f°".format(r.params.rotationDeg))
            Field("Сдвиг N", "%.4f м".format(r.params.translationN))
            Field("Сдвиг E", "%.4f м".format(r.params.translationE))
            Field("Сдвиг H", "%.4f м".format(r.params.translationH))
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NewtonInfoBadge("планарный RMS")
                Text(
                    "${"%.3f".format(r.rmsPlanar * 1000)} мм",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NewtonInfoBadge("вертикальный RMS")
                Text(
                    "${"%.3f".format(r.rmsVertical * 1000)} мм",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider()
            NewtonSectionLabel("Невязки по парам")
            r.residuals.forEachIndexed { idx, res ->
                Text(
                    "${idx + 1}: ΔN=${"%+.3f".format(res.deltaN)} ΔE=${"%+.3f".format(res.deltaE)} ΔH=${"%+.3f".format(res.deltaH)} (план ${"%.3f".format(res.planar * 1000)} мм)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AddPairDialog(
    onAdd: (LocalCalibration.Pair2D) -> Unit,
    onDismiss: () -> Unit,
) {
    var measuredN by remember { mutableStateOf("") }
    var measuredE by remember { mutableStateOf("") }
    var measuredH by remember { mutableStateOf("") }
    var knownN by remember { mutableStateOf("") }
    var knownE by remember { mutableStateOf("") }
    var knownH by remember { mutableStateOf("") }

    fun parse(s: String) = s.trim().replace(',', '.').toDoubleOrNull()
    val parsed: LocalCalibration.Pair2D? = run {
        val mn = parse(measuredN) ?: return@run null
        val me = parse(measuredE) ?: return@run null
        val mh = parse(measuredH) ?: return@run null
        val kn = parse(knownN) ?: return@run null
        val ke = parse(knownE) ?: return@run null
        val kh = parse(knownH) ?: return@run null
        LocalCalibration.Pair2D(mn, me, mh, kn, ke, kh)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Контрольная пара") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NewtonSectionLabel("Измерено")
                Triple(
                    measuredN,
                    measuredE,
                    measuredH,
                    onN = { measuredN = it },
                    onE = { measuredE = it },
                    onH = { measuredH = it },
                )
                NewtonSectionLabel("Эталон")
                Triple(
                    knownN,
                    knownE,
                    knownH,
                    onN = { knownN = it },
                    onE = { knownE = it },
                    onH = { knownH = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onAdd) },
                enabled = parsed != null,
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Suppress("FunctionName")
@Composable
private fun Triple(
    n: String,
    e: String,
    h: String,
    onN: (String) -> Unit,
    onE: (String) -> Unit,
    onH: (String) -> Unit,
) {
    OutlinedTextField(
        value = n,
        onValueChange = onN,
        label = { Text("N, м") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = e,
        onValueChange = onE,
        label = { Text("E, м") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = h,
        onValueChange = onH,
        label = { Text("H, м") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
