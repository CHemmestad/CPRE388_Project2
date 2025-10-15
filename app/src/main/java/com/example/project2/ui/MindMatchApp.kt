package com.example.project2.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.ui.screens.CreatePuzzleScreen
import com.example.project2.ui.screens.DailyChallengeScreen
import com.example.project2.ui.screens.DashboardScreen
import com.example.project2.ui.screens.LeaderboardScreen
import com.example.project2.ui.screens.ProfileScreen
import com.example.project2.ui.screens.PuzzleLibraryScreen

/**
 * Top-level navigation destinations for the app.
 */
enum class MindMatchDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Dashboard("dashboard", "Dashboard", Icons.Filled.Dashboard),
    Puzzles("puzzles", "Puzzles", Icons.Filled.Psychology),
    Create("create", "Create", Icons.Filled.LibraryAdd),
    Daily("daily", "Daily", Icons.Filled.Schedule),
    Leaderboard("leaderboard", "Leaders", Icons.Filled.Leaderboard),
    Profile("profile", "Profile", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMatchApp(
    modifier: Modifier = Modifier,
    viewModel: MindMatchViewModel = MindMatchViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(textForDestination(currentDestination)) }
            )
        },
        bottomBar = {
            MindMatchNavigationBar(
                destinations = MindMatchDestination.entries.toTypedArray(),
                currentDestination = currentDestination,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MindMatchDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MindMatchDestination.Dashboard.route) {
                DashboardScreen(
                    profile = viewModel.profile,
                    puzzles = viewModel.puzzles,
                    progress = viewModel.progressByPuzzle,
                    dailyChallenge = viewModel.dailyChallenge
                )
            }
            composable(MindMatchDestination.Puzzles.route) {
                PuzzleLibraryScreen(
                    puzzles = viewModel.puzzles,
                    progress = viewModel.progressByPuzzle
                )
            }
            composable(MindMatchDestination.Create.route) {
                CreatePuzzleScreen()
            }
            composable(MindMatchDestination.Daily.route) {
                DailyChallengeScreen(
                    challenge = viewModel.dailyChallenge
                )
            }
            composable(MindMatchDestination.Leaderboard.route) {
                LeaderboardScreen(
                    leaderboard = viewModel.leaderboard,
                    puzzles = viewModel.puzzles.associateBy(PuzzleDescriptor::id)
                )
            }
            composable(MindMatchDestination.Profile.route) {
                ProfileScreen(
                    profile = viewModel.profile
                )
            }
        }
    }
}

@Composable
private fun MindMatchNavigationBar(
    destinations: Array<MindMatchDestination>,
    currentDestination: NavDestination?,
    onNavigate: (MindMatchDestination) -> Unit
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestination.isDestinationInHierarchy(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors()
            )
        }
    }
}

@Composable
private fun NavDestination?.isDestinationInHierarchy(destination: MindMatchDestination): Boolean {
    return this?.hierarchy?.any { it.route == destination.route } == true
}

private fun textForDestination(destination: NavDestination?): String {
    val fallback = MindMatchDestination.Dashboard.label
    return destination?.route?.let { route ->
        MindMatchDestination.entries.find { it.route == route }?.label
    } ?: fallback
}
