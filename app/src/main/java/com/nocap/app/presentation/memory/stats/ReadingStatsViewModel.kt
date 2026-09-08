package com.nocap.app.presentation.memory.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.stats.LocalReadingStatsRepository
import com.nocap.app.domain.repository.MostReadBookItem
import com.nocap.app.domain.repository.ReadingStatsRepository
import com.nocap.app.domain.repository.ReadingStatsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReadingStatsUiState(
    val isLoading: Boolean = true,
    val summary: ReadingStatsSummary = ReadingStatsSummary(0, 0, 0, 0, 0),
    val mostReadBooks: List<MostReadBookItem> = emptyList()
)

class ReadingStatsViewModel(
    private val statsRepository: ReadingStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingStatsUiState())
    val uiState: StateFlow<ReadingStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val now = System.currentTimeMillis()
            val summary = withContext(Dispatchers.IO) {
                statsRepository.getStatsSummary(now)
            }
            val mostRead = withContext(Dispatchers.IO) {
                statsRepository.getMostReadBooks(10)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    summary = summary,
                    mostReadBooks = mostRead
                )
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val repo = LocalReadingStatsRepository(db.readingSessionDao(), db.catalogDao())
                return ReadingStatsViewModel(repo) as T
            }
        }
    }
}
