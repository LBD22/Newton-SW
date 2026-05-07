package ru.newton.fieldapp.features.survey.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
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
import ru.newton.fieldapp.core.ui.components.NewtonTile
import ru.newton.fieldapp.features.survey.codesets.CodeSetsScreen
import ru.newton.fieldapp.features.survey.continuous.ContinuousSurveyScreen
import ru.newton.fieldapp.features.survey.defaults.SurveyDefaultsScreen
import ru.newton.fieldapp.features.survey.line.LineSurveyScreen
import ru.newton.fieldapp.features.survey.map.MapSurveyScreen
import ru.newton.fieldapp.features.survey.point.PointSurveyScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutHistoryScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutPickerScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutToLineScreen
import ru.newton.fieldapp.features.survey.stakeout.StakeoutToPointScreen
import ru.newton.fieldapp.features.survey.track.TrackRecordingScreen

const val SURVEY_TAB_ROUTE = "survey"

private const val INDEX_ROUTE = "survey/index"
private const val MAP_ROUTE = "survey/map"
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
        composable(MAP_ROUTE) { MapSurveyScreen(onBack = { navController.popBackStack() }) }
        composable(POINT_ROUTE) { PointSurveyScreen(onBack = { navController.popBackStack() }) }
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
        val tiles = listOf(
            SurveyTile("Карта", Icons.Default.Map) { navController.navigate(MAP_ROUTE) },
            SurveyTile("Точка", Icons.Default.GpsFixed) { navController.navigate(POINT_ROUTE) },
            SurveyTile("Линия", Icons.Default.Route) { navController.navigate(LINE_ROUTE) },
            SurveyTile("Непрерывно", Icons.Default.AllInclusive) { navController.navigate(CONTINUOUS_ROUTE) },
            SurveyTile("Вынос точки", Icons.Default.Straighten) { navController.navigate(STAKEOUT_PICKER_ROUTE) },
            SurveyTile("Вынос линии", Icons.Default.Polyline) { navController.navigate(STAKEOUT_LINE_ROUTE) },
            SurveyTile("История выноса", Icons.Default.History) { navController.navigate(STAKEOUT_HISTORY_ROUTE) },
            SurveyTile("Трек", Icons.Default.Timeline) { navController.navigate(TRACK_ROUTE) },
            SurveyTile("CAD", Icons.Default.Architecture) { navController.navigate("cad/view") },
            SurveyTile("Параметры", Icons.Default.Settings) { navController.navigate(DEFAULTS_ROUTE) },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tiles, key = SurveyTile::title) { tile ->
                NewtonTile(title = tile.title, icon = tile.icon, onClick = tile.onClick)
            }
        }
    }
}

private data class SurveyTile(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
