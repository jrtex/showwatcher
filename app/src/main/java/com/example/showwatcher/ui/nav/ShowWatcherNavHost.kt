package com.example.showwatcher.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.showwatcher.ui.addshow.AddShowScreen
import com.example.showwatcher.ui.archive.ArchiveScreen
import com.example.showwatcher.ui.archivedetail.ArchiveDetailScreen
import com.example.showwatcher.ui.dashboard.DashboardScreen
import com.example.showwatcher.ui.showdetail.ShowDetailScreen
import com.example.showwatcher.ui.upcoming.UpcomingScreen

@Composable
fun ShowWatcherNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onUpcoming = currentDestination?.hasRoute(Destination.Upcoming::class) == true
    val onDashboard = currentDestination?.hasRoute(Destination.Dashboard::class) == true
    val onArchive = currentDestination?.hasRoute(Destination.Archive::class) == true

    fun navigateToTab(destination: Destination) {
        // Unconditionally clear the whole back stack (pop up to the graph root, inclusive) before
        // pushing the tab, rather than the saveState/restoreState singleTop dance, which silently
        // no-op'd when the target destination was already in the back stack but not on top.
        // Popping up to a specific named destination (e.g. Dashboard) only fixes the trip back
        // from a screen reached through Dashboard; popping up to the graph's own id clears
        // everything symmetrically regardless of which tab you're switching from.
        navController.navigate(destination) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            // Always visible, including from Show Detail/Archive Detail/Add Show, so there's
            // always an obvious way back to Active/Archive without relying on system back.
            NavigationBar {
                NavigationBarItem(
                    selected = onUpcoming,
                    onClick = { navigateToTab(Destination.Upcoming) },
                    icon = {},
                    label = { Text("Upcoming") },
                )
                NavigationBarItem(
                    selected = onDashboard,
                    onClick = { navigateToTab(Destination.Dashboard) },
                    icon = {},
                    label = { Text("Active") },
                )
                NavigationBarItem(
                    selected = onArchive,
                    onClick = { navigateToTab(Destination.Archive) },
                    icon = {},
                    label = { Text("Archive") },
                )
            }
        },
        floatingActionButton = {
            if (onDashboard || onUpcoming) {
                FloatingActionButton(onClick = { navController.navigate(Destination.AddShow) }) {
                    Text("+")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable<Destination.Upcoming> {
                UpcomingScreen(onShowClick = { navController.navigate(Destination.ShowDetail(it)) })
            }
            composable<Destination.Dashboard> {
                DashboardScreen(onShowClick = { navController.navigate(Destination.ShowDetail(it)) })
            }
            composable<Destination.AddShow> {
                AddShowScreen(
                    onShowAdded = { showId ->
                        navController.navigate(Destination.ShowDetail(showId)) {
                            popUpTo(Destination.Dashboard)
                        }
                    },
                )
            }
            composable<Destination.ShowDetail> {
                ShowDetailScreen(onDeleted = { navController.popBackStack() })
            }
            composable<Destination.Archive> {
                ArchiveScreen(onShowClick = { navController.navigate(Destination.ArchiveDetail(it)) })
            }
            composable<Destination.ArchiveDetail> {
                ArchiveDetailScreen(
                    onReactivated = { showId ->
                        navController.navigate(Destination.ShowDetail(showId)) {
                            popUpTo<Destination.ArchiveDetail> { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
