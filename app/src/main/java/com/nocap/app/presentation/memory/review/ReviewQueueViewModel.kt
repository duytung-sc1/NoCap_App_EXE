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
    QUICK_10("Ôn nhanh 10 phút", 20, 10),
    ALL("Ôn tập tất cả thẻ", null, null)
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
    val easyCount: Int = 0,
    val errorMessage: String? = null,
    val timedOut: Boolean = false
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
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadDueItems()
    }

    fun loadDueItems(mode: SessionMode = SessionMode.STANDARD) {
        loadJob?.cancel()
        timerJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, items = emptyList(), sessionMode = mode, errorMessage = null, timedOut = false) }
            try {
                val now = System.currentTimeMillis()
                val due = withContext(Dispatchers.IO) {
                    if (mode == SessionMode.ALL) {
                        reviewRepository.getAllReviewItemsWithDetails()
                    } else {
                        reviewRepository.getDueItemsWithDetails(now)
                    }
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
                        easyCount = 0,
                        errorMessage = null,
                        timedOut = false
                    )
                }

                if (initialSeconds != null && filtered.isNotEmpty()) {
                    startTimer()
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSessionComplete = false, errorMessage = "Không tải được danh sách ôn tập. Hãy thử lại.")
                }
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
                    _uiState.update { it.copy(remainingSeconds = 0, isSessionComplete = true, timedOut = true) }
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
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val result = withContext(Dispatchers.IO) {
                    reviewRepository.submitReview(current.reviewItem, rating, now)
                }
                result.getOrThrow()
                val nextIndex = _uiState.value.currentIndex + 1
                val isDone = nextIndex >= _uiState.value.items.size ||
                    _uiState.value.remainingSeconds == 0 || _uiState.value.isSessionComplete
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
            } catch (_: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "Chưa lưu được kết quả ôn tập. Hãy thử lại.") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
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
