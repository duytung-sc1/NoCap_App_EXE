package com.ebookreader.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ebookreader.app.presentation.bookdetails.BookDetailsScreen
import com.ebookreader.app.presentation.catalog.DiscoverScreen
import com.ebookreader.app.presentation.favorites.FavoritesScreen
import com.ebookreader.app.presentation.home.HomeScreen
import com.ebookreader.app.presentation.library.LibraryScreen
import com.ebookreader.app.presentation.reader.ReaderScreen
import com.ebookreader.app.presentation.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    val isFullScreen = currentRoute?.startsWith("reader/") == true

    Scaffold(
        bottomBar = {
            if (!isFullScreen && Screen.bottomNavItems.any { it.route == currentRoute }) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    },
                    onContinueReadingClick = { bookId ->
                        navController.navigate(Screen.Reader.createRoute(bookId))
                    }
                )
            }
            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    },
                    onReadBookClick = { bookId ->
                        navController.navigate(Screen.Reader.createRoute(bookId))
                    },
                    onNavigateToDiscover = {
                        navController.navigate(Screen.Discover.route)
                    }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.BookDetails.route,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
                BookDetailsScreen(
                    bookId = bookId,
                    onBackClick = { navController.popBackStack() },
                    onReadClick = { id ->
                        navController.navigate(Screen.Reader.createRoute(id))
                    }
                )
            }
            composable(
                route = Screen.Reader.route,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
                ReaderScreen(
                    bookId = bookId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
