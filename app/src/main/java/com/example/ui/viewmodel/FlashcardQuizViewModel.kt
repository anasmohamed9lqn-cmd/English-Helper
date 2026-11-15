package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.WordEntity
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlashcardQuizUiState(
    val topicTitle: String = "",
    val reviewIntervalDays: Int = 0,
    val cards: List<WordEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isCompleted: Boolean = false,
    val reviewedCardIds: Set<Long> = emptySet(),
    val timerWasReset: Boolean = false,
    val isLoading: Boolean = true,
) {
    val currentCard: WordEntity? get() = cards.getOrNull(currentIndex)
    val masteredInSessionCount: Int get() = cards.count { it.isMastered }
}

class FlashcardQuizViewModel(
    private val topicId: Long,
    private val requestedCount: Int,
    private val prioritizeUnmastered: Boolean,
    private val repository: VocabRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardQuizUiState())
    val uiState: StateFlow<FlashcardQuizUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            val topic = repository.observeTopicById(topicId).first()
            val allWords = repository.getWordsForTopicOnce(topicId)
            val orderedWords = if (prioritizeUnmastered) {
                val unmastered = allWords.filter { !it.isMastered }.shuffled()
                val mastered = allWords.filter { it.isMastered }.shuffled()
                unmastered + mastered
            } else {
                allWords.shuffled()
            }
            val selected = if (requestedCount <= 0 || requestedCount >= orderedWords.size) {
                orderedWords
            } else {
                orderedWords.take(requestedCount)
            }
            val firstId = selected.firstOrNull()?.id
            _uiState.value = FlashcardQuizUiState(
                topicTitle = topic?.title.orEmpty(),
                reviewIntervalDays = topic?.reviewIntervalDays ?: 0,
                cards = selected,
                currentIndex = 0,
                isFlipped = false,
                isCompleted = selected.isEmpty(),
                reviewedCardIds = if (firstId != null) setOf(firstId) else emptySet(),
                timerWasReset = false,
                isLoading = false,
            )
        }
    }

    fun flipCard() {
        _uiState.update { state ->
            if (state.isCompleted || state.cards.isEmpty()) state
            else state.copy(isFlipped = !state.isFlipped)
        }
    }

    fun nextCard() {
        val currentState = _uiState.value
        if (currentState.cards.isEmpty() || currentState.isCompleted) return

        val nextIdx = currentState.currentIndex + 1
        if (nextIdx >= currentState.cards.size) {
            completeQuizSession()
        } else {
            val nextId = currentState.cards[nextIdx].id
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    isFlipped = false,
                    reviewedCardIds = it.reviewedCardIds + nextId,
                )
            }
        }
    }

    fun previousCard() {
        _uiState.update { state ->
            if (state.currentIndex > 0 && !state.isCompleted) {
                state.copy(
                    currentIndex = state.currentIndex - 1,
                    isFlipped = false,
                )
            } else {
                state
            }
        }
    }

    fun toggleMasteredCurrentCard() {
        val card = _uiState.value.currentCard ?: return
        val newMastered = !card.isMastered
        viewModelScope.launch {
            repository.setWordMastered(card.id, newMastered)
            _uiState.update { state ->
                val updatedCards = state.cards.map { item ->
                    if (item.id == card.id) item.copy(isMastered = newMastered) else item
                }
                state.copy(cards = updatedCards)
            }
        }
    }

    private fun completeQuizSession() {
        viewModelScope.launch {
            val hadTimer = _uiState.value.reviewIntervalDays > 0
            repository.resetTopicReviewCycleAfterQuiz(topicId)
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    isFlipped = false,
                    timerWasReset = hadTimer,
                )
            }
        }
    }

    fun restartSession() {
        _uiState.update { it.copy(isLoading = true) }
        loadDeck()
    }

    class Factory(
        private val topicId: Long,
        private val requestedCount: Int,
        private val prioritizeUnmastered: Boolean,
        private val repository: VocabRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FlashcardQuizViewModel(
                topicId = topicId,
                requestedCount = requestedCount,
                prioritizeUnmastered = prioritizeUnmastered,
                repository = repository,
            ) as T
        }
    }
}
