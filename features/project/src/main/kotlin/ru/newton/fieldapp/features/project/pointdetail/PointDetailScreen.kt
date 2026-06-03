package ru.newton.fieldapp.features.project.pointdetail

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.model.PointSource
import ru.newton.fieldapp.domain.repository.PointRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val FILE_PROVIDER_AUTHORITY = "ru.newton.fieldapp.fileprovider"
private const val PHOTOS_DIR = "photos"

@HiltViewModel
class PointDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pointRepository: PointRepository,
    ) : ViewModel() {
        private val pointId: Long = checkNotNull(savedStateHandle["pointId"]) {
            "PointDetailViewModel requires `pointId` nav argument"
        }

        val state: StateFlow<PointDetailState> = pointRepository.observeById(pointId)
            .map { p -> if (p == null) PointDetailState.Deleted else PointDetailState.Loaded(p) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PointDetailState.Loading)

        fun saveNote(note: String) {
            viewModelScope.launch { pointRepository.updateNote(pointId, note.trim()) }
        }

        fun setPhoto(path: String?) {
            viewModelScope.launch { pointRepository.updatePhotoPath(pointId, path) }
        }

        fun delete() {
            viewModelScope.launch { pointRepository.delete(pointId) }
        }
    }

sealed interface PointDetailState {
    data object Loading : PointDetailState
    data object Deleted : PointDetailState
    data class Loaded(val point: Point) : PointDetailState
}

@Composable
fun PointDetailScreen(
    onBack: () -> Unit,
    viewModel: PointDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is PointDetailState.Deleted) onBack()
    }

    PointDetailContent(
        state = state,
        onBack = onBack,
        onDelete = viewModel::delete,
        onSaveNote = viewModel::saveNote,
        onSetPhoto = viewModel::setPhoto,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointDetailContent(
    state: PointDetailState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSaveNote: (String) -> Unit,
    onSetPhoto: (String?) -> Unit,
) {
    var confirmingDelete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? PointDetailState.Loaded)?.point?.name ?: "Точка"
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state is PointDetailState.Loaded) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                PointDetailState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                PointDetailState.Deleted -> Unit
                is PointDetailState.Loaded -> LoadedBody(
                    point = state.point,
                    onSaveNote = onSaveNote,
                    onSetPhoto = onSetPhoto,
                )
            }
        }
    }

    if (confirmingDelete && state is PointDetailState.Loaded) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Удалить точку?") },
            text = { Text("«${state.point.name}» будет удалена безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun LoadedBody(
    point: Point,
    onSaveNote: (String) -> Unit,
    onSetPhoto: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AttributesCard(point)
        CoordinatesCard(point)
        NoteCard(point, onSaveNote)
        PhotoCard(point, onSetPhoto)
        Box(modifier = Modifier.fillMaxWidth()) {
            NewtonInfoBadge("id ${point.id}")
        }
    }
}

@Composable
private fun AttributesCard(point: Point) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("Имя и атрибуты")
            Text(point.name, style = MaterialTheme.typography.headlineSmall)
            Field("Ревизия", "${point.revision}")
            Field("Источник", sourceLabel(point.source))
            point.code?.takeIf { it.isNotBlank() }?.let { Field("Код", it) }
            point.layerId?.let { Field("Слой (id)", it.toString()) }
            point.externalRef?.takeIf { it.isNotBlank() }?.let { Field("Внешняя ссылка", it) }
            Field(
                "Создана",
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(point.createdAtUtc)),
            )
        }
    }
}

@Composable
private fun CoordinatesCard(point: Point) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("Координаты проекта")
            Field("N", "%.4f".format(point.n))
            Field("E", "%.4f".format(point.e))
            Field("H", "%.4f".format(point.h))
        }
    }
}

@Composable
private fun NoteCard(point: Point, onSaveNote: (String) -> Unit) {
    var draft by remember(point.id, point.note) { mutableStateOf(point.note) }
    val dirty by remember { derivedStateOf { draft != point.note } }

    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel("Заметка")
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Описание точки, особенности местности") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
            )
            if (dirty) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonPrimaryButton(
                        onClick = { onSaveNote(draft) },
                        text = "Сохранить",
                        modifier = Modifier.weight(1f),
                    )
                    NewtonSecondaryButton(
                        onClick = { draft = point.note },
                        text = "Отмена",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(point: Point, onSetPhoto: (String?) -> Unit) {
    val context = LocalContext.current
    var pendingFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingFile
        pendingFile = null
        if (success && file != null) {
            // Persist relative path under filesDir/photos/ for portability.
            val relativePath = "$PHOTOS_DIR/${file.name}"
            onSetPhoto(relativePath)
        } else if (file != null) {
            // Camera was cancelled — clean up the empty file we pre-created.
            scope.launch { runCatching { file.delete() } }
        }
    }

    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel("Фото")
            point.photoPath?.let { rel ->
                val absoluteFile = remember(rel) { File(context.filesDir, rel) }
                if (absoluteFile.exists()) {
                    PhotoThumbnail(absoluteFile)
                } else {
                    Text(
                        "Файл фото не найден: $rel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } ?: Text(
                "Фото не прикреплено.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NewtonPrimaryButton(
                    onClick = {
                        val file = newPhotoFile(context, point.id)
                        pendingFile = file
                        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                        takePictureLauncher.launch(uri)
                    },
                    text = if (point.photoPath == null) "Сделать фото" else "Заменить",
                    icon = Icons.Default.PhotoCamera,
                    modifier = Modifier.weight(1f),
                )
                if (point.photoPath != null) {
                    NewtonSecondaryButton(
                        onClick = {
                            val rel = point.photoPath
                            scope.launch {
                                runCatching { File(context.filesDir, rel).delete() }
                                onSetPhoto(null)
                            }
                        },
                        text = "Удалить фото",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(file: File) {
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }
    if (bitmap == null) {
        Text("Не удалось открыть фото.", color = MaterialTheme.colorScheme.error)
        return
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Фото точки",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(12.dp)),
    )
}

private fun newPhotoFile(context: Context, pointId: Long): File {
    val dir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
    val ts = System.currentTimeMillis()
    return File(dir, "${pointId}_$ts.jpg")
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

private fun sourceLabel(source: PointSource): String = when (source) {
    PointSource.SURVEY -> "Съёмка"
    PointSource.IMPORT -> "Импорт"
    PointSource.MANUAL -> "Ручной ввод"
    PointSource.CALC -> "Расчёт"
}
