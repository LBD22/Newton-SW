package ru.newton.fieldapp.features.project.details

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.MetricPillRow
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonNavCard
import ru.newton.fieldapp.core.ui.components.SectionLabel
import ru.newton.fieldapp.core.ui.components.TileAccent
import ru.newton.fieldapp.core.ui.components.TileData
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.displayLabel
import ru.newton.fieldapp.domain.model.HeightMode
import ru.newton.fieldapp.domain.model.PointSource
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
        onSetHeightMode = viewModel::setHeightMode,
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
    onSetHeightMode: (HeightMode) -> Unit,
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
                    onSetHeightMode = onSetHeightMode,
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
    onSetHeightMode: (HeightMode) -> Unit,
) {
    val crsLabel = CrsPresets.parse(state.project.crsConfig.presetId)?.displayLabel()
        ?: state.project.crsConfig.presetId
    val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(state.project.createdAtUtc))
    val updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(state.project.updatedAtUtc))

    val totalPoints = state.points.size
    val surveyedCount = state.points.count { it.source == PointSource.SURVEY }
    val importedCount = state.points.count { it.source == PointSource.IMPORT }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Hero project header: name + CRS pill + dates + comment ──────
        NewtonCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = state.project.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W800),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrsPill(label = crsLabel)
                }
                MetaRow("Создан", createdAt)
                MetaRow("Обновлён", updatedAt)
                state.project.comment?.takeIf { it.isNotBlank() }?.let {
                    MetaRow("Комментарий", it)
                }
            }
        }

        // ── Stats row: total / surveyed / imported ─────────────────────
        MetricPillRow {
            MetricPill(label = "Точек", value = totalPoints.toString(), accent = true)
            MetricPill(label = "Снято", value = surveyedCount.toString())
            MetricPill(label = "Импорт", value = importedCount.toString())
        }

        // ── Quick-action tile row (Карта / Точки / Слои) ───────────────
        SectionLabel("Быстрые действия")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Points is the most-used: accent tile (filled, outlined border).
            TileAccent(
                title = "Точки",
                icon = Icons.Default.Place,
                onClick = onOpenPoints,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
            TileData(
                title = "Слои",
                icon = Icons.Default.Layers,
                onClick = onOpenLayers,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
            TileData(
                title = "СК / DXF",
                icon = Icons.Default.Architecture,
                onClick = onChangeCrs,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
        }

        // ── Full CRS nav card with the human-readable label visible ───
        NewtonNavCard(
            title = "Система координат",
            subtitle = crsLabel,
            leadingIcon = Icons.Default.GpsFixed,
            onClick = onChangeCrs,
        )

        HeightModeCard(
            current = state.project.crsConfig.heightMode,
            onSetHeightMode = onSetHeightMode,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeightModeCard(
    current: HeightMode,
    onSetHeightMode: (HeightMode) -> Unit,
) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Высоты")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = current == HeightMode.ELLIPSOIDAL,
                    onClick = { onSetHeightMode(HeightMode.ELLIPSOIDAL) },
                    label = { Text("Эллипсоидальные") },
                )
                FilterChip(
                    selected = current == HeightMode.ORTHOMETRIC,
                    onClick = { onSetHeightMode(HeightMode.ORTHOMETRIC) },
                    label = { Text("Ортометрические") },
                )
            }
            Text(
                "Ортометрические высоты считаются как H − N, где N (превышение " +
                    "геоида) берётся из GGA приёмника. Применяется к точкам, " +
                    "снятым ПОСЛЕ смены режима; ранее снятые не пересчитываются.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CrsPill(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.W700),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
