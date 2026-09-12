package com.nocap.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Trang chủ", Icons.Default.Home)
    data object Discover : Screen("discover?categoryId={categoryId}", "Khám phá", Icons.Default.Search) {
        fun createRoute(categoryId: String? = null) = if (categoryId != null) "discover?categoryId=$categoryId" else "discover"
    }
    data object Library : Screen("library", "Thư viện", Icons.AutoMirrored.Filled.List)
    data object Memory : Screen("memory", "Ghi nhớ", com.nocap.app.core.designsystem.AppIcons.Bookmarks)
    data object Favorites : Screen("favorites", "Yêu thích", Icons.Default.Favorite)
    data object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)
    data object BookDetails : Screen("book_details/{bookId}") {
        fun createRoute(bookId: String) = "book_details/$bookId"
    }
    data object Reader : Screen("reader/{bookId}?locator={locator}") {
        fun createRoute(bookId: String, locatorJson: String? = null): String {
            return if (!locatorJson.isNullOrBlank()) {
                "reader/$bookId?locator=${android.net.Uri.encode(locatorJson)}"
            } else {
                "reader/$bookId"
            }
        }
    }
    data object ReviewQueue : Screen("review_queue")
    data object KnowledgeSearch : Screen("knowledge_search")
    data object GlobalAnnotations : Screen("global_annotations")
    data object ReadingStats : Screen("reading_stats")

    companion object {
        val bottomNavItems = listOf(Home, Discover, Library, Memory, Settings)
    }
}

