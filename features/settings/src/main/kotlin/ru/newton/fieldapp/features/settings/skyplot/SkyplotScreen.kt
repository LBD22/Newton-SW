package ru.newton.fieldapp.features.settings.skyplot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import ru.newton.fieldapp.gnss.data.parsers.SatelliteInView
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * SET-201 — sky plot of satellites currently in view.
 *
 * Layout: a stereographic-style polar plot (north up, 0° at the top, 90°
 * elevation at the centre) plus a sortable PRN/Elev/Az/SNR table below.
 * Each constellation gets its own dot colour so the surveyor can tell at
 * a glance whether GPS or Galileo is occluded.
 */
@HiltViewModel
class SkyplotViewModel
    @Inject
    constructor(store: GnssStatusStore) : ViewModel() {
        val satellites: StateFlow<List<SatelliteInView>> = store.status
            .map { it.satellitesInView }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

@Composable
fun SkyplotScreen(
    onBack: () -> Unit,
    viewModel: SkyplotViewModel = hiltViewModel(),
) {
    val sats by viewModel.satellites.collectAsStateWithLifecycle()
    SkyplotContent(satellites = sats, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkyplotContent(
    satellites: List<SatelliteInView>,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Спутники") },
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NewtonSectionLabel("Skyplot")
                        NewtonInfoBadge("${satellites.size} в видимости")
                    }
                    SkyPlotCanvas(satellites)
                }
            }
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Список")
                    if (satellites.isEmpty()) {
                        Text(
                            "GSV-сообщение не получено. Включите вывод GPGSV в " +
                                "настройках сообщений и убедитесь, что приёмник эмиттит его.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("PRN", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("Сист", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("Elev°", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("Az°", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("SNR", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(satellites, key = { "${it.constellation}-${it.prn}" }) { sat ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(sat.prn.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(sat.constellation, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(sat.elevationDeg?.toString() ?: "—", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(sat.azimuthDeg?.toString() ?: "—", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(sat.snrDbHz?.toString() ?: "—", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyPlotCanvas(satellites: List<SatelliteInView>) {
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = min(w, h) / 2f * 0.92f
            val stroke = Stroke(width = 2f)

            // Concentric elevation circles at 0°, 30°, 60°, 90° (centre = zenith).
            for (angle in listOf(0, 30, 60)) {
                val rr = r * (90 - angle) / 90f
                drawCircle(
                    color = outline.copy(alpha = 0.4f),
                    radius = rr,
                    center = Offset(cx, cy),
                    style = stroke,
                )
            }
            drawCircle(color = outline, radius = r, center = Offset(cx, cy), style = stroke)

            // Cardinal cross.
            drawLine(outline.copy(alpha = 0.4f), Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 1f)
            drawLine(outline.copy(alpha = 0.4f), Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1f)

            // Centre pip.
            drawCircle(color = onSurface, radius = 3f, center = Offset(cx, cy))

            satellites.forEach { sat ->
                val elev = sat.elevationDeg ?: return@forEach
                val az = sat.azimuthDeg ?: return@forEach
                if (elev < 0 || elev > 90) return@forEach
                // Project: 0 elev → outer rim, 90 elev → centre. Az measured
                // clockwise from north, so 0=N(up), 90=E(right) on canvas.
                val rho = r * (90 - elev) / 90f
                val θ = Math.toRadians(az.toDouble() - 90)
                val x = cx + (rho * cos(θ)).toFloat()
                val y = cy + (rho * sin(θ)).toFloat()
                val color = colorForConstellation(sat.constellation)
                drawCircle(
                    color = color,
                    radius = 8f + (sat.snrDbHz ?: 0).coerceIn(0, 50) * 0.15f,
                    center = Offset(x, y),
                )
            }
        }
        // Cardinal labels — drawn outside the Canvas via Text, positioned via Box.
        Text(
            "N",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Text(
            "E",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        Text(
            "S",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        Text(
            "W",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

private fun colorForConstellation(c: String): Color = when (c.uppercase()) {
    "GP" -> Color(0xFF2C5BB5) // GPS — blue
    "GL" -> Color(0xFFE53935) // GLONASS — red
    "GA" -> Color(0xFFFB8C00) // Galileo — orange
    "BD", "GB" -> Color(0xFF8E24AA) // BeiDou — purple
    "GN" -> Color(0xFF4CB85C) // combined — green
    else -> Color(0xFF757575)
}
