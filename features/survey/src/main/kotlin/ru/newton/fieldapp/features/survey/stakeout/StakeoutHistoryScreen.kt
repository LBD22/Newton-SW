package ru.newton.fieldapp.features.survey.stakeout

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.model.StakeoutMode
import ru.newton.fieldapp.domain.model.StakeoutResult
import ru.newton.fieldapp.domain.repository.StakeoutResultRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StakeoutHistoryRow(
    val result: StakeoutResult,
    val passed: Boolean,
)

data class StakeoutHistoryState(
    val rows: List<StakeoutHistoryRow> = emptyList(),
    val toleranceHorizontalM: Double = 0.030,
    val toleranceVerticalM: Double = 0.050,
)

@HiltViewModel
class StakeoutHistoryViewModel
    @OptIn(ExperimentalCoroutinesApi::class)
    @Inject
    constructor(
        private val activeProject: ActiveProjectStore,
        preferences: ru.newton.fieldapp.features.survey.defaults.SurveyPreferences,
        private val repository: StakeoutResultRepository,
        private val exportUseCase: ru.newton.fieldapp.domain.usecase.ExportStakeoutHistoryCsvUseCase,
    ) : ViewModel() {
        suspend fun prepareExport(): String {
            val id = activeProject.activeId.firstOrNull() ?: return ""
            return exportUseCase(id)
        }
        val state: StateFlow<StakeoutHistoryState> = kotlinx.coroutines.flow.combine(
            activeProject.activeId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else repository.observeByProject(id)
            },
            preferences.defaults,
        ) { results, defaults ->
            val rows = results.map { r ->
                StakeoutHistoryRow(
                    result = r,
                    passed = r.deltaHorizontalM <= defaults.toleranceHorizontalM &&
                        kotlin.math.abs(r.deltaVerticalM) <= defaults.toleranceVerticalM,
                )
            }
            StakeoutHistoryState(
                rows = rows,
                toleranceHorizontalM = defaults.toleranceHorizontalM,
                toleranceVerticalM = defaults.toleranceVerticalM,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StakeoutHistoryState())

        fun delete(id: Long) {
            viewModelScope.launch { repository.delete(id) }
        }
    }

@Composable
fun StakeoutHistoryScreen(
    onBack: () -> Unit,
    viewModel: StakeoutHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val payload = viewModel.prepareExport()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
                Toast.makeText(context, "История выноса сохранена", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Не удалось сохранить: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    StakeoutHistoryContent(
        state = state,
        onBack = onBack,
        onDelete = viewModel::delete,
        onExport = { exportLauncher.launch("stakeout_history.csv") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutHistoryContent(
    state: StakeoutHistoryState,
    onBack: () -> Unit,
    onDelete: (Long) -> Unit,
    onExport: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("История выноса") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state.rows.isNotEmpty()) {
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.Download, contentDescription = "Экспорт CSV")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.rows.isEmpty()) {
                NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NewtonSectionLabel("Пусто")
                        Text(
                            "В активном проекте нет записей выноса. Сделайте «Сохранить как-есть» " +
                                "на экране выноса точки или линии — результаты появятся здесь.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    "Допуски: гориз ≤ ${"%.3f".format(state.toleranceHorizontalM)} м, " +
                        "верт ≤ ${"%.3f".format(state.toleranceVerticalM)} м",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.rows, key = { it.result.id }) { row ->
                        ResultRow(row, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(row: StakeoutHistoryRow, onDelete: (Long) -> Unit) {
    val result = row.result
    val passColors = ru.newton.fieldapp.core.ui.theme.LocalFixStatusColors.current
    NewtonCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(result.targetLabel, style = MaterialTheme.typography.titleMedium)
                    NewtonInfoBadge(if (result.mode == StakeoutMode.POINT) "точка" else "линия")
                    if (row.passed) {
                        ru.newton.fieldapp.core.ui.components.NewtonSuccessBadge(text = "PASS")
                    } else {
                        ru.newton.fieldapp.core.ui.components.NewtonStatusPill(
                            text = "FAIL",
                            background = passColors.noFix.copy(alpha = 0.16f),
                            contentColor = passColors.noFix,
                        )
                    }
                }
                Text(
                    "Δ горизонт: ${"%.3f".format(result.deltaHorizontalM)} м, " +
                        "Δ верт: ${"%+.3f".format(result.deltaVerticalM)} м",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Цель: N=${"%.3f".format(result.targetN)}  E=${"%.3f".format(result.targetE)}  H=${"%.3f".format(result.targetH)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Факт: N=${"%.3f".format(result.actualN)}  E=${"%.3f".format(result.actualE)}  H=${"%.3f".format(result.actualH)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(result.savedAtUtc)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onDelete(result.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}
