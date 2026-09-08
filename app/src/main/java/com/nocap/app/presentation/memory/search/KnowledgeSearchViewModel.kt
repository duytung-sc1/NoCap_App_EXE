package com.nocap.app.presentation.memory.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.search.LocalKnowledgeSearchRepository
import com.nocap.app.domain.model.KnowledgeItemType
import com.nocap.app.domain.model.KnowledgeSearchResult
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.repository.KnowledgeSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KnowledgeSearchUiState(
    val query: String = "",
    val typeFilter: KnowledgeItemType = KnowledgeItemType.ALL,
    val formatFilter: PublicationFormat? = null,
    val results: List<KnowledgeSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

class KnowledgeSearchViewModel(
    private val searchRepository: KnowledgeSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeSearchUiState())
    val uiState: StateFlow<KnowledgeSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        triggerSearch(debounceMs = 300)
    }

    fun onTypeFilterChange(type: KnowledgeItemType) {
        _uiState.update { it.copy(typeFilter = type) }
        triggerSearch(debounceMs = 0)
    }

    fun onFormatFilterChange(format: PublicationFormat?) {
        _uiState.update { it.copy(formatFilter = format) }
        triggerSearch(debounceMs = 0)
    }

    fun clearQuery() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList(),
                isSearching = false,
                hasSearched = false
            )
        }
    }

    private fun triggerSearch(debounceMs: Long) {
        searchJob?.cancel()
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false, hasSearched = false) }
            return
        }

        searchJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            _uiState.update { it.copy(isSearching = true) }

            val currentState = _uiState.value
            val res = withContext(Dispatchers.IO) {
                searchRepository.search(
                    query = currentState.query,
                    typeFilter = currentState.typeFilter,
                    formatFilter = currentState.formatFilter
                )
            }

            _uiState.update {
                it.copy(
                    results = res,
                    isSearching = false,
                    hasSearched = true
                )
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val repo = LocalKnowledgeSearchRepository(
                    catalogDao = db.catalogDao(),
                    highlightDao = db.highlightDao(),
                    bookmarkDao = db.bookmarkDao(),
                    downloadDao = db.downloadDao()
                )
                return KnowledgeSearchViewModel(repo) as T
            }
        }
    }
}
