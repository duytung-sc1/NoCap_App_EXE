package com.nocap.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.importer.LocalImportBookRepository
import com.nocap.app.domain.model.PublicationSource
import com.nocap.app.domain.repository.ImportException
import com.nocap.app.presentation.bookdetails.BookDetailsScreen
import com.nocap.app.presentation.catalog.DiscoverScreen
import com.nocap.app.presentation.favorites.FavoritesScreen
import com.nocap.app.presentation.home.HomeScreen
import com.nocap.app.presentation.library.LibraryScreen
import com.nocap.app.presentation.reader.ReaderScreen
import com.nocap.app.presentation.settings.SettingsScreen

@Composable
fun AppNavHost(
    pendingImportSource: PublicationSource? = null,
    onClearPendingImport: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    var isImportingShared by remember { mutableStateOf(false) }

    LaunchedEffect(pendingImportSource) {
        val source = pendingImportSource ?: return@LaunchedEffect
        isImportingShared = true
        val db = AppDatabase.getInstance(context)
        val importRepo = LocalImportBookRepository(
            context = context,
            catalogDao = db.catalogDao(),
            downloadDao = db.downloadDao(),
            progressDao = db.progressDao(),
            bookmarkDao = db.bookmarkDao(),
            favoriteDao = db.favoriteDao()
        )
        val result = importRepo.importPublication(source)
        isImportingShared = false
        onClearPendingImport()

        result.onSuccess { bookId ->
            navController.navigate(Screen.Reader.createRoute(bookId))
        }.onFailure { error ->
            if (error is ImportException.DuplicateBook) {
                navController.navigate(Screen.Reader.createRoute(error.existingBookId))
            } else {
                navController.navigate(Screen.Library.route)
            }
        }
    }

    val isFullScreen = currentRoute?.startsWith("reader/") == true

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!isFullScreen && Screen.bottomNavItems.any { it.route == currentRoute || (it == Screen.Discover && currentRoute?.startsWith("discover") == true) }) {
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
                        },
                        onCategoryClick = { categoryId ->
                            navController.navigate(Screen.Discover.createRoute(categoryId))
                        }
                    )
                }
                composable(
                    route = Screen.Discover.route,
                    arguments = listOf(
                        navArgument("categoryId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getString("categoryId")
                    DiscoverScreen(
                        initialCategoryId = categoryId,
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
                            navController.navigate(Screen.Discover.createRoute())
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

        if (isImportingShared) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Đang xử lý tệp chia sẻ...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
