package com.example.project2.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleType
import com.example.project2.ui.screens.CreateAccountScreen
import com.example.project2.ui.screens.CreatePuzzleScreen
import com.example.project2.ui.screens.DailyChallengePlayScreen
import com.example.project2.ui.screens.DailyChallengeScreen
import com.example.project2.ui.screens.DashboardScreen
import com.example.project2.ui.screens.DailyChallengeGeneratorScreen
import com.example.project2.ui.screens.DifficultySelectionScreen
import com.example.project2.ui.screens.JigsawPuzzleScreen
import com.example.project2.ui.screens.LeaderboardScreen
import com.example.project2.ui.screens.LoginScreen
import com.example.project2.ui.screens.PatternMemoryScreen
import com.example.project2.ui.screens.ProfileScreen
import com.example.project2.ui.screens.PuzzleLibraryScreen
import com.example.project2.ui.screens.PuzzleNotFoundScreen
import com.example.project2.ui.screens.PuzzleNotReadyScreen
import com.example.project2.ui.theme.CharcoalSurface
import com.example.project2.ui.theme.RoyalBluePrimary
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.platform.LocalContext


private const val PUZZLE_ID_ARG = "puzzleId"
private const val GRID_SIZE_ARG = "gridSize"

private const val PUZZLE_PLAY_ROUTE = "puzzlePlay/{$PUZZLE_ID_ARG}"

private const val JIGSAW_DIFFICULTY_ROUTE = "jigsawDifficulty/{$PUZZLE_ID_ARG}"

private const val JIGSAW_PLAY_ROUTE = "jigsawPlay/{$PUZZLE_ID_ARG}/{$GRID_SIZE_ARG}"

private const val DAILY_PLAY_ROUTE = "dailyPlay"
private const val AUTH_LOGIN_ROUTE = "auth_login"
private const val AUTH_CREATE_ROUTE = "auth_create"
private const val AUTH_SECRET_ROUTE = "auth_secret"

private fun buildPuzzlePlayRoute(puzzleId: String): String {
    return PUZZLE_PLAY_ROUTE.replace("{$PUZZLE_ID_ARG}", puzzleId)
}

/**
 * Top-level navigation destinations for the app.
 */
enum class MindMatchDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Dashboard("dashboard", "Home", Icons.Filled.Dashboard),
    Puzzles("puzzles", "Puzzles", Icons.Filled.Psychology),
    Create("create", "Create", Icons.Filled.LibraryAdd),
    Daily("daily", "Daily", Icons.Filled.Schedule),
    Leaderboard("leaderboard", "Leaders", Icons.Filled.Leaderboard),
    Profile("profile", "Profile", Icons.Filled.Person)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMatchApp(
    modifier: Modifier = Modifier,
    providedViewModel: MindMatchViewModel? = null
) {
    val viewModel: MindMatchViewModel = providedViewModel ?: viewModel()
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }

    if (!isAuthenticated) {
        AuthNavHost(
            onAuthenticated = { isAuthenticated = true }
        )
    } else {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val isTopLevelDestination = MindMatchDestination.entries.any { destination ->
            currentDestination.isDestinationInHierarchy(destination)
        }
        val puzzleTitle = when (currentDestination?.route) {
            PUZZLE_PLAY_ROUTE, JIGSAW_DIFFICULTY_ROUTE, JIGSAW_PLAY_ROUTE -> {
                navBackStackEntry?.arguments?.getString(PUZZLE_ID_ARG)?.let { puzzleId ->
                    viewModel.puzzles.firstOrNull { it.id == puzzleId }?.title
                }
            }
            else -> null
        }
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(puzzleTitle ?: textForDestination(currentDestination)) },
                    navigationIcon = {
                        if (!isTopLevelDestination && navController.previousBackStackEntry != null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Navigate back"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CharcoalSurface,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                if (isTopLevelDestination) {
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
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MindMatchDestination.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                val onPlayPuzzle: (PuzzleDescriptor) -> Unit = { puzzle ->
                    if (puzzle.type == PuzzleType.JIGSAW) {
                        navController.navigate("jigsawDifficulty/${puzzle.id}")
                    } else {
                        navController.navigate(buildPuzzlePlayRoute(puzzle.id))
                    }
                }

                composable(MindMatchDestination.Dashboard.route) {
                    DashboardScreen(
                        profile = viewModel.profile,
                        puzzles = viewModel.puzzles,
                        progress = viewModel.progressByPuzzle,
                        dailyChallenge = viewModel.dailyChallenge,
                        onPlayPuzzle = onPlayPuzzle,
                        onViewDailyChallenge = { navController.navigate(DAILY_PLAY_ROUTE) }
                    )
                }
                composable(MindMatchDestination.Puzzles.route) {
                    PuzzleLibraryScreen(
                        puzzles = viewModel.puzzles,
                        progress = viewModel.progressByPuzzle,
                        onPlayPuzzle = onPlayPuzzle
                    )
                }
                composable(MindMatchDestination.Create.route) {
                    CreatePuzzleScreen()
                }
                composable(MindMatchDestination.Daily.route) {
                    DailyChallengeScreen(
                        challenge = viewModel.dailyChallenge,
                        onStartChallenge = {
                            navController.navigate(DAILY_PLAY_ROUTE)
                        }
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
                        profile = viewModel.profile,
                        onLogout = { isAuthenticated = false }
                    )
                }

                composable(
                    route = JIGSAW_DIFFICULTY_ROUTE,
                    arguments = listOf(navArgument(PUZZLE_ID_ARG) { type = NavType.StringType })
                ) { backStackEntry ->
                    val puzzleId = backStackEntry.arguments?.getString(PUZZLE_ID_ARG)
                    DifficultySelectionScreen { selectedGridSize ->
                        navController.navigate("jigsawPlay/$puzzleId/$selectedGridSize")
                    }
                }

                composable(
                    route = JIGSAW_PLAY_ROUTE,
                    arguments = listOf(
                        navArgument(PUZZLE_ID_ARG) { type = NavType.StringType },
                        navArgument(GRID_SIZE_ARG) { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val puzzleId = backStackEntry.arguments?.getString(PUZZLE_ID_ARG)
                    val gridSize = backStackEntry.arguments?.getInt(GRID_SIZE_ARG)
                    val puzzle = puzzleId?.let { id -> viewModel.puzzles.firstOrNull { it.id == id } }

                    if (puzzle != null && gridSize != null) {
                        JigsawPuzzleScreen(
                            puzzle = puzzle,
                            onBack = { navController.popBackStack() },
                            gridSize = gridSize
                        )
                    } else {
                        PuzzleNotFoundScreen()
                    }
                }

                composable(
                    route = PUZZLE_PLAY_ROUTE,
                    arguments = listOf(navArgument(PUZZLE_ID_ARG) { type = NavType.StringType })
                ) { backStackEntry ->
                    val puzzleId = backStackEntry.arguments?.getString(PUZZLE_ID_ARG)
                    val puzzle = puzzleId?.let { id -> viewModel.puzzles.firstOrNull { it.id == id } }
                    val progress = puzzleId?.let { viewModel.progressByPuzzle[it] }

                    when {
                        puzzle == null -> PuzzleNotFoundScreen()
                        puzzle.type == PuzzleType.PATTERN_MEMORY -> PatternMemoryScreen(
                            puzzle = puzzle,
                            progress = progress,
                            onProgressUpdated = { newProgress ->
                                viewModel.saveProgress(newProgress)   // <-- writes to Firebase + updates cache
                            },
                            onBack = { navController.popBackStack() }
                        )
                        else -> PuzzleNotReadyScreen(puzzle = puzzle)
                    }
                }
                composable(DAILY_PLAY_ROUTE) {
                    DailyChallengePlayScreen(
                        challenge = viewModel.dailyChallenge
                    )
                }
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
    NavigationBar(
        containerColor = CharcoalSurface,
        contentColor = Color.White
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Color.White,
            indicatorColor = RoyalBluePrimary,
            unselectedIconColor = Color.White.copy(alpha = 0.6f),
            unselectedTextColor = Color.White.copy(alpha = 0.6f)
        )
        destinations.forEach { destination ->
            val selected = currentDestination.isDestinationInHierarchy(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = itemColors
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

@Composable
private fun AuthNavHost(
    onAuthenticated: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AUTH_LOGIN_ROUTE
    ) {
        composable(AUTH_LOGIN_ROUTE) {
            LoginScreen(

                onLogin = onAuthenticated,
                onCreateAccount = { navController.navigate(AUTH_CREATE_ROUTE) },
                onSecretAccess = { navController.navigate(AUTH_SECRET_ROUTE) }
            )
        }
        composable(AUTH_CREATE_ROUTE) {
            val context = LocalContext.current
            CreateAccountScreen(
                onBackToLogin = { navController.popBackStack() },
                onCreateAccount = {
                    // show toast using a stable UI context
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    }

                    // give toast time to appear before navigating
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (navController.popBackStack().not()) {
                            // fallback in case stack is empty
                            navController.navigate(AUTH_LOGIN_ROUTE)
                        }
                    }, 1200)
                }
            )
        }

        composable(AUTH_SECRET_ROUTE) {
            DailyChallengeGeneratorScreen(
                onBackToLogin = { navController.popBackStack() }
            )
        }
    }
}
