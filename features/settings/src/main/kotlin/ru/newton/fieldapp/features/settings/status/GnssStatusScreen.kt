package ru.newton.fieldapp.features.settings.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.theme.LocalFixStatusColors
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * SET-200/202 — detailed GNSS / RTK status. Both screens read the same
 * [GnssStatusStore]; SET-200 emphasises position + DOPs + sat counts, SET-202
 * focuses on the RTK-specific fields (correction age, fix quality, σH/σV).
 *
 * Lives in :features:settings because surveyors expect to find diagnostics
 * under "Настройки" rather than "Съёмка". The persistent strip at the top of
 * the app gives at-a-glance numbers; this screen is for troubleshooting.
 */
@HiltViewModel
class GnssStatusViewModel
    @Inject
    constructor(store: GnssStatusStore) : ViewModel() {
        val status: StateFlow<GnssStatus> = store.status
    }

@Composable
fun GnssStatusScreen(
    onBack: () -> Unit,
    viewModel: GnssStatusViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    StatusContent(title = "GNSS-статус", status = status, mode = StatusMode.Gnss, onBack = onBack)
}

@Composable
fun RtkStatusScreen(
    onBack: () -> Unit,
    viewModel: GnssStatusViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    StatusContent(title = "RTK-статус", status = status, mode = StatusMode.Rtk, onBack = onBack)
}

private enum class StatusMode { Gnss, Rtk }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusContent(
    title: String,
    status: GnssStatus,
    mode: StatusMode,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FixCard(status)
            when (mode) {
                StatusMode.Gnss -> {
                    PositionCard(status)
                    DopCard(status)
                    SatsCard(status)
                }
                StatusMode.Rtk -> {
                    AccuracyCard(status)
                    CorrectionsCard(status)
                    PositionCard(status)
                }
            }
            UpdatedAt(status)
        }
    }
}

@Composable
private fun FixCard(status: GnssStatus) {
    val fixColors = LocalFixStatusColors.current
    val (label, tint) = when (status.fix) {
        FixQuality.NoFix -> "нет фикса" to fixColors.noFix
        FixQuality.Single -> "Single" to fixColors.single
        FixQuality.DGnss -> "DGNSS" to fixColors.float
        FixQuality.FloatRtk -> "Float RTK" to fixColors.float
        FixQuality.FixedRtk -> "Fixed RTK" to fixColors.fixed
        is FixQuality.Ppp -> "PPP" to fixColors.float
    }
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("Текущий фикс")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.headlineSmall, color = tint)
                NewtonInfoBadge("${status.satsUsed} / ${status.satsVisible}")
            }
        }
    }
}

@Composable
private fun PositionCard(status: GnssStatus) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("Позиция (WGS-84)")
            FieldRow("φ", status.latitude?.let { "%.7f°".format(it) } ?: "—")
            FieldRow("λ", status.longitude?.let { "%.7f°".format(it) } ?: "—")
            FieldRow("h", status.ellipsoidalHeight?.let { "%.3f м".format(it) } ?: "—")
            if (status.n != null && status.e != null) {
                FieldRow("N", "%.3f м".format(status.n))
                FieldRow("E", "%.3f м".format(status.e))
                status.h?.let { FieldRow("H", "%.3f м".format(it)) }
            }
        }
    }
}

@Composable
private fun DopCard(status: GnssStatus) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("DOP")
            FieldRow("HDOP", status.hdop?.let { "%.2f".format(it) } ?: "—")
            FieldRow("VDOP", status.vdop?.let { "%.2f".format(it) } ?: "—")
            FieldRow("PDOP", status.pdop?.let { "%.2f".format(it) } ?: "—")
        }
    }
}

@Composable
private fun SatsCard(status: GnssStatus) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("Спутники")
            FieldRow("В решении", status.satsUsed.toString())
            FieldRow("Видимо", status.satsVisible.toString())
        }
    }
}

@Composable
private fun AccuracyCard(status: GnssStatus) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("Точность (GST)")
            FieldRow("σN", status.sigmaN?.let { "%.3f м".format(it) } ?: "—")
            FieldRow("σE", status.sigmaE?.let { "%.3f м".format(it) } ?: "—")
            FieldRow("σH", status.sigmaH?.let { "%.3f м".format(it) } ?: "—")
        }
    }
}

@Composable
private fun CorrectionsCard(status: GnssStatus) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NewtonSectionLabel("Поправки RTCM")
            FieldRow(
                "Возраст",
                status.correctionAgeSec?.let { "%.1f с".format(it) } ?: "—",
            )
        }
    }
}

@Composable
private fun UpdatedAt(status: GnssStatus) {
    if (status.timestampUtc == 0L) return
    Text(
        "Обновлено: ${
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(status.timestampUtc))
        }",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FieldRow(label: String, value: String) {
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
