package ru.newton.fieldapp.features.survey.stakeout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.domain.model.Point

@Composable
fun StakeoutToLineScreen(
    onBack: () -> Unit,
    viewModel: StakeoutToLineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StakeoutToLineContent(
        state = state,
        onBack = onBack,
        onSelectA = viewModel::selectA,
        onSelectB = viewModel::selectB,
        onClear = viewModel::clear,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutToLineContent(
    state: StakeoutToLineState,
    onBack: () -> Unit,
    onSelectA: (Long) -> Unit,
    onSelectB: (Long) -> Unit,
    onClear: () -> Unit,
) {
    var pickingFor by remember { mutableStateOf<Endpoint?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вынос линии") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EndpointPicker(
                label = "Точка A (начало)",
                point = state.pointA,
                onClick = { pickingFor = Endpoint.A },
            )
            EndpointPicker(
                label = "Точка B (конец)",
                point = state.pointB,
                onClick = { pickingFor = Endpoint.B },
            )
            if (state.pointA != null || state.pointB != null) {
                NewtonSecondaryButton(
                    onClick = onClear,
                    text = "Сбросить",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val vector = state.vector
            when {
                vector == null && (state.pointA == null || state.pointB == null) -> Text(
                    "Выберите две точки — линия проходит от A к B.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                vector == null -> Text(
                    "Ожидание фикса…",
                    style = MaterialTheme.typography.titleMedium,
                )
                else -> LiveBlock(state, vector)
            }
        }
    }

    pickingFor?.let { which ->
        PointPickerDialog(
            title = if (which == Endpoint.A) "Выбор точки A" else "Выбор точки B",
            points = state.availablePoints,
            onDismiss = { pickingFor = null },
            onPick = { id ->
                if (which == Endpoint.A) onSelectA(id) else onSelectB(id)
                pickingFor = null
            },
        )
    }
}

private enum class Endpoint { A, B }

@Composable
private fun EndpointPicker(label: String, point: Point?, onClick: () -> Unit) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (point == null) {
                NewtonPrimaryButton(onClick = onClick, text = "Выбрать…")
            } else {
                Text(point.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "N=${"%.3f".format(point.n)}  E=${"%.3f".format(point.e)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                TextButton(onClick = onClick) { Text("Заменить…") }
            }
        }
    }
}

@Composable
private fun LiveBlock(state: StakeoutToLineState, vector: LineStakeoutVector) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Линия: ${state.pointA?.name} → ${state.pointB?.name}", style = MaterialTheme.typography.titleMedium)
            val side = when {
                vector.offM > 0 -> "справа"
                vector.offM < 0 -> "слева"
                else -> "точно на линии"
            }
            Text(
                "Off-line: ${"%+.3f".format(vector.offM)} м  ($side)",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Along-line: ${"%.3f".format(vector.alongM)} м",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "ΔH: ${"%+.3f".format(vector.deltaH)} м",
                style = MaterialTheme.typography.bodyLarge,
            )
            vector.nearestEnd?.let {
                Text(
                    "Ближе к ${if (it == LineStakeoutVector.Endpoint.A) "A" else "B"} " +
                        "(дистанция ${"%.3f".format(vector.distanceToFootM)} м)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PointPickerDialog(
    title: String,
    points: List<Point>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (points.isEmpty()) {
                Text("В активном проекте нет точек.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(points, key = { it.id }) { p ->
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            TextButton(
                                onClick = { onPick(p.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "${p.name}  (N=${"%.2f".format(p.n)}  E=${"%.2f".format(p.e)})",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}
