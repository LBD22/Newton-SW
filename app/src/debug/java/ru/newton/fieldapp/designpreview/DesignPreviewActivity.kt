package ru.newton.fieldapp.designpreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.components.CompassRose
import ru.newton.fieldapp.core.ui.components.EpochProgressRing
import ru.newton.fieldapp.core.ui.components.FieldOutlineButton
import ru.newton.fieldapp.core.ui.components.FieldPrimaryButton
import ru.newton.fieldapp.core.ui.components.FieldTextField
import ru.newton.fieldapp.core.ui.components.FixQuality
import ru.newton.fieldapp.core.ui.components.FormRow
import ru.newton.fieldapp.core.ui.components.ExtendedFieldFab
import ru.newton.fieldapp.core.ui.components.InlineStatus
import ru.newton.fieldapp.core.ui.components.ListTile
import ru.newton.fieldapp.core.ui.components.ListTileIconVariant
import ru.newton.fieldapp.core.ui.components.LiveCard
import ru.newton.fieldapp.core.ui.components.MetricPillRow
import ru.newton.fieldapp.core.ui.components.NowPill
import ru.newton.fieldapp.core.ui.components.PendingBanner
import ru.newton.fieldapp.core.ui.components.RadioGroupCard
import ru.newton.fieldapp.core.ui.components.RadioRow
import ru.newton.fieldapp.core.ui.components.SectionLabel
import ru.newton.fieldapp.core.ui.components.StatusCard
import ru.newton.fieldapp.core.ui.components.SwitchRow
import ru.newton.fieldapp.core.ui.components.TileAccent
import ru.newton.fieldapp.core.ui.components.TileData
import ru.newton.fieldapp.core.ui.components.TileHero
import ru.newton.fieldapp.core.ui.components.TileSystem
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Debug-only activity that renders every Phase 2 + Phase 3 component on one
 * scrollable screen. Launched manually via `adb shell am start -n
 * ru.newton.fieldapp.debug/ru.newton.fieldapp.designpreview.DesignPreviewActivity`
 * — never wired into the normal launcher or NavHost.
 */
class DesignPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewtonTheme {
                DesignPreviewScreen()
            }
        }
    }
}

@Composable
private fun DesignPreviewScreen() {
    val colors = NewtonTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PreviewHeader("Newton · Field Blue · component preview") }

        item { SectionLabel("Status card") }
        item {
            StatusCard(
                fix = FixQuality.RtkFix,
                fixLabel = "RTK Fix",
                metaText = "18 SAT · HDOP 0.8 · 1.2с",
                project = "Объект «К-12 Север»",
                miniCoords = "N 55°45′12.3456″   E 37°37′02.7890″",
            )
        }

        item { SectionLabel("Metric pills") }
        item {
            MetricPillRow {
                MetricPill(label = "SAT", value = "18")
                MetricPill(label = "HDOP", value = "0.8", accent = true)
                MetricPill(label = "AGE", value = "1.2с")
            }
        }

        item { SectionLabel("Tiles (Hero / Accent / Data / System)") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TileHero(
                    title = "Снять\nточку",
                    sub = "RTK · 30 эпох",
                    icon = Icons.Default.AddLocationAlt,
                    onClick = {},
                    modifier = Modifier.weight(2f).aspectRatio(1f),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    TileAccent(title = "Линия", icon = Icons.Default.Timeline, onClick = {})
                    TileAccent(title = "Вынос", icon = Icons.Default.Flag, onClick = {})
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TileData(title = "Карта", icon = Icons.Default.Map, onClick = {}, modifier = Modifier.weight(1f))
                TileData(title = "Точки", icon = Icons.Default.Apps, onClick = {}, badge = "142", modifier = Modifier.weight(1f))
                TileData(title = "CAD", icon = Icons.Default.Architecture, onClick = {}, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TileSystem(title = "Приёмник", icon = Icons.Default.SatelliteAlt, onClick = {}, modifier = Modifier.weight(1f))
                TileSystem(title = "NTRIP", icon = Icons.Default.Cloud, onClick = {}, modifier = Modifier.weight(1f))
                TileSystem(title = "Проекты", icon = Icons.Default.FolderOpen, onClick = {}, modifier = Modifier.weight(1f))
            }
        }

        item { SectionLabel("Compass rose (Stakeout)") }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CompassRose(bearing = 47f)
            }
        }

        item { SectionLabel("Progress ring (Survey averaging)") }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                EpochProgressRing(current = 14, total = 30)
            }
        }

        item { SectionLabel("Live card + Inline status") }
        item {
            LiveCard(
                latitude = "55°45′12.347″",
                longitude = "37°37′02.789″",
                height = "187.231 m",
                sigma = "σ 11 / 16 mm  ·  HRMS / VRMS",
                fixLabel = "RTK Fix",
            )
        }
        item {
            InlineStatus(
                keyword = "RTK Fix",
                segments = listOf("17 SAT", "HDOP 0.9"),
            )
        }

        item { SectionLabel("Now-pill + Section label (with dashed tail)") }
        item {
            NowPill(text = "Активно · Rover / NTRIP")
        }

        item { SectionLabel("Buttons (Primary / Outline / Extended FAB)") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldOutlineButton(
                    text = "Сменить",
                    icon = Icons.Default.SwapHoriz,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                FieldPrimaryButton(
                    text = "Зафиксировать",
                    icon = Icons.Default.PushPin,
                    onClick = {},
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
        item {
            ExtendedFieldFab(
                text = "Измерить точку",
                icon = Icons.Default.Add,
                onClick = {},
            )
        }

        item { SectionLabel("Radio group + Switch row + Form row") }
        item {
            var mode by remember { mutableStateOf("Rover") }
            RadioGroupCard {
                RadioRow(label = "Rover", active = mode == "Rover", onClick = { mode = "Rover" })
                RadioRow(label = "Base", active = mode == "Base", onClick = { mode = "Base" })
                RadioRow(label = "Static (PPK)", active = mode == "Static (PPK)", onClick = { mode = "Static (PPK)" }, showDivider = false)
            }
        }
        item {
            var gpsOn by remember { mutableStateOf(true) }
            var glonassOn by remember { mutableStateOf(true) }
            var bdsOn by remember { mutableStateOf(false) }
            RadioGroupCard {
                SwitchRow(label = "GPS", checked = gpsOn, onCheckedChange = { gpsOn = it })
                SwitchRow(label = "GLONASS", checked = glonassOn, onCheckedChange = { glonassOn = it })
                SwitchRow(label = "BeiDou", checked = bdsOn, onCheckedChange = { bdsOn = it }, showDivider = false)
            }
        }
        item { FormRow(label = "Угол возвышения", value = "15°") }
        item { FormRow(label = "Минимум SNR", value = "35 дБ") }

        item { SectionLabel("List tiles (default + navy variant for hardware)") }
        item {
            ListTile(
                title = "NTRIP · «РИВНЕ-Н»",
                subtitle = "caster.example.ru  ·  mount MAX",
                icon = Icons.Default.CloudDownload,
                onClick = {},
            )
        }
        item {
            ListTile(
                title = "Newton N2 · Pole",
                subtitle = "2.000 м  ·  фаз. центр −0.018 м",
                icon = Icons.Default.Straighten,
                iconVariant = ListTileIconVariant.Navy,
                onClick = {},
            )
        }

        item { SectionLabel("Text field (focused state)") }
        item {
            var pointName by remember { mutableStateOf("P-007") }
            FieldTextField(
                value = pointName,
                onValueChange = { pointName = it },
                label = "Имя точки",
                mono = false,
            )
        }

        item { SectionLabel("Pending banner (settings unsaved)") }
        item {
            PendingBanner(
                pendingCount = 3,
                onApply = {},
            )
        }
    }
}

@Composable
private fun PreviewHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W800),
        color = NewtonTheme.colors.brandDeep,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}
