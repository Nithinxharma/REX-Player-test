package xyz.mpv.rex.ui.cloudstream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.preferences.CSCorePreferences

sealed class CSSearchUiState {
  object Idle : CSSearchUiState()
  object Loading : CSSearchUiState()
  data class Results(
    val query: String,
    val items: List<SearchResponse>,
    val providersSearched: Int,
  ) : CSSearchUiState()
  data class Empty(val query: String) : CSSearchUiState()
  data class Error(val message: String) : CSSearchUiState()
}

class CSSearchViewModel(
  private val pluginCoordinator: CloudStreamPluginCoordinator,
  private val cscorePreferences: CSCorePreferences,
) : ViewModel() {

  private val _uiState = MutableStateFlow<CSSearchUiState>(CSSearchUiState.Idle)
  val uiState: StateFlow<CSSearchUiState> = _uiState.asStateFlow()

  private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
  val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

  private val _selectedProvider = MutableStateFlow<MainAPI?>(null)
  val selectedProvider: StateFlow<MainAPI?> = _selectedProvider.asStateFlow()

  val availableProviders: List<MainAPI>
    get() = pluginCoordinator.registeredProviders

  private var searchJob: Job? = null

  init {
    loadHistory()
  }

  fun selectProvider(provider: MainAPI?) {
    _selectedProvider.value = provider
  }

  fun search(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
      _uiState.value = CSSearchUiState.Idle
      return
    }

    addToHistory(trimmed)

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      _uiState.value = CSSearchUiState.Loading

      val targetProviders = _selectedProvider.value?.let { listOf(it) }
        ?: pluginCoordinator.registeredProviders

      if (targetProviders.isEmpty()) {
        _uiState.value = CSSearchUiState.Error("No CloudStream extensions installed. Install extensions first.")
        return@launch
      }

      val allResults = withContext(Dispatchers.IO) {
        val collected = mutableListOf<SearchResponse>()
        for (provider in targetProviders) {
          runCatching {
            val res = provider.search(trimmed) ?: provider.quickSearch(trimmed)
            if (!res.isNullOrEmpty()) {
              collected.addAll(res)
            }
          }
        }
        collected.distinctBy { it.url }
      }

      if (allResults.isEmpty()) {
        _uiState.value = CSSearchUiState.Empty(trimmed)
      } else {
        _uiState.value = CSSearchUiState.Results(
          query = trimmed,
          items = allResults,
          providersSearched = targetProviders.size,
        )
      }
    }
  }

  private fun loadHistory() {
    val saved = cscorePreferences.customSites.get() // reuse or simple storage
    // Keep local list in state
  }

  fun addToHistory(query: String) {
    val current = _searchHistory.value.toMutableList()
    current.remove(query)
    current.add(0, query)
    _searchHistory.value = current.take(10)
  }

  fun clearHistory() {
    _searchHistory.value = emptyList()
  }
}
