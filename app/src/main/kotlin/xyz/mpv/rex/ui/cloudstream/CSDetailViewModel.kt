package xyz.mpv.rex.ui.cloudstream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator

sealed class CSDetailUiState {
  object Loading : CSDetailUiState()
  data class Success(
    val loadResponse: LoadResponse,
    val provider: MainAPI,
    val episodes: List<Episode>,
    val selectedSeason: Int? = null,
    val seasons: List<Int> = emptyList(),
  ) : CSDetailUiState()
  data class Error(val message: String) : CSDetailUiState()
}

sealed class CSPlaybackEvent {
  data class PlayStream(
    val title: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
  ) : CSPlaybackEvent()
  data class ShowSources(
    val title: String,
    val sources: List<ExtractorLink>,
    val subtitles: List<SubtitleFile>,
  ) : CSPlaybackEvent()
  data class ExtractionFailed(val message: String) : CSPlaybackEvent()
}

class CSDetailViewModel(
  private val url: String,
  private val apiName: String,
  private val pluginCoordinator: CloudStreamPluginCoordinator,
) : ViewModel() {

  private val _uiState = MutableStateFlow<CSDetailUiState>(CSDetailUiState.Loading)
  val uiState: StateFlow<CSDetailUiState> = _uiState.asStateFlow()

  private val _playbackEvents = MutableSharedFlow<CSPlaybackEvent>()
  val playbackEvents: SharedFlow<CSPlaybackEvent> = _playbackEvents.asSharedFlow()

  private val _isResolvingStream = MutableStateFlow(false)
  val isResolvingStream: StateFlow<Boolean> = _isResolvingStream.asStateFlow()

  init {
    loadDetails()
  }

  fun loadDetails() {
    viewModelScope.launch {
      _uiState.value = CSDetailUiState.Loading

      val provider = pluginCoordinator.getProviderByName(apiName)
        ?: pluginCoordinator.getProviderByUrl(url)

      if (provider == null) {
        _uiState.value = CSDetailUiState.Error("Provider '$apiName' is not currently loaded. Check installed extensions.")
        return@launch
      }

      val response = withContext(Dispatchers.IO) {
        runCatching { provider.load(url) }.getOrNull()
      }

      if (response == null) {
        _uiState.value = CSDetailUiState.Error("Failed to load details from provider.")
        return@launch
      }

      val episodes = when (response) {
        is TvSeriesLoadResponse -> response.episodes
        is AnimeLoadResponse -> response.episodes.values.flatten()
        else -> emptyList()
      }

      val seasons = episodes.mapNotNull { it.season }.distinct().sorted()
      val initialSeason = seasons.firstOrNull()

      _uiState.value = CSDetailUiState.Success(
        loadResponse = response,
        provider = provider,
        episodes = episodes,
        selectedSeason = initialSeason,
        seasons = seasons,
      )
    }
  }

  fun selectSeason(season: Int) {
    val current = _uiState.value as? CSDetailUiState.Success ?: return
    _uiState.value = current.copy(selectedSeason = season)
  }

  fun playMovie(movie: MovieLoadResponse, provider: MainAPI) {
    resolveAndPlay(movie.dataUrl, movie.name, provider)
  }

  fun playEpisode(episode: Episode, provider: MainAPI) {
    val title = episode.name ?: "Episode ${episode.episode ?: 1}"
    resolveAndPlay(episode.data, title, provider)
  }

  private fun resolveAndPlay(data: String, title: String, provider: MainAPI) {
    viewModelScope.launch {
      _isResolvingStream.value = true

      val extractedLinks = mutableListOf<ExtractorLink>()
      val extractedSubs = mutableListOf<SubtitleFile>()

      withContext(Dispatchers.IO) {
        runCatching {
          provider.loadLinks(
            data = data,
            isCasting = false,
            subtitleCallback = { sub -> extractedSubs.add(sub) },
            callback = { link -> extractedLinks.add(link) },
          )
        }
      }

      _isResolvingStream.value = false

      if (extractedLinks.isEmpty()) {
        _playbackEvents.emit(CSPlaybackEvent.ExtractionFailed("No playable video streams found for this source."))
      } else if (extractedLinks.size == 1) {
        val link = extractedLinks.first()
        val headers = mutableMapOf<String, String>()
        if (link.referer.isNotBlank()) {
          headers["Referer"] = link.referer
        }
        _playbackEvents.emit(CSPlaybackEvent.PlayStream(title, link.url, headers))
      } else {
        _playbackEvents.emit(CSPlaybackEvent.ShowSources(title, extractedLinks, extractedSubs))
      }
    }
  }

  fun selectSource(title: String, link: ExtractorLink) {
    viewModelScope.launch {
      val headers = mutableMapOf<String, String>()
      if (link.referer.isNotBlank()) {
        headers["Referer"] = link.referer
      }
      _playbackEvents.emit(CSPlaybackEvent.PlayStream(title, link.url, headers))
    }
  }
}
