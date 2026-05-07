package ru.newton.fieldapp.features.settings.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LabelImportant
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import ru.newton.fieldapp.features.settings.about.AboutScreen
import ru.newton.fieldapp.features.settings.apply.ApplyScreen
import ru.newton.fieldapp.features.settings.bluetooth.BluetoothConnectScreen
import ru.newton.fieldapp.features.settings.bridge.BridgeScreen
import ru.newton.fieldapp.features.settings.btio.BluetoothIoScreen
import ru.newton.fieldapp.features.settings.correction.CorrectionSourceScreen
import ru.newton.fieldapp.features.settings.gsm.GsmScreen
import ru.newton.fieldapp.features.settings.ntrip.NtripProfileEditScreen
import ru.newton.fieldapp.features.settings.ntrip.NtripProfileListScreen
import ru.newton.fieldapp.features.settings.output.OutputConfigScreen
import ru.newton.fieldapp.features.settings.ppp.PppScreen
import ru.newton.fieldapp.features.settings.rover.RoverSettingsScreen
import ru.newton.fieldapp.features.settings.skyplot.SkyplotScreen
import ru.newton.fieldapp.features.settings.staticobs.StaticObsScreen
import ru.newton.fieldapp.features.settings.status.GnssStatusScreen
import ru.newton.fieldapp.features.settings.status.RtkStatusScreen
import ru.newton.fieldapp.features.settings.tiles.OfflineTilesScreen
import ru.newton.fieldapp.features.settings.units.UnitsScreen

const val SETTINGS_TAB_ROUTE = "settings"

private const val INDEX_ROUTE = "settings/index"
private const val BLUETOOTH_ROUTE = "settings/bluetooth"
private const val ROVER_ROUTE = "settings/rover"
private const val APPLY_ROUTE = "settings/apply"
private const val CORRECTION_ROUTE = "settings/correction"
private const val NTRIP_LIST_ROUTE = "settings/ntrip/list"
private const val NTRIP_EDIT_PATTERN = "settings/ntrip/edit/{profileId}"
private const val OUTPUT_ROUTE = "settings/output"
private const val UNITS_ROUTE = "settings/units"
private const val TILES_ROUTE = "settings/tiles"
private const val GNSS_STATUS_ROUTE = "settings/status/gnss"
private const val RTK_STATUS_ROUTE = "settings/status/rtk"
private const val SKYPLOT_ROUTE = "settings/skyplot"
private const val PPP_ROUTE = "settings/ppp"
private const val GSM_ROUTE = "settings/gsm"
private const val BTIO_ROUTE = "settings/btio"
private const val BRIDGE_ROUTE = "settings/bridge"
private const val STATIC_OBS_ROUTE = "settings/static"
private const val ABOUT_ROUTE = "settings/about"

private fun ntripEditRoute(id: Long) = "settings/ntrip/edit/$id"

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    navigation(startDestination = INDEX_ROUTE, route = SETTINGS_TAB_ROUTE) {
        composable(INDEX_ROUTE) { SettingsIndexScreen(navController) }
        composable(BLUETOOTH_ROUTE) {
            BluetoothConnectScreen(onBack = { navController.popBackStack() })
        }
        composable(ROVER_ROUTE) { RoverSettingsScreen(onBack = { navController.popBackStack() }) }
        composable(APPLY_ROUTE) { ApplyScreen(onBack = { navController.popBackStack() }) }
        composable(CORRECTION_ROUTE) {
            CorrectionSourceScreen(
                onBack = { navController.popBackStack() },
                onManageProfiles = { navController.navigate(NTRIP_LIST_ROUTE) },
            )
        }
        composable(NTRIP_LIST_ROUTE) {
            NtripProfileListScreen(
                onBack = { navController.popBackStack() },
                onCreate = { navController.navigate(ntripEditRoute(0L)) },
                onEdit = { id -> navController.navigate(ntripEditRoute(id)) },
            )
        }
        composable(
            route = NTRIP_EDIT_PATTERN,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
        ) {
            NtripProfileEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(OUTPUT_ROUTE) { OutputConfigScreen(onBack = { navController.popBackStack() }) }
        composable(UNITS_ROUTE) { UnitsScreen(onBack = { navController.popBackStack() }) }
        composable(TILES_ROUTE) { OfflineTilesScreen(onBack = { navController.popBackStack() }) }
        composable(GNSS_STATUS_ROUTE) { GnssStatusScreen(onBack = { navController.popBackStack() }) }
        composable(RTK_STATUS_ROUTE) { RtkStatusScreen(onBack = { navController.popBackStack() }) }
        composable(SKYPLOT_ROUTE) { SkyplotScreen(onBack = { navController.popBackStack() }) }
        composable(PPP_ROUTE) { PppScreen(onBack = { navController.popBackStack() }) }
        composable(GSM_ROUTE) { GsmScreen(onBack = { navController.popBackStack() }) }
        composable(BTIO_ROUTE) { BluetoothIoScreen(onBack = { navController.popBackStack() }) }
        composable(BRIDGE_ROUTE) { BridgeScreen(onBack = { navController.popBackStack() }) }
        composable(STATIC_OBS_ROUTE) { StaticObsScreen(onBack = { navController.popBackStack() }) }
        composable(ABOUT_ROUTE) { AboutScreen(onBack = { navController.popBackStack() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsIndexScreen(navController: NavController) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        val tiles = listOf(
            SettingsTile("Bluetooth", Icons.Default.Bluetooth) { navController.navigate(BLUETOOTH_ROUTE) },
            SettingsTile("Поправки", Icons.Default.CloudDownload) { navController.navigate(CORRECTION_ROUTE) },
            SettingsTile("Ровер", Icons.Default.GpsFixed) { navController.navigate(ROVER_ROUTE) },
            SettingsTile("Сообщения", Icons.Default.SettingsInputAntenna) { navController.navigate(OUTPUT_ROUTE) },
            SettingsTile("GNSS-статус", Icons.Default.SatelliteAlt) { navController.navigate(GNSS_STATUS_ROUTE) },
            SettingsTile("RTK-статус", Icons.Default.SignalCellularAlt) { navController.navigate(RTK_STATUS_ROUTE) },
            SettingsTile("Skyplot", Icons.Default.SatelliteAlt) { navController.navigate(SKYPLOT_ROUTE) },
            SettingsTile("PPP / SBAS", Icons.Default.PrivacyTip) { navController.navigate(PPP_ROUTE) },
            SettingsTile("GSM", Icons.Default.NetworkCell) { navController.navigate(GSM_ROUTE) },
            SettingsTile("BT I/O", Icons.Default.BluetoothConnected) { navController.navigate(BTIO_ROUTE) },
            SettingsTile("Bridge", Icons.Default.CallMerge) { navController.navigate(BRIDGE_ROUTE) },
            SettingsTile("Запись NMEA", Icons.Default.FiberManualRecord) { navController.navigate(STATIC_OBS_ROUTE) },
            SettingsTile("Наборы кодов", Icons.Default.LabelImportant) { navController.navigate("survey/codesets") },
            SettingsTile("Применение", Icons.Default.CheckCircle) { navController.navigate(APPLY_ROUTE) },
            SettingsTile("Единицы", Icons.Default.Straighten) { navController.navigate(UNITS_ROUTE) },
            SettingsTile("Офлайн-карты", Icons.Default.Layers) { navController.navigate(TILES_ROUTE) },
            SettingsTile("О приложении", Icons.Default.Info) { navController.navigate(ABOUT_ROUTE) },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tiles, key = SettingsTile::title) { tile ->
                NewtonTile(title = tile.title, icon = tile.icon, onClick = tile.onClick)
            }
        }
    }
}

private data class SettingsTile(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
