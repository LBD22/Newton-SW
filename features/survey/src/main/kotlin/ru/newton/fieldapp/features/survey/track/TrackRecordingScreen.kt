package ru.newton.fieldapp.features.survey.track

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.domain.model.TrackSession
import ru.newton.fieldapp.gnss.data.FixQuality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackRecordingScreen(
    onBack: () -> Unit,
    viewModel: TrackRecordingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<TrackSession?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        val session = pendingExport
        pendingExport = null
        if (uri == null || session == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val payload = viewModel.prepareTrackExport(session.id)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
                Toast.makeText(context, "Трек сохранён", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Не удалось сохранить: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    TrackRecordingContent(
        state = state,
        onBack = onBack,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onDelete = viewModel::delete,
        onExport = { session ->
            pendingExport = session
            exportLauncher.launch("${session.name}.csv")
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackRecordingContent(
    state: TrackRecordingState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDelete: (Long) -> Unit,
    onExport: (TrackSession) -> Unit,
) {
    val isRecording = state.activeSessionId != null
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Запись трека") },
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
                if (isRecording) {
                    NewtonPrimaryButton(
                        onClick = onStop,
                        text = "Остановить запись",
                        icon = Icons.Default.Stop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    NewtonSuccessButton(
                        onClick = onStart,
                        text = "Начать запись",
                        icon = Icons.Default.PlayArrow,
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
            ActiveCard(state = state, isRecording = isRecording)
            HistoryCard(
                sessions = state.sessions,
                activeId = state.activeSessionId,
                onDelete = onDelete,
                onExport = onExport,
            )
        }
    }
}

@Composable
private fun ActiveCard(state: TrackRecordingState, isRecording: Boolean) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel(if (isRecording) "Идёт запись" else "Готов к записи")
            if (isRecording) {
                Text(
                    "Накоплено точек: ${state.activePointCount}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Сэмпл ≈ 1 раз в секунду. Точки без фикса пропускаются.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Текущий фикс: ${describeFix(state.currentFix)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    "Запись пишет позицию в активный проект каждую секунду пока кнопка нажата. " +
                        "Сначала отметьте проект как активный и подключитесь к приёмнику.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    sessions: List<TrackSession>,
    activeId: Long?,
    onDelete: (Long) -> Unit,
    onExport: (TrackSession) -> Unit,
) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel("История треков")
            if (sessions.isEmpty()) {
                Text(
                    "Нет ни одной записанной сессии в активном проекте.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sessions, key = TrackSession::id) { session ->
                        SessionRow(
                            session,
                            isActive = session.id == activeId,
                            onDelete = onDelete,
                            onExport = onExport,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: TrackSession,
    isActive: Boolean,
    onDelete: (Long) -> Unit,
    onExport: (TrackSession) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.name, style = MaterialTheme.typography.titleSmall)
            val started = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(session.startedAtUtc))
            val ended = session.stoppedAtUtc?.let {
                SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it))
            } ?: "идёт"
            Text(
                "$started — $ended",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isActive) NewtonInfoBadge("Идёт")
        if (!isActive) {
            IconButton(onClick = { onExport(session) }) {
                Icon(Icons.Default.Download, contentDescription = "Экспорт CSV")
            }
            IconButton(onClick = { onDelete(session.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
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
