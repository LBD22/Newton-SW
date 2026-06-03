package ru.newton.fieldapp.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.data.preferences.OnboardingPreferences
import ru.newton.fieldapp.features.cad.nav.cadGraph
import ru.newton.fieldapp.features.project.nav.PROJECT_TAB_ROUTE
import ru.newton.fieldapp.features.project.nav.projectGraph
import ru.newton.fieldapp.features.settings.nav.SETTINGS_TAB_ROUTE
import ru.newton.fieldapp.features.settings.nav.settingsGraph
import ru.newton.fieldapp.features.survey.nav.SURVEY_TAB_ROUTE
import ru.newton.fieldapp.features.survey.nav.surveyGraph
import ru.newton.fieldapp.onboarding.OnboardingScreen
import javax.inject.Inject

@HiltViewModel
class OnboardingGateViewModel
    @Inject
    constructor(
        preferences: OnboardingPreferences,
    ) : ViewModel() {
        // null = still loading first value; renders nothing until DataStore replies.
        val completed: StateFlow<Boolean?> = preferences.completed
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

/**
 * Root navigation: three tabs (Проект / Настройки / Съёмка) with a persistent
 * GNSS status strip pinned just below the toolbar of every screen.
 *
 * On first run the [OnboardingScreen] takes over the whole UI until the user
 * either completes or skips it (APP-002–005). Once finished, [completed]
 * flips and the wizard never shows again. We let the user dismiss-via-skip
 * instead of forcing a four-step flow on power users.
 */
@Composable
fun NewtonNavHost(gateViewModel: OnboardingGateViewModel = hiltViewModel()) {
    val completed by gateViewModel.completed.collectAsStateWithLifecycle()
    var dismissed by remember { mutableStateOf(false) }

    when {
        completed == null -> Unit // initial DataStore read in flight; brief blank
        completed == false && !dismissed -> OnboardingScreen(onFinished = { dismissed = true })
        else -> MainShell()
    }
}

@Composable
private fun MainShell() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination

            NavigationBar {
                TopTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GnssStatusStrip()
            NavHost(
                navController = navController,
                startDestination = TopTab.PROJECT.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                projectGraph(navController)
                settingsGraph(navController)
                surveyGraph(navController)
                cadGraph(navController)
            }
        }
    }
}

internal enum class TopTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    PROJECT(PROJECT_TAB_ROUTE, "Проект", Icons.Default.Folder),
    SETTINGS(SETTINGS_TAB_ROUTE, "Настройки", Icons.Default.Settings),
    SURVEY(SURVEY_TAB_ROUTE, "Съёмка", Icons.Default.Map),
}
