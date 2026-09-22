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

enum class SessionMode(val title: String, val maxItems: Int?, val durationMinutes: Int?) {
    STANDARD("Ôn tập ngắt quãng", null, null),
    QUICK_5("Ôn nhanh 5 phút", 10, 5),
    QUICK_10("Ôn nhanh 10 phút", 20, 10)
}

data class ReviewQueueUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val items: List<ReviewItemWithDetails> = emptyList(),
    val currentIndex: Int = 0,
    val completedCount: Int = 0,
    val isSessionComplete: Boolean = false,
    val sessionMode: SessionMode = SessionMode.STANDARD,
    val remainingSeconds: Int? = null,
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0
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
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        loadDueItems()
    }

    fun loadDueItems(mode: SessionMode = SessionMode.STANDARD) {
        timerJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionMode = mode) }
            val now = System.currentTimeMillis()
            val due = withContext(Dispatchers.IO) {
                reviewRepository.getDueItemsWithDetails(now)
            }
            val filtered = if (mode.maxItems != null) due.take(mode.maxItems) else due
            val initialSeconds = mode.durationMinutes?.let { it * 60 }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSubmitting = false,
                    items = filtered,
                    currentIndex = 0,
                    completedCount = 0,
                    isSessionComplete = filtered.isEmpty(),
                    sessionMode = mode,
                    remainingSeconds = initialSeconds,
                    againCount = 0,
                    hardCount = 0,
                    goodCount = 0,
                    easyCount = 0
                )
            }

            if (initialSeconds != null && filtered.isNotEmpty()) {
                startTimer()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val current = _uiState.value.remainingSeconds ?: break
                if (current <= 1) {
                    _uiState.update { it.copy(remainingSeconds = 0, isSessionComplete = true) }
                    break
                } else {
                    _uiState.update { it.copy(remainingSeconds = current - 1) }
                }
            }
        }
    }

    fun answerCurrent(rating: ReviewRating) {
        if (_uiState.value.isSubmitting) return
        val current = _uiState.value.currentItem ?: return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    reviewRepository.submitReview(current.reviewItem, rating, now)
                }
                val nextIndex = _uiState.value.currentIndex + 1
                val isDone = nextIndex >= _uiState.value.items.size
                if (isDone) {
                    timerJob?.cancel()
                }
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        currentIndex = nextIndex,
                        completedCount = it.completedCount + 1,
                        isSessionComplete = isDone,
                        againCount = it.againCount + if (rating == ReviewRating.AGAIN) 1 else 0,
                        hardCount = it.hardCount + if (rating == ReviewRating.HARD) 1 else 0,
                        goodCount = it.goodCount + if (rating == ReviewRating.GOOD) 1 else 0,
                        easyCount = it.easyCount + if (rating == ReviewRating.EASY) 1 else 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
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
