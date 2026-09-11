package com.nocap.app.core.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.nocap.app.core.localization.Text
import com.nocap.app.core.localization.localize
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        Screen.bottomNavItems.forEach { screen ->
            val targetRoute = if (screen == Screen.Discover) Screen.Discover.createRoute() else screen.route
            val isSelected = currentRoute == screen.route || (screen == Screen.Discover && currentRoute?.startsWith("discover") == true)
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    screen.icon?.let {
                        Icon(imageVector = it, contentDescription = screen.title?.let { title -> localize(title) })
                    }
                },
                label = {
                    screen.title?.let { Text(text = it) }
                }
            )
        }
    }
}
