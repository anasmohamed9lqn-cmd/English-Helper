package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.ExportResult
import com.example.data.backup.ImportResult
import com.example.data.local.TopicWithStats
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val topics: List<TopicWithStats> = emptyList(),
    val dueTopics: List<TopicWithStats> = emptyList(),
    val totalTopicsCount: Int = 0,
    val totalWordsCount: Int = 0,
    val dueReviewsCount: Int = 0,
    val searchQuery: String = "",
    val languageCode: String = "en",
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: VocabRepository,
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000L)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTopicsWithStats(System.currentTimeMillis()),
        repository.observeTotalWordCount(),
        repository.preferencesFlow,
        searchQueryFlow,
        tickerFlow,
    ) { allTopics, totalWords, prefs, query, now ->
        val dueList = allTopics.filter { item ->
            val nextAt = item.topic.nextReviewAt
            item.topic.reviewIntervalDays > 0 && nextAt != null && nextAt <= now
        }
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            allTopics
        } else {
            allTopics.filter { it.topic.title.contains(trimmed, ignoreCase = true) }
        }
        HomeUiState(
            topics = filtered,
            dueTopics = dueList,
            totalTopicsCount = allTopics.size,
            totalWordsCount = totalWords,
            dueReviewsCount = dueList.size,
            searchQuery = query,
            languageCode = prefs.languageCode,
            currentTimeMillis = now,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState(),
    )

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            repository.toggleLanguage()
        }
    }

    fun createTopic(title: String, reviewIntervalDays: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createTopic(title, reviewIntervalDays)
        }
    }

    fun renameTopic(topicId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameTopic(topicId, newTitle)
        }
    }

    fun deleteTopic(topicId: Long) {
        viewModelScope.launch {
            repository.deleteTopic(topicId)
        }
    }

    suspend fun exportBackup(): ExportResult = repository.exportBackup()

    suspend fun importBackup(rawJson: String): Result<ImportResult> =
        repository.importBackup(rawJson)

    class Factory(private val repository: VocabRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
