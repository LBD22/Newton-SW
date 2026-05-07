package ru.newton.fieldapp.features.cad.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.newton.fieldapp.features.cad.dxf.DxfImportScreen
import ru.newton.fieldapp.features.cad.view.CadViewScreen

/**
 * CAD-001 + CAD-002 nav graph. Composed at the [NavGraphBuilder] root so
 * other feature modules can navigate to CAD by route string without
 * pulling `:features:cad` as a direct dependency — the architecture rule
 * "no feature ↔ feature" stays intact.
 */
const val CAD_VIEW_ROUTE = "cad/view"
const val DXF_IMPORT_ROUTE = "cad/import"

fun NavGraphBuilder.cadGraph(navController: NavController) {
    composable(CAD_VIEW_ROUTE) {
        CadViewScreen(
            onBack = { navController.popBackStack() },
            onImportDxf = { navController.navigate(DXF_IMPORT_ROUTE) },
        )
    }
    composable(DXF_IMPORT_ROUTE) {
        DxfImportScreen(onBack = { navController.popBackStack() })
    }
}
