package com.nocap.app.presentation.memory.review

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.core.database.dao.ReviewItemWithDetails
import com.nocap.app.data.review.LocalReviewRepository
import com.nocap.app.domain.repository.ReviewRepository
import com.nocap.app.domain.review.ReviewRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewQueueUiState(
    val isLoading: Boolean = true,
    val items: List<ReviewItemWithDetails> = emptyList(),
    val currentIndex: Int = 0,
    val completedCount: Int = 0,
    val isSessionComplete: Boolean = false
) {
    val currentItem: ReviewItemWithDetails?
        get() = items.getOrNull(currentIndex)
    val totalCount: Int
        get() = items.size
}

class ReviewQueueViewModel(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewQueueUiState())
    val uiState: StateFlow<ReviewQueueUiState> = _uiState.asStateFlow()

    init {
        loadDueItems()
    }

    fun loadDueItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val now = System.currentTimeMillis()
            val due = withContext(Dispatchers.IO) {
                reviewRepository.getDueItemsWithDetails(now)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    items = due,
                    currentIndex = 0,
                    completedCount = 0,
                    isSessionComplete = due.isEmpty()
                )
            }
        }
    }

    fun answerCurrent(rating: ReviewRating) {
        val current = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                reviewRepository.submitReview(current.reviewItem, rating, now)
            }
            val nextIndex = _uiState.value.currentIndex + 1
            val isDone = nextIndex >= _uiState.value.items.size
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    completedCount = it.completedCount + 1,
                    isSessionComplete = isDone
                )
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context)
                val repo = LocalReviewRepository(db.reviewDao())
                return ReviewQueueViewModel(repo) as T
            }
        }
    }
}
