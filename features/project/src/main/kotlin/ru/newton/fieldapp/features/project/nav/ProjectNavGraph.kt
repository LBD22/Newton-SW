package ru.newton.fieldapp.features.project.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import ru.newton.fieldapp.features.project.addpoint.AddPointScreen
import ru.newton.fieldapp.features.project.calibration.CalibrationScreen
import ru.newton.fieldapp.features.project.changecrs.ChangeCrsScreen
import ru.newton.fieldapp.features.project.create.CreateProjectScreen
import ru.newton.fieldapp.features.project.details.ProjectDetailsScreen
import ru.newton.fieldapp.features.project.layers.LayersScreen
import ru.newton.fieldapp.features.project.list.ProjectListScreen
import ru.newton.fieldapp.features.project.pointdetail.PointDetailScreen
import ru.newton.fieldapp.features.project.points.PointsListScreen

/**
 * Navigation graph for the «Проект» tab.
 *
 * Public entry point [PROJECT_TAB_ROUTE] matches what `:app/NewtonNavHost` uses
 * for the bottom-navigation tab. Internal destinations are private to this module.
 */
const val PROJECT_TAB_ROUTE = "project"

private const val LIST_ROUTE = "project/list"
private const val CREATE_ROUTE = "project/create"
private const val DETAILS_ROUTE_PATTERN = "project/details/{projectId}"
private const val ADD_POINT_ROUTE_PATTERN = "project/details/{projectId}/add-point"
private const val CHANGE_CRS_ROUTE_PATTERN = "project/details/{projectId}/change-crs"
private const val POINTS_LIST_ROUTE_PATTERN = "project/details/{projectId}/points"
private const val POINT_DETAIL_ROUTE_PATTERN = "project/details/{projectId}/point/{pointId}"
private const val LAYERS_ROUTE_PATTERN = "project/details/{projectId}/layers"
private const val CALIBRATION_ROUTE = "project/calibration"

private fun detailsRoute(projectId: Long) = "project/details/$projectId"
private fun addPointRoute(projectId: Long) = "project/details/$projectId/add-point"
private fun changeCrsRoute(projectId: Long) = "project/details/$projectId/change-crs"
private fun pointsListRoute(projectId: Long) = "project/details/$projectId/points"
private fun pointDetailRoute(projectId: Long, pointId: Long) =
    "project/details/$projectId/point/$pointId"
private fun layersRoute(projectId: Long) = "project/details/$projectId/layers"

fun NavGraphBuilder.projectGraph(navController: NavController) {
    navigation(startDestination = LIST_ROUTE, route = PROJECT_TAB_ROUTE) {
        composable(LIST_ROUTE) {
            ProjectListScreen(
                onCreateProject = { navController.navigate(CREATE_ROUTE) },
                onOpenProject = { id -> navController.navigate(detailsRoute(id)) },
                onOpenPoints = { id -> navController.navigate(pointsListRoute(id)) },
                onOpenCalibration = { navController.navigate(CALIBRATION_ROUTE) },
            )
        }
        composable(CALIBRATION_ROUTE) {
            CalibrationScreen(onBack = { navController.popBackStack() })
        }
        composable(CREATE_ROUTE) {
            CreateProjectScreen(
                onCancel = { navController.popBackStack() },
                onCreated = { id ->
                    // Pop the create screen, then push details.
                    navController.popBackStack(LIST_ROUTE, inclusive = false)
                    navController.navigate(detailsRoute(id))
                },
            )
        }
        composable(
            route = DETAILS_ROUTE_PATTERN,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) { entry ->
            val projectId = entry.arguments?.getLong("projectId") ?: return@composable
            ProjectDetailsScreen(
                onBack = { navController.popBackStack() },
                onChangeCrs = { navController.navigate(changeCrsRoute(projectId)) },
                onOpenPoints = { navController.navigate(pointsListRoute(projectId)) },
                onOpenLayers = { navController.navigate(layersRoute(projectId)) },
            )
        }
        composable(
            route = LAYERS_ROUTE_PATTERN,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
            LayersScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = POINTS_LIST_ROUTE_PATTERN,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) { entry ->
            val projectId = entry.arguments?.getLong("projectId") ?: return@composable
            PointsListScreen(
                onBack = { navController.popBackStack() },
                onAddPoint = { navController.navigate(addPointRoute(projectId)) },
                onOpenPoint = { pointId ->
                    navController.navigate(pointDetailRoute(projectId, pointId))
                },
            )
        }
        composable(
            route = ADD_POINT_ROUTE_PATTERN,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
            AddPointScreen(
                onCancel = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = CHANGE_CRS_ROUTE_PATTERN,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
            ChangeCrsScreen(
                onBack = { navController.popBackStack() },
                onApplied = { navController.popBackStack() },
            )
        }
        composable(
            route = POINT_DETAIL_ROUTE_PATTERN,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("pointId") { type = NavType.LongType },
            ),
        ) {
            PointDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
