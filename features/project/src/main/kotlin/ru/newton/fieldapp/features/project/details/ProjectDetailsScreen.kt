package ru.newton.fieldapp.features.project.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonNavCard
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.displayLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PRJ-006 — project metadata summary. Mirrors the screen-map split:
 *  - this screen owns the project-scope info (name, CRS preset, dates, comment)
 *  - the points list moved to [PointsListScreen] (PRJ-010) so it can have its
 *    own search filter without crowding the summary
 *  - clicking a point still drills into PRJ-011 [PointDetailScreen]
 *
 * Import/export and "Add point" actions live on the points-list screen now —
 * they're list-scope, not project-scope.
 */
@Composable
fun ProjectDetailsScreen(
    onBack: () -> Unit,
    onChangeCrs: () -> Unit,
    onOpenPoints: () -> Unit,
    onOpenLayers: () -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProjectDetailsContent(
        state = state,
        onBack = onBack,
        onChangeCrs = onChangeCrs,
        onOpenPoints = onOpenPoints,
        onOpenLayers = onOpenLayers,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDetailsContent(
    state: ProjectDetailsState,
    onBack: () -> Unit,
    onChangeCrs: () -> Unit,
    onOpenPoints: () -> Unit,
    onOpenLayers: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    val title = when (state) {
                        is ProjectDetailsState.Content -> state.project.name
                        else -> "Проект"
                    }
                    Text(title)
                },
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
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                ProjectDetailsState.Loading -> CircularProgressIndicator()
                ProjectDetailsState.NotFound ->
                    Text("Проект не найден", style = MaterialTheme.typography.titleMedium)
                is ProjectDetailsState.Error ->
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                is ProjectDetailsState.Content -> ContentBody(
                    state = state,
                    onChangeCrs = onChangeCrs,
                    onOpenPoints = onOpenPoints,
                    onOpenLayers = onOpenLayers,
                )
            }
        }
    }
}

@Composable
private fun ContentBody(
    state: ProjectDetailsState.Content,
    onChangeCrs: () -> Unit,
    onOpenPoints: () -> Unit,
    onOpenLayers: () -> Unit,
) {
    val crsLabel = CrsPresets.parse(state.project.crsConfig.presetId)?.displayLabel()
        ?: state.project.crsConfig.presetId
    val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(state.project.createdAtUtc))
    val updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(state.project.updatedAtUtc))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NewtonCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NewtonSectionLabel("Параметры проекта")
                MetaRow("Создан", createdAt)
                MetaRow("Обновлён", updatedAt)
                state.project.comment?.takeIf { it.isNotBlank() }?.let { MetaRow("Комментарий", it) }
            }
        }

        NewtonNavCard(
            title = "Точки (${state.points.size})",
            subtitle = "Просмотр, добавление, импорт и экспорт CSV",
            leadingIcon = Icons.Default.Place,
            onClick = onOpenPoints,
        )

        NewtonNavCard(
            title = "Слои",
            subtitle = "Группировка точек и цвета на карте",
            leadingIcon = Icons.Default.Layers,
            onClick = onOpenLayers,
        )

        NewtonNavCard(
            title = "Система координат",
            subtitle = crsLabel,
            leadingIcon = Icons.Default.GpsFixed,
            onClick = onChangeCrs,
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
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
