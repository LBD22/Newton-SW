package ru.newton.fieldapp.features.settings.staticobs

import android.net.Uri
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.data.staticobs.StaticNmeaRecorder
import ru.newton.fieldapp.data.staticobs.StaticRecorderState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StaticObsViewModel
    @Inject
    constructor(
        private val recorder: StaticNmeaRecorder,
    ) : ViewModel() {
        val state: StateFlow<StaticRecorderState> = recorder.state
        private val _files = MutableStateFlow<List<File>>(recorder.listPastSessions())
        val files: StateFlow<List<File>> = _files.asStateFlow()

        fun start() = recorder.start()
        fun stop() {
            recorder.stop()
            refresh()
        }
        fun delete(file: File) {
            recorder.delete(file)
            refresh()
        }
        fun refresh() {
            viewModelScope.launch { _files.value = recorder.listPastSessions() }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticObsScreen(
    onBack: () -> Unit,
    viewModel: StaticObsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingShareFile by remember { mutableStateOf<File?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        val src = pendingShareFile
        pendingShareFile = null
        if (uri == null || src == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri).use { out ->
                    requireNotNull(out)
                    src.inputStream().use { it.copyTo(out) }
                }
            }
        }
    }

    // While recording, tick once per second so the UI shows live duration.
    LaunchedEffect(state) {
        while (state is StaticRecorderState.Active) {
            delay(1_000)
            // Trigger recomposition by reading the StateFlow's current value
            // — the recorder updates bytesWritten on each chunk anyway.
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Запись NMEA (статика)") },
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
            ) {
                if (state is StaticRecorderState.Active) {
                    NewtonPrimaryButton(
                        onClick = viewModel::stop,
                        text = "Остановить",
                        icon = Icons.Default.Stop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    NewtonSuccessButton(
                        onClick = viewModel::start,
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
            ActiveCard(state)
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NewtonSectionLabel("Что это")
                    Text(
                        "Сохраняет в файл всё, что приёмник присылает по DataSPP — " +
                            "NMEA-строки, RTCM-байты и любые другие сообщения. Файл " +
                            "можно отдать на пост-обработку (NMEA-плеер, RTKLIB, " +
                            "конвертеры в RINEX). Запись идёт в реальном времени; " +
                            "размер ограничен только местом на устройстве.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NewtonSectionLabel("Записанные сессии")
                        NewtonInfoBadge("${files.size}")
                    }
                    if (files.isEmpty()) {
                        Text(
                            "Пока ничего не записано.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(files, key = File::getAbsolutePath) { file ->
                                FileRow(
                                    file = file,
                                    onShare = {
                                        pendingShareFile = file
                                        saveLauncher.launch(file.name)
                                    },
                                    onDelete = { viewModel.delete(file) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveCard(state: StaticRecorderState) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel(if (state is StaticRecorderState.Active) "Идёт запись" else "Готов к старту")
            when (state) {
                is StaticRecorderState.Active -> {
                    val rec = state.recording
                    val durationSec = ((System.currentTimeMillis() - rec.startedAtUtc) / 1000L)
                    Text(
                        rec.file.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Длительность: ${formatDuration(durationSec)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Записано: ${humanBytes(rec.bytesWritten)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                StaticRecorderState.Idle -> Text(
                    "Подключите приёмник по DataSPP и запустите запись. " +
                        "Файл сохранится во внутреннюю память приложения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is StaticRecorderState.Failed -> Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun FileRow(file: File, onShare: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.titleSmall)
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(file.lastModified()))
            Text(
                "$ts · ${humanBytes(file.length())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.SaveAlt, contentDescription = "Сохранить копию")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить")
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds / 60) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun humanBytes(n: Long): String = when {
    n >= 1_000_000 -> "%.2f МБ".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1f КБ".format(n / 1_000.0)
    else -> "$n Б"
}
