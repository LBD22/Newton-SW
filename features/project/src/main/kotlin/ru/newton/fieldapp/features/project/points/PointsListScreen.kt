package ru.newton.fieldapp.features.project.points

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.model.PointSource
import ru.newton.fieldapp.features.project.csvimport.CsvMappingDialog
import ru.newton.fieldapp.features.project.details.ProjectDetailsEvent
import ru.newton.fieldapp.features.project.details.ProjectDetailsState
import ru.newton.fieldapp.features.project.details.ProjectDetailsViewModel

/**
 * PRJ-010 — dedicated screen for the project's point list with search filter.
 *
 * Reuses [ProjectDetailsViewModel] because the underlying data (project +
 * points + CSV import/export) is identical. The summary screen
 * (`ProjectDetailsScreen`) shows the metadata card and routes here for the
 * actual list. Splitting them mirrors the screen-map (PRJ-006 vs PRJ-010) and
 * keeps the summary uncluttered when the project has hundreds of points.
 */
@Composable
fun PointsListScreen(
    onBack: () -> Unit,
    onAddPoint: () -> Unit,
    onOpenPoint: (Long) -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingCsv by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { content ->
                if (content != null) pendingCsv = content
            }.onFailure {
                snackbarHostState.showSnackbar("Не удалось прочитать файл: ${it.message}")
            }
        }
    }

    pendingCsv?.let { csv ->
        CsvMappingDialog(
            content = csv,
            onConfirm = { mapping, delim, decSep ->
                pendingCsv = null
                viewModel.onImportCsvWithMapping(csv, mapping, delim, decSep)
            },
            onDismiss = { pendingCsv = null },
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val payload = viewModel.prepareExport()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
                snackbarHostState.showSnackbar("Экспорт сохранён")
            }.onFailure {
                snackbarHostState.showSnackbar("Не удалось сохранить файл: ${it.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectDetailsEvent.ImportFinished -> snackbarHostState.showSnackbar(
                    "Импорт: добавлено ${event.saved}, пропущено ${event.skipped}",
                )
                is ProjectDetailsEvent.ImportFailed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    PointsListContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddPoint = onAddPoint,
        onOpenPoint = onOpenPoint,
        onImport = { importLauncher.launch(arrayOf("text/*")) },
        onExport = {
            val name = (state as? ProjectDetailsState.Content)?.project?.name ?: "project"
            exportLauncher.launch("$name.csv")
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointsListContent(
    state: ProjectDetailsState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddPoint: () -> Unit,
    onOpenPoint: (Long) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var sourceFilter by remember { mutableStateOf<PointSource?>(null) }
    var codeFilter by remember { mutableStateOf<String?>(null) }
    val points = (state as? ProjectDetailsState.Content)?.points.orEmpty()
    val codes = remember(points) {
        points.mapNotNull { it.code?.takeIf { c -> c.isNotBlank() } }
            .distinct()
            .sorted()
    }
    // Active filters compose: name substring AND source AND code.
    val filtered by remember(points, query, sourceFilter, codeFilter) {
        derivedStateOf {
            points.asSequence()
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                .filter { sourceFilter == null || it.source == sourceFilter }
                .filter { codeFilter == null || it.code == codeFilter }
                .toList()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val total = points.size
                    val shown = filtered.size
                    Text(
                        if (shown == total) "Точки ($total)" else "Точки ($shown из $total)",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state is ProjectDetailsState.Content) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Импорт CSV…") },
                                onClick = { menuOpen = false; onImport() },
                            )
                            DropdownMenuItem(
                                text = { Text("Экспорт CSV…") },
                                onClick = { menuOpen = false; onExport() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state is ProjectDetailsState.Content) {
                FloatingActionButton(onClick = onAddPoint) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить точку")
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Поиск по имени") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // Source filter chips (Survey / Import / Manual / Calc)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = sourceFilter == null,
                    onClick = { sourceFilter = null },
                    label = { Text("все") },
                )
                PointSource.entries.forEach { src ->
                    FilterChip(
                        selected = sourceFilter == src,
                        onClick = { sourceFilter = if (sourceFilter == src) null else src },
                        label = { Text(sourceLabel(src)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
            // Code filter chips — only render the row if any code exists.
            if (codes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = codeFilter == null,
                        onClick = { codeFilter = null },
                        label = { Text("любой код") },
                    )
                    codes.forEach { code ->
                        FilterChip(
                            selected = codeFilter == code,
                            onClick = { codeFilter = if (codeFilter == code) null else code },
                            label = { Text(code) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    ProjectDetailsState.Loading,
                    ProjectDetailsState.NotFound,
                    is ProjectDetailsState.Error,
                    -> Unit
                    is ProjectDetailsState.Content -> {
                        if (filtered.isEmpty()) {
                            Text(
                                if (points.isEmpty()) {
                                    "Точек ещё нет. Нажмите + чтобы добавить."
                                } else {
                                    "Ничего не найдено."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(filtered, key = Point::id) { point ->
                                    PointRow(point, onClick = { onOpenPoint(point.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sourceLabel(source: PointSource): String = when (source) {
    PointSource.SURVEY -> "съёмка"
    PointSource.IMPORT -> "импорт"
    PointSource.MANUAL -> "ручной"
    PointSource.CALC -> "расчёт"
}

@Composable
private fun PointRow(point: Point, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${point.name}  rev ${point.revision}", style = MaterialTheme.typography.titleMedium)
            Text(
                "N=${"%.3f".format(point.n)}  E=${"%.3f".format(point.e)}  H=${"%.3f".format(point.h)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            point.code?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Код: $it",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
