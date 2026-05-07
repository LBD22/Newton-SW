package ru.newton.fieldapp.features.settings.tiles

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.data.preferences.OfflineTilesPreferences
import java.io.File
import javax.inject.Inject

@HiltViewModel
class OfflineTilesViewModel
    @Inject
    constructor(
        private val preferences: OfflineTilesPreferences,
    ) : ViewModel() {
        val activeFile: StateFlow<File?> = preferences.activeFile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        suspend fun import(uri: Uri, name: String): File = preferences.import(uri, name)

        fun clear() {
            viewModelScope.launch { preferences.clear() }
        }
    }

@Composable
fun OfflineTilesScreen(
    onBack: () -> Unit,
    viewModel: OfflineTilesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active by viewModel.activeFile.collectAsStateWithLifecycle()
    var lastError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "tiles.mbtiles"
                } ?: "tiles.mbtiles"
                viewModel.import(uri, name)
            }.onFailure { lastError = it.message }
        }
    }

    OfflineTilesContent(
        activePath = active?.absolutePath,
        activeBytes = active?.length() ?: 0L,
        lastError = lastError,
        onPick = { launcher.launch(arrayOf("*/*")) },
        onClear = viewModel::clear,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineTilesContent(
    activePath: String?,
    activeBytes: Long,
    lastError: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Офлайн-карты") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Активный архив")
                    if (activePath != null) {
                        Text(activePath, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Размер: ${"%.1f".format(activeBytes / 1024.0 / 1024.0)} МБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Архив не выбран — карта работает онлайн через OSM.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            NewtonPrimaryButton(
                onClick = onPick,
                text = if (activePath == null) "Выбрать MBTiles…" else "Заменить архив",
                icon = Icons.Default.FolderOpen,
                modifier = Modifier.fillMaxWidth(),
            )

            if (activePath != null) {
                NewtonSecondaryButton(
                    onClick = onClear,
                    text = "Удалить архив",
                    icon = Icons.Default.Delete,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                "Поддерживается формат MBTiles (Mapbox/SQLite). Файл копируется во " +
                    "внутреннюю память приложения. Для офлайн-съёмки рекомендуется " +
                    "пакет с зумом 14–18 на район работ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            lastError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
