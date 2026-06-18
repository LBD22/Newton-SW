package ru.newton.fieldapp.features.survey.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import ru.newton.fieldapp.core.ui.components.TileAccent
import ru.newton.fieldapp.core.ui.components.TileData
import ru.newton.fieldapp.core.ui.components.TileHero
import ru.newton.fieldapp.core.ui.components.TileSystem
import ru.newton.fieldapp.features.survey.codesets.CodeSetsScreen
import ru.newton.fieldapp.features.survey.continuous.ContinuousSurveyScreen
import ru.newton.fieldapp.features.survey.defaults.SurveyDefaultsScreen
import ru.newton.fieldapp.features.survey.line.LineSurveyScreen
import ru.newton.fieldapp.features.survey.map.MapSurveyScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutHistoryScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutPickerScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutToLineScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutToPointScreen
import ru.newton.fieldapp.features.survey.track.TrackRecordingScreen

const val SURVEY_TAB_ROUTE = "survey"

private const val INDEX_ROUTE = "survey/index"
private const val POINT_ROUTE = "survey/point"
private const val LINE_ROUTE = "survey/line"
private const val CONTINUOUS_ROUTE = "survey/continuous"
private const val DEFAULTS_ROUTE = "survey/defaults"
private const val STAKEOUT_PICKER_ROUTE = "survey/stakeout/picker"
private const val STAKEOUT_TARGET_PATTERN = "survey/stakeout/{targetId}"
private const val STAKEOUT_LINE_ROUTE = "survey/stakeout/line"
private const val STAKEOUT_HISTORY_ROUTE = "survey/stakeout/history"
private const val TRACK_ROUTE = "survey/track"

private fun stakeoutTargetRoute(id: Long) = "survey/stakeout/$id"

fun NavGraphBuilder.surveyGraph(navController: NavController) {
    navigation(startDestination = INDEX_ROUTE, route = SURVEY_TAB_ROUTE) {
        composable(INDEX_ROUTE) { SurveyIndexScreen(navController) }
        // Unified map-centric capture screen (absorbs the old standalone «Карта»):
        // map background + surveyed points + epoch-averaging capture + options.
        composable(POINT_ROUTE) {
            MapSurveyScreen(
                onBack = { navController.popBackStack() },
                onOpenOptions = { navController.navigate(DEFAULTS_ROUTE) },
            )
        }
        composable(LINE_ROUTE) { LineSurveyScreen(onBack = { navController.popBackStack() }) }
        composable(CONTINUOUS_ROUTE) { ContinuousSurveyScreen(onBack = { navController.popBackStack() }) }
        composable("survey/codesets") { CodeSetsScreen(onBack = { navController.popBackStack() }) }
        composable(DEFAULTS_ROUTE) { SurveyDefaultsScreen(onBack = { navController.popBackStack() }) }
        composable(STAKEOUT_PICKER_ROUTE) {
            StakeoutPickerScreen(
                onBack = { navController.popBackStack() },
                onPick = { id -> navController.navigate(stakeoutTargetRoute(id)) },
            )
        }
        composable(
            route = STAKEOUT_TARGET_PATTERN,
            arguments = listOf(navArgument("targetId") { type = NavType.LongType }),
        ) {
            StakeoutToPointScreen(onBack = { navController.popBackStack() })
        }
        composable(STAKEOUT_LINE_ROUTE) {
            StakeoutToLineScreen(onBack = { navController.popBackStack() })
        }
        composable(STAKEOUT_HISTORY_ROUTE) {
            StakeoutHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(TRACK_ROUTE) {
            TrackRecordingScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveyIndexScreen(navController: NavController) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Съёмка") },
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Hero "Снять точку" 2×2 + two accent tiles stacked to the right
            // — primary survey action gets focal weight; map/line are the
            // next most-used.
            // Hero "Снять точку" now opens the map-centric capture screen
            // (map + surveyed points + averaging) — the old separate «Карта»
            // tile is gone, folded into this one.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TileHero(
                    title = "Снять\nточку",
                    sub = "Карта + усреднение",
                    icon = Icons.Default.AddLocationAlt,
                    onClick = { navController.navigate(POINT_ROUTE) },
                    modifier = Modifier.weight(2f).aspectRatio(1f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TileAccent(
                        title = "Линия",
                        icon = Icons.Default.Route,
                        onClick = { navController.navigate(LINE_ROUTE) },
                        modifier = Modifier.aspectRatio(1f),
                    )
                    TileAccent(
                        title = "Непрерывно",
                        icon = Icons.Default.AllInclusive,
                        onClick = { navController.navigate(CONTINUOUS_ROUTE) },
                        modifier = Modifier.aspectRatio(1f),
                    )
                }
            }

            // Data row — operational views over collected data
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TileData(
                    title = "Вынос точки",
                    icon = Icons.Default.Straighten,
                    onClick = { navController.navigate(STAKEOUT_PICKER_ROUTE) },
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
                TileData(
                    title = "Вынос линии",
                    icon = Icons.Default.Polyline,
                    onClick = { navController.navigate(STAKEOUT_LINE_ROUTE) },
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            // System row — history, track recording, CAD, params
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TileSystem(
                    title = "История",
                    icon = Icons.Default.History,
                    onClick = { navController.navigate(STAKEOUT_HISTORY_ROUTE) },
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
                TileSystem(
                    title = "Трек",
                    icon = Icons.Default.Timeline,
                    onClick = { navController.navigate(TRACK_ROUTE) },
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
                TileSystem(
                    title = "CAD",
                    icon = Icons.Default.Architecture,
                    onClick = { navController.navigate("cad/view") },
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
            }

            // Tail row — odd-one parameters tile + spacer to keep grid alignment
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TileSystem(
                    title = "Параметры",
                    icon = Icons.Default.Settings,
                    onClick = { navController.navigate(DEFAULTS_ROUTE) },
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
                Spacer(modifier = Modifier.weight(2f))
            }
        }
    }
}

private data class SurveyTile(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
