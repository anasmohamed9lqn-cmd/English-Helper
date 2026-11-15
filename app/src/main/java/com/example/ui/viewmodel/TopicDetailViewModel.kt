package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.TopicEntity
import com.example.data.local.WordEntity
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TopicDetailUiState(
    val topic: TopicEntity? = null,
    val allWords: List<WordEntity> = emptyList(),
    val filteredWords: List<WordEntity> = emptyList(),
    val searchQuery: String = "",
    val prioritizeUnmastered: Boolean = true,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
)

class TopicDetailViewModel(
    private val topicId: Long,
    private val repository: VocabRepository,
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000L)
        }
    }

    val uiState: StateFlow<TopicDetailUiState> = combine(
        repository.observeTopicById(topicId),
        repository.observeWordsForTopic(topicId),
        repository.preferencesFlow,
        searchQueryFlow,
        tickerFlow,
    ) { topic, words, prefs, query, now ->
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            words
        } else {
            words.filter { w ->
                w.word.contains(trimmed, ignoreCase = true) ||
                    w.phonetics.contains(trimmed, ignoreCase = true) ||
                    w.translation.contains(trimmed, ignoreCase = true) ||
                    w.exampleSentence.contains(trimmed, ignoreCase = true)
            }
        }
        TopicDetailUiState(
            topic = topic,
            allWords = words,
            filteredWords = filtered,
            searchQuery = query,
            prioritizeUnmastered = prefs.prioritizeUnmastered,
            currentTimeMillis = now,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TopicDetailUiState(),
    )

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun renameTopic(newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameTopic(topicId, newTitle)
        }
    }

    fun deleteTopic(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTopic(topicId)
            onDeleted()
        }
    }

    fun updateReviewTimer(intervalDays: Int) {
        viewModelScope.launch {
            repository.updateTopicReviewTimer(topicId, intervalDays)
        }
    }

    fun addWord(
        word: String,
        phonetics: String,
        translation: String,
        exampleSentence: String,
    ) {
        if (word.isBlank()) return
        viewModelScope.launch {
            repository.addWord(
                topicId = topicId,
                word = word,
                phonetics = phonetics,
                translation = translation,
                exampleSentence = exampleSentence,
            )
        }
    }

    fun updateWord(word: WordEntity) {
        if (word.word.isBlank()) return
        viewModelScope.launch {
            repository.updateWord(word)
        }
    }

    fun deleteWord(wordId: Long) {
        viewModelScope.launch {
            repository.deleteWord(wordId)
        }
    }

    fun toggleWordMastered(word: WordEntity) {
        viewModelScope.launch {
            repository.setWordMastered(word.id, !word.isMastered)
        }
    }

    fun setPrioritizeUnmastered(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPrioritizeUnmastered(enabled)
        }
    }

    class Factory(
        private val topicId: Long,
        private val repository: VocabRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TopicDetailViewModel(topicId, repository) as T
        }
    }
}
