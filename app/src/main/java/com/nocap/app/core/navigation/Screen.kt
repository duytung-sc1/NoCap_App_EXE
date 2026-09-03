package com.nocap.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Discover : Screen("discover?categoryId={categoryId}", "Discover", Icons.Default.Search) {
        fun createRoute(categoryId: String? = null) = if (categoryId != null) "discover?categoryId=$categoryId" else "discover"
    }
    data object Library : Screen("library", "Library", Icons.AutoMirrored.Filled.List)
    data object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object BookDetails : Screen("book_details/{bookId}") {
        fun createRoute(bookId: String) = "book_details/$bookId"
    }
    data object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: String) = "reader/$bookId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Discover, Library, Favorites, Settings)
    }
}
