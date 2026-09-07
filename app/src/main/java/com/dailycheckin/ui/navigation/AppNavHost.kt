package com.dailycheckin.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dailycheckin.R
import com.dailycheckin.ui.edit.TaskEditScreen
import com.dailycheckin.ui.home.HomeScreen
import com.dailycheckin.ui.settings.SettingsScreen
import com.dailycheckin.ui.template.TemplateScreen

object Routes {
    const val HOME = "home"
    const val TEMPLATE = "template"
    const val SETTINGS = "settings"
    const val EDIT = "edit?taskId={taskId}"

    fun edit(taskId: Long? = null) = "edit?taskId=${taskId ?: -1L}"
}

private data class BottomItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, R.string.tab_home, Icons.Outlined.CheckCircleOutline),
    BottomItem(Routes.TEMPLATE, R.string.tab_template, Icons.Outlined.LibraryAdd),
    BottomItem(Routes.SETTINGS, R.string.tab_settings, Icons.Outlined.Settings)
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in setOf(Routes.HOME, Routes.TEMPLATE, Routes.SETTINGS)) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAddTask = { navController.navigate(Routes.TEMPLATE) },
                    onEditTask = { navController.navigate(Routes.edit(it)) }
                )
            }
            composable(Routes.TEMPLATE) {
                TemplateScreen(
                    onEditTask = { navController.navigate(Routes.edit(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.EDIT,
                arguments = listOf(navArgument("taskId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: -1L
                TaskEditScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
