package xyz.mpv.rex.ui.cloudstream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator

sealed class CSDiscoverUiState {
  object Loading : CSDiscoverUiState()
  object NoPlugins : CSDiscoverUiState()
  data class Success(
    val providers: List<MainAPI>,
    val selectedProvider: MainAPI,
    val sections: List<HomePageList>,
    val isLoadingSections: Boolean = false,
  ) : CSDiscoverUiState()
  data class Error(val message: String, val providers: List<MainAPI> = emptyList()) : CSDiscoverUiState()
}

class CSDiscoverViewModel(
  private val pluginCoordinator: CloudStreamPluginCoordinator,
) : ViewModel() {

  private val _uiState = MutableStateFlow<CSDiscoverUiState>(CSDiscoverUiState.Loading)
  val uiState: StateFlow<CSDiscoverUiState> = _uiState.asStateFlow()

  fun loadDiscover(preferredProviderName: String? = null) {
    viewModelScope.launch {
      _uiState.value = CSDiscoverUiState.Loading

      // Ensure plugins are initialized
      if (!pluginCoordinator.isInitialized) {
        pluginCoordinator.initialize()
      }

      val allProviders = pluginCoordinator.registeredProviders
      if (allProviders.isEmpty()) {
        _uiState.value = CSDiscoverUiState.NoPlugins
        return@launch
      }

      // Filter providers that support main page discovery
      val discoverableProviders = allProviders.filter { it.hasMainPage }
      val targetProvider = if (preferredProviderName != null) {
        discoverableProviders.firstOrNull { it.name == preferredProviderName }
          ?: allProviders.firstOrNull { it.name == preferredProviderName }
          ?: discoverableProviders.firstOrNull()
          ?: allProviders.first()
      } else {
        discoverableProviders.firstOrNull() ?: allProviders.first()
      }

      loadSectionsForProvider(targetProvider, discoverableProviders.ifEmpty { allProviders })
    }
  }

  fun selectProvider(provider: MainAPI) {
    val currentState = _uiState.value
    val providers = when (currentState) {
      is CSDiscoverUiState.Success -> currentState.providers
      is CSDiscoverUiState.Error -> currentState.providers
      else -> pluginCoordinator.registeredProviders
    }
    viewModelScope.launch {
      loadSectionsForProvider(provider, providers)
    }
  }

  private suspend fun loadSectionsForProvider(provider: MainAPI, availableProviders: List<MainAPI>) {
    _uiState.value = CSDiscoverUiState.Success(
      providers = availableProviders,
      selectedProvider = provider,
      sections = emptyList(),
      isLoadingSections = true,
    )

    val sections = withContext(Dispatchers.IO) {
      runCatching {
        val lists = mutableListOf<HomePageList>()
        // Attempt to load provider main page lists
        if (provider.mainPage.isNotEmpty()) {
          for (data in provider.mainPage.take(5)) {
            runCatching {
              val pageResponse = provider.getMainPage(1, MainPageRequest(data.name, data.data, data.horizontalImages))
              if (pageResponse != null && pageResponse.items.isNotEmpty()) {
                lists.addAll(pageResponse.items.filter { it.list.isNotEmpty() })
              }
            }
          }
        } else {
          // General main page request
          val pageResponse = provider.getMainPage(1, MainPageRequest("Home", provider.mainUrl, false))
          if (pageResponse != null && pageResponse.items.isNotEmpty()) {
            lists.addAll(pageResponse.items.filter { it.list.isNotEmpty() })
          }
        }

        // If provider returned nothing from main page, attempt a fallback default query
        if (lists.isEmpty()) {
          val fallbackResults = runCatching { provider.search("") ?: provider.search("a") }.getOrNull()
          if (!fallbackResults.isNullOrEmpty()) {
            lists.add(HomePageList("Featured", fallbackResults))
          }
        }

        lists
      }.getOrElse { emptyList() }
    }

    if (sections.isEmpty()) {
      _uiState.value = CSDiscoverUiState.Success(
        providers = availableProviders,
        selectedProvider = provider,
        sections = emptyList(),
        isLoadingSections = false,
      )
    } else {
      _uiState.value = CSDiscoverUiState.Success(
        providers = availableProviders,
        selectedProvider = provider,
        sections = sections,
        isLoadingSections = false,
      )
    }
  }
}
