package com.example.android.interviewassistant.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.android.interviewassistant.domain.AppRepository
import com.example.android.interviewassistant.ui.screens.flashcards.FlashcardsScreen
import com.example.android.interviewassistant.ui.screens.home.HomeScreen
import com.example.android.interviewassistant.ui.screens.interview.InterviewScreen
import com.example.android.interviewassistant.ui.screens.interview.InterviewViewModel
import com.example.android.interviewassistant.ui.screens.onboarding.OnboardingScreen
import com.example.android.interviewassistant.ui.screens.onboarding.OnboardingViewModel
import com.example.android.interviewassistant.ui.screens.progress.ProgressScreen
import com.example.android.interviewassistant.ui.screens.study.StudyScreen
import com.example.android.interviewassistant.ui.screens.study.StudyViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Interview : Screen("interview", "Interview", Icons.Default.Mic)
    object Study : Screen("study", "Study", Icons.Default.MenuBook)
    object Flashcards : Screen("flashcards", "Cards", Icons.Default.Style)
    object Progress : Screen("progress", "Progress", Icons.Default.BarChart)
    object Onboarding : Screen("onboarding", "Setup", Icons.Default.Person)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Interview,
    Screen.Study,
    Screen.Flashcards,
    Screen.Progress
)

@Composable
fun AppNavigation(
    startDestination: String,
    repository: AppRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != Screen.Onboarding.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = navBackStackEntry?.destination?.hierarchy
                                ?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                val vm: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.factory(repository)
                )
                OnboardingScreen(
                    viewModel = vm,
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = repository,
                    onNavigateTo = { screen ->
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Interview.route) {
                val vm: InterviewViewModel = viewModel(
                    factory = InterviewViewModel.factory(repository)
                )
                InterviewScreen(viewModel = vm)
            }
            composable(Screen.Study.route) {
                val vm: StudyViewModel = viewModel(
                    factory = StudyViewModel.factory(repository)
                )
                StudyScreen(viewModel = vm, repository = repository)
            }
            composable(Screen.Flashcards.route) {
                FlashcardsScreen(repository = repository)
            }
            composable(Screen.Progress.route) {
                ProgressScreen(repository = repository)
            }
        }
    }
}
