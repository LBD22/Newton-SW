package ru.newton.fieldapp.features.project.list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.theme.NewtonTheme
import ru.newton.fieldapp.domain.model.CrsConfig
import ru.newton.fieldapp.domain.model.GeoidConfig
import ru.newton.fieldapp.domain.model.HeightMode
import ru.newton.fieldapp.domain.model.Project

@Composable
fun ProjectListScreen(
    onCreateProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenPoints: (Long) -> Unit,
    onOpenCalibration: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingExportProjectId by remember { mutableStateOf<Long?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        val projectId = pendingExportProjectId
        pendingExportProjectId = null
        if (uri == null || projectId == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri).use { out ->
                    requireNotNull(out) { "Не удалось открыть файл для записи" }
                    viewModel.exportBackup(projectId, out)
                }
            }.onSuccess { result ->
                snackbarHostState.showSnackbar(
                    "Бэкап сохранён: ${result.pointCount} точек, ${result.photoCount} фото",
                )
            }.onFailure {
                snackbarHostState.showSnackbar("Не удалось сохранить бэкап: ${it.message}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Не удалось открыть файл бэкапа" }
                    viewModel.importBackup(input)
                }
            }.onSuccess { result ->
                snackbarHostState.showSnackbar(
                    "Импортировано: ${result.pointCount} точек, ${result.photoCount} фото",
                )
            }.onFailure {
                snackbarHostState.showSnackbar("Не удалось восстановить: ${it.message}")
            }
        }
    }

    ProjectListContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onCreateProject = onCreateProject,
        onOpenProject = onOpenProject,
        onOpenPoints = onOpenPoints,
        onOpenCalibration = onOpenCalibration,
        onMakeActive = viewModel::makeActive,
        onExportProject = { projectId, suggestedName ->
            pendingExportProjectId = projectId
            exportLauncher.launch("$suggestedName.newton-backup.zip")
        },
        onImportBackup = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectListContent(
    state: ProjectListState,
    snackbarHostState: SnackbarHostState,
    onCreateProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenPoints: (Long) -> Unit = {},
    onOpenCalibration: () -> Unit = {},
    onMakeActive: (Long) -> Unit = {},
    onExportProject: (Long, String) -> Unit = { _, _ -> },
    onImportBackup: () -> Unit = {},
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Проекты") },
                actions = {
                    IconButton(onClick = onOpenCalibration) {
                        Icon(Icons.Default.Calculate, contentDescription = "Локальная привязка")
                    }
                    IconButton(onClick = onImportBackup) {
                        Icon(Icons.Default.Restore, contentDescription = "Восстановить из бэкапа")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateProject) {
                Icon(Icons.Default.Add, contentDescription = "Новый проект")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                ProjectListState.Loading -> CircularProgressIndicator()
                ProjectListState.Empty -> EmptyState()
                is ProjectListState.Content -> ProjectList(
                    items = state.projects,
                    activeId = state.activeProjectId,
                    activeProjectName = state.projects.firstOrNull { it.id == state.activeProjectId }?.name,
                    onOpen = onOpenProject,
                    onOpenPoints = onOpenPoints,
                    onMakeActive = onMakeActive,
                    onExportProject = onExportProject,
                )
                is ProjectListState.Error -> ErrorState(state.message)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Проектов ещё нет", style = MaterialTheme.typography.titleLarge)
        Text(
            "Нажмите + чтобы создать первый или восстановите из бэкапа",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(24.dp),
    )
}

@Composable
private fun ProjectList(
    items: List<Project>,
    activeId: Long?,
    activeProjectName: String?,
    onOpen: (Long) -> Unit,
    onOpenPoints: (Long) -> Unit,
    onMakeActive: (Long) -> Unit,
    onExportProject: (Long, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        // Quick-jump shortcut to the active project's point database. Mirrors
        // the LandStar dashboard pattern: a survey day starts and ends in
        // the points list, so it gets pinned at the top.
        if (activeId != null && activeProjectName != null) {
            item("active-points-shortcut") {
                ActivePointsShortcut(
                    projectName = activeProjectName,
                    onClick = { onOpenPoints(activeId) },
                )
            }
        }
        items(items, key = { it.id }) { project ->
            ProjectRow(
                project = project,
                isActive = project.id == activeId,
                onOpen = onOpen,
                onMakeActive = onMakeActive,
                onExport = onExportProject,
            )
        }
    }
}

@Composable
private fun ActivePointsShortcut(projectName: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text("База точек", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Активный проект: $projectName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: Project,
    isActive: Boolean,
    onOpen: (Long) -> Unit,
    onMakeActive: (Long) -> Unit,
    onExport: (Long, String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ElevatedCard(
        onClick = { onOpen(project.id) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                project.comment?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Text(
                    "СК: ${project.crsConfig.presetId}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (isActive) {
                    Text(
                        "Активный проект",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    TextButton(onClick = { onMakeActive(project.id) }) {
                        Text("Сделать активным")
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Меню проекта")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Бэкап в файл…") },
                        onClick = {
                            menuOpen = false
                            onExport(project.id, project.name.replace("[^A-Za-z0-9_-]".toRegex(), "_"))
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectListPreview_Empty() {
    NewtonTheme {
        ProjectListContent(
            state = ProjectListState.Empty,
            snackbarHostState = remember { SnackbarHostState() },
            onCreateProject = {},
            onOpenProject = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectListPreview_Content() {
    NewtonTheme {
        ProjectListContent(
            state = ProjectListState.Content(
                projects = listOf(
                    Project(
                        id = 1,
                        name = "Тестовый объект",
                        comment = "Контурная съёмка кадастра",
                        crsConfig = CrsConfig(
                            presetId = "GSK2011_GK_8",
                            geoid = GeoidConfig.None,
                            heightMode = HeightMode.ELLIPSOIDAL,
                        ),
                        createdAtUtc = 0L,
                        updatedAtUtc = 0L,
                    ),
                ),
                activeProjectId = 1,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onCreateProject = {},
            onOpenProject = {},
        )
    }
}
