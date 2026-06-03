package ru.newton.fieldapp.features.survey.line

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.gnss.data.FixQuality

@Composable
fun LineSurveyScreen(
    onBack: () -> Unit,
    viewModel: LineSurveyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LineSurveyContent(
        state = state,
        onBack = onBack,
        onNameChanged = viewModel::onNameChanged,
        onStartVertex = viewModel::startNextVertex,
        onCancelCurrent = viewModel::cancelCurrent,
        onUndo = viewModel::removeLastVertex,
        onFinish = viewModel::finish,
        onReset = viewModel::reset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineSurveyContent(
    state: LineSurveyState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onStartVertex: () -> Unit,
    onCancelCurrent: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
) {
    val name = when (state) {
        is LineSurveyState.Idle -> state.lineName
        is LineSurveyState.Collecting -> state.lineName
        is LineSurveyState.BetweenVertices -> state.lineName
        is LineSurveyState.Saving -> state.lineName
        is LineSurveyState.Saved -> state.lineName
        is LineSurveyState.Error -> ""
    }
    val vertexCount = when (state) {
        is LineSurveyState.Collecting -> state.collectedVertexCount
        is LineSurveyState.BetweenVertices -> state.vertices.size
        is LineSurveyState.Saving -> state.total
        is LineSurveyState.Saved -> state.savedCount
        else -> 0
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Съёмка линии") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state) {
                    is LineSurveyState.Idle -> NewtonSuccessButton(
                        onClick = onStartVertex,
                        text = "Снять первую вершину",
                        icon = Icons.Default.Add,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is LineSurveyState.Collecting -> NewtonSecondaryButton(
                        onClick = onCancelCurrent,
                        text = "Отмена",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is LineSurveyState.BetweenVertices -> {
                        NewtonSecondaryButton(
                            onClick = onUndo,
                            text = "Откатить",
                            icon = Icons.Default.Undo,
                            modifier = Modifier.weight(1f),
                        )
                        NewtonPrimaryButton(
                            onClick = onStartVertex,
                            text = "Следующая",
                            icon = Icons.Default.Add,
                            modifier = Modifier.weight(1f),
                        )
                        NewtonSuccessButton(
                            onClick = onFinish,
                            text = "Завершить",
                            icon = Icons.Default.Save,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    is LineSurveyState.Saving -> Unit
                    is LineSurveyState.Saved, is LineSurveyState.Error -> NewtonPrimaryButton(
                        onClick = onReset,
                        text = "Новая линия",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NameCard(name = name, enabled = state is LineSurveyState.Idle || state is LineSurveyState.BetweenVertices, onNameChanged = onNameChanged)
            ProgressCard(state, vertexCount)
            (state as? LineSurveyState.Error)?.let {
                Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NameCard(name: String, enabled: Boolean, onNameChanged: (String) -> Unit) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel("Линия")
            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("Имя линии") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressCard(state: LineSurveyState, vertexCount: Int) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NewtonSectionLabel("Вершины")
                NewtonInfoBadge("$vertexCount шт.")
            }
            when (state) {
                is LineSurveyState.Idle -> Text(
                    "Установите веху на первой точке линии и нажмите «Снять первую вершину».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is LineSurveyState.Collecting -> {
                    Text(
                        "Накоплено для текущей вершины: ${state.collectedAtCurrentVertex} / ${state.target}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { state.collectedAtCurrentVertex.toFloat() / state.target.toFloat().coerceAtLeast(1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Текущий фикс: ${describeFix(state.currentFix)}", style = MaterialTheme.typography.bodyMedium)
                }
                is LineSurveyState.BetweenVertices -> {
                    if (state.vertices.isEmpty()) {
                        Text(
                            "Пока нет вершин.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.vertices.forEachIndexed { idx, v ->
                            Text(
                                "v$idx: φ=${"%.7f".format(v.n)}°  λ=${"%.7f".format(v.e)}°  h=${"%.3f".format(v.h)} м  σH=${"%.3f".format(v.sigmaH)} м",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                is LineSurveyState.Saving -> Text("Сохранение ${state.total} вершин…", style = MaterialTheme.typography.titleMedium)
                is LineSurveyState.Saved -> Text(
                    "Сохранено: ${state.savedCount} вершин (как точки ${state.lineName}_v0…)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                is LineSurveyState.Error -> Unit
            }
        }
    }
}

private fun describeFix(fix: FixQuality): String = when (fix) {
    FixQuality.NoFix -> "нет фикса"
    FixQuality.Single -> "Single"
    FixQuality.DGnss -> "DGNSS"
    FixQuality.FloatRtk -> "Float RTK"
    FixQuality.FixedRtk -> "Fixed RTK"
    is FixQuality.Ppp -> "PPP"
}
