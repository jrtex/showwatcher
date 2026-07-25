package com.example.showwatcher.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.showwatcher.ui.addshow.AddShowScreen
import com.example.showwatcher.ui.archive.ArchiveScreen
import com.example.showwatcher.ui.archivedetail.ArchiveDetailScreen
import com.example.showwatcher.ui.dashboard.DashboardScreen
import com.example.showwatcher.ui.settings.SettingsScreen
import com.example.showwatcher.ui.showdetail.ShowDetailScreen
import com.example.showwatcher.ui.upcoming.UpcomingScreen

// How close to the left/right screen edge a drag must start to count as a tab-switch swipe,
// and how far it must travel before it commits — mirrors a system edge-swipe gesture rather
// than a drag-anywhere pager, so it doesn't fight with scrolling the show grids.
private val EdgeZoneWidth = 32.dp
private val SwipeThreshold = 64.dp

// Left-to-right tab order, matching the bottom NavigationBar, used both to resolve edge-swipe
// targets and to pick a consistent left/right slide direction for tab transitions.
private val mainTabs = listOf(Destination.Upcoming, Destination.Dashboard, Destination.Archive)

private fun NavDestination?.tabIndex(): Int? = when {
    this == null -> null
    hasRoute(Destination.Upcoming::class) -> 0
    hasRoute(Destination.Dashboard::class) -> 1
    hasRoute(Destination.Archive::class) -> 2
    else -> null
}

// Slides between main tabs in the direction implied by their order; any transition involving a
// non-tab destination (Show Detail, Add Show, Settings, ...) keeps the previous no-animation
// behavior.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterTransition(): EnterTransition {
    val from = initialState.destination.tabIndex()
    val to = targetState.destination.tabIndex()
    return when {
        from != null && to != null && to > from -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        from != null && to != null && to < from -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        else -> EnterTransition.None
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitTransition(): ExitTransition {
    val from = initialState.destination.tabIndex()
    val to = targetState.destination.tabIndex()
    return when {
        from != null && to != null && to > from -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        from != null && to != null && to < from -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        else -> ExitTransition.None
    }
}

@Composable
fun ShowWatcherNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTabIndex = currentDestination.tabIndex()
    val onUpcoming = currentTabIndex == 0
    val onDashboard = currentTabIndex == 1
    val onArchive = currentTabIndex == 2

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
        val density = LocalDensity.current
        val edgeZonePx = with(density) { EdgeZoneWidth.toPx() }
        val swipeThresholdPx = with(density) { SwipeThreshold.toPx() }
        var contentWidthPx by remember { mutableFloatStateOf(0f) }
        // The offset detectHorizontalDragGestures hands to onDragStart is measured *after*
        // Android's touch-slop is consumed, not the raw finger-down point — on a real device
        // that can already be a dozen-plus px away from where the finger actually landed,
        // shrinking the effective edge zone. Track the true down position separately so the
        // edge check reflects where the gesture actually started.
        var rawDownX by remember { mutableFloatStateOf(0f) }

        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard,
            modifier = Modifier
                .padding(padding)
                .onSizeChanged { contentWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        rawDownX = down.position.x
                    }
                }
                .pointerInput(currentTabIndex, edgeZonePx, swipeThresholdPx) {
                    var startedInLeftEdge = false
                    var startedInRightEdge = false
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            startedInLeftEdge = rawDownX <= edgeZonePx
                            startedInRightEdge = rawDownX >= contentWidthPx - edgeZonePx
                            dragTotal = 0f
                        },
                        onDragEnd = {
                            val tabIndex = currentTabIndex
                            val targetIndex = when {
                                tabIndex == null -> null
                                startedInLeftEdge && dragTotal >= swipeThresholdPx -> tabIndex - 1
                                startedInRightEdge && dragTotal <= -swipeThresholdPx -> tabIndex + 1
                                else -> null
                            }
                            if (targetIndex != null && targetIndex in mainTabs.indices) {
                                navigateToTab(mainTabs[targetIndex])
                            }
                            startedInLeftEdge = false
                            startedInRightEdge = false
                            dragTotal = 0f
                        },
                        onDragCancel = {
                            startedInLeftEdge = false
                            startedInRightEdge = false
                            dragTotal = 0f
                        },
                    ) { _, dragAmount ->
                        dragTotal += dragAmount
                    }
                },
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() },
        ) {
            composable<Destination.Upcoming> {
                UpcomingScreen(
                    onShowClick = { navController.navigate(Destination.ShowDetail(it)) },
                    onSettingsClick = { navController.navigate(Destination.Settings) },
                )
            }
            composable<Destination.Dashboard> {
                DashboardScreen(
                    onShowClick = { navController.navigate(Destination.ShowDetail(it)) },
                    onSettingsClick = { navController.navigate(Destination.Settings) },
                )
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
                ArchiveScreen(
                    onShowClick = { navController.navigate(Destination.ArchiveDetail(it)) },
                    onSettingsClick = { navController.navigate(Destination.Settings) },
                )
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
            composable<Destination.Settings> {
                SettingsScreen()
            }
        }
    }
}
