package com.nocap.app.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.data.catalog.LocalCatalogRepository
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.Category
import com.nocap.app.domain.model.HomeFeed
import com.nocap.app.domain.repository.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class DiscoverUiState(
    val searchQuery: String = "",
    val selectedCategoryId: String = "all",
    val books: List<CatalogBook> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: CatalogRepository
) : ViewModel() {

    // ── Home feed ────────────────────────────────────────────────────────────

    val homeFeed: StateFlow<HomeFeed> = repository.observeHomeFeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeFeed())

    // ── Discover state ────────────────────────────────────────────────────────

    private val _discoverState = MutableStateFlow(DiscoverUiState())
    val discoverState: StateFlow<DiscoverUiState> = _discoverState

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Debounce search query and react to category selection
    val discoverBooks: StateFlow<List<CatalogBook>> = _discoverState
        .debounce(250)
        .flatMapLatest { state ->
            val query = state.searchQuery.trim()
            val categoryId = state.selectedCategoryId
            when {
                query.isNotBlank() -> repository.searchBooks(query)
                categoryId != "all" -> repository.observeBooksByCategory(categoryId)
                else -> repository.observeBooksByCategory("all")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _discoverState.update { it.copy(searchQuery = query, selectedCategoryId = "all") }
    }

    fun selectCategory(categoryId: String) {
        _discoverState.update {
            it.copy(
                selectedCategoryId = categoryId,
                searchQuery = ""
            )
        }
    }

    fun onCategorySelected(categoryId: String) {
        _discoverState.update {
            it.copy(
                selectedCategoryId = if (it.selectedCategoryId == categoryId) "all" else categoryId,
                searchQuery = ""
            )
        }
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CatalogViewModel(LocalCatalogRepository()) as T
            }
        }
    }
}
