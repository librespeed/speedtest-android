package org.librespeed.speedtest.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.librespeed.speedtest.R
import org.librespeed.speedtest.ui.history.HistoryScreen
import org.librespeed.speedtest.ui.result.ResultScreen
import org.librespeed.speedtest.ui.servers.ServersScreen
import org.librespeed.speedtest.ui.settings.SettingsScreen
import org.librespeed.speedtest.ui.speedtest.SpeedtestScreen
import org.librespeed.speedtest.ui.speedtest.SpeedtestViewModel

/** Fold crease position in window coordinates (px), for tabletop layouts. */
data class HingeBounds(val top: Int, val bottom: Int)

enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    SPEEDTEST("speedtest", R.string.nav_speedtest, Icons.Filled.Speed),
    HISTORY("history", R.string.nav_history, Icons.Filled.History),
    SERVERS("servers", R.string.nav_servers, Icons.Filled.Public),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings)
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun App(windowWidth: WindowWidthSizeClass = WindowWidthSizeClass.Compact, hinge: HingeBounds? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val speedtestViewModel: SpeedtestViewModel = viewModel()
    val useRail = windowWidth != WindowWidthSizeClass.Compact
    val onResultScreen = currentRoute?.startsWith("result/") == true ||
        currentRoute?.startsWith("testdetails/") == true ||
        currentRoute?.startsWith("share/") == true ||
        currentRoute == "compare" || currentRoute == "licenses"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!useRail && !onResultScreen) {
                NavigationBar {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateTo(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.label)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (useRail && !onResultScreen) {
                NavigationRail {
                    Destination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateTo(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.label)) }
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = Destination.SPEEDTEST.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Destination.SPEEDTEST.route) {
                    SpeedtestScreen(
                        viewModel = speedtestViewModel,
                        onServersClick = { navController.navigateTo(Destination.SERVERS) },
                        onSettingsClick = { navController.navigateTo(Destination.SETTINGS) },
                        onResult = { id -> navController.navigate("result/$id") },
                        hinge = hinge
                    )
                }
                composable(Destination.HISTORY.route) {
                    HistoryScreen(onOpen = { id -> navController.navigate("result/$id") })
                }
                composable(Destination.SERVERS.route) {
                    ServersScreen(speedtestViewModel, onCompareClick = { navController.navigate("compare") })
                }
                composable("compare") {
                    org.librespeed.speedtest.ui.servers.CompareScreen(
                        speedtestViewModel = speedtestViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Destination.SETTINGS.route) {
                    val uiState by speedtestViewModel.state.collectAsStateWithLifecycle()
                    SettingsScreen(
                        serverLabel = uiState.selectedServer?.name,
                        serverPinned = uiState.pinnedServer,
                        serverCount = uiState.servers.size,
                        onServersClick = { navController.navigateTo(Destination.SERVERS) },
                        onLicensesClick = { navController.navigate("licenses") }
                    )
                }
                composable("licenses") {
                    org.librespeed.speedtest.ui.settings.LicensesScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "result/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    ResultScreen(
                        entryId = entry.arguments?.getLong("id") ?: 0L,
                        onBack = { navController.popBackStack() },
                        onTestAgain = { serverName ->
                            navController.navigateTo(Destination.SPEEDTEST)
                            speedtestViewModel.testAgain(serverName)
                        },
                        onTestDetails = { id -> navController.navigate("testdetails/$id") },
                        onShare = { id -> navController.navigate("share/$id") }
                    )
                }
                composable(
                    route = "testdetails/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    org.librespeed.speedtest.ui.result.TestDetailsScreen(
                        entryId = entry.arguments?.getLong("id") ?: 0L,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "share/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    org.librespeed.speedtest.ui.result.ShareScreen(
                        entryId = entry.arguments?.getLong("id") ?: 0L,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateTo(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
