package xyz.mpv.rex.ui.cloudstream

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.player.PlayerActivity
import xyz.mpv.rex.ui.utils.LocalBackStack

@Serializable
class CSDetailScreen(
  val url: String,
  val apiName: String,
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val pluginCoordinator = koinInject<CloudStreamPluginCoordinator>()

    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
      CSDetailViewModel(url, apiName, pluginCoordinator)
    }

    val state by viewModel.uiState.collectAsState()
    val isResolvingStream by viewModel.isResolvingStream.collectAsState()

    var sourcesDialogData by remember { mutableStateOf<Pair<String, List<ExtractorLink>>?>(null) }

    LaunchedEffect(Unit) {
      viewModel.playbackEvents.collect { event ->
        when (event) {
          is CSPlaybackEvent.PlayStream -> {
            sourcesDialogData = null
            val intent = Intent(context, PlayerActivity::class.java).apply {
              data = Uri.parse(event.url)
              putExtra("title", event.title)
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
          }

          is CSPlaybackEvent.ShowSources -> {
            sourcesDialogData = event.title to event.sources
          }

          is CSPlaybackEvent.ExtractionFailed -> {
            Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
          }
        }
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = (state as? CSDetailUiState.Success)?.loadResponse?.name ?: "Details",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          navigationIcon = {
            IconButton(onClick = { backStack.removeLastOrNull() }) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        )
      },
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      ) {
        when (val uiState = state) {
          is CSDetailUiState.Loading -> {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }

          is CSDetailUiState.Error -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                Text(
                  text = "Error Loading Details",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = uiState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { viewModel.loadDetails() }) {
                  Text("Retry")
                }
              }
            }
          }

          is CSDetailUiState.Success -> {
            val response = uiState.loadResponse
            val isMovie = response is MovieLoadResponse

            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              // Header Row: Poster + Info
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .width(130.dp)
                      .aspectRatio(2f / 3f)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                  ) {
                    if (!response.posterUrl.isNullOrBlank()) {
                      AsyncImage(
                        model = response.posterUrl,
                        contentDescription = response.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                      )
                    } else {
                      Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                      )
                    }
                  }

                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = response.name,
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                    )

                    Text(
                      text = "Provider: ${uiState.provider.name}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.SemiBold,
                    )

                    response.year?.let { year ->
                      Text(
                        text = "Year: $year",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }

                    response.rating?.let { rating ->
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.Star,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(16.dp),
                        )
                        Text(
                          text = String.format("%.1f", rating / 1000f),
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.Medium,
                        )
                      }
                    }

                    if (isMovie) {
                      Spacer(Modifier.height(8.dp))
                      Button(
                        onClick = { viewModel.playMovie(response as MovieLoadResponse, uiState.provider) },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isResolvingStream,
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play Movie")
                      }
                    }
                  }
                }
              }

              // Plot / Synopsis
              response.plot?.let { plot ->
                if (plot.isNotBlank()) {
                  item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = plot,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                      )
                    }
                  }
                }
              }

              // Seasons & Episodes Section for Series
              if (!isMovie && uiState.episodes.isNotEmpty()) {
                if (uiState.seasons.size > 1) {
                  item {
                    LazyRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      items(uiState.seasons) { season ->
                        FilterChip(
                          selected = uiState.selectedSeason == season,
                          onClick = { viewModel.selectSeason(season) },
                          label = { Text("Season $season") },
                          colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                          ),
                        )
                      }
                    }
                  }
                }

                item {
                  Text(
                    text = "Episodes (${uiState.episodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }

                val filteredEpisodes = if (uiState.selectedSeason != null) {
                  uiState.episodes.filter { it.season == null || it.season == uiState.selectedSeason }
                } else {
                  uiState.episodes
                }

                items(filteredEpisodes) { episode ->
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable(enabled = !isResolvingStream) {
                        viewModel.playEpisode(episode, uiState.provider)
                      },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                      )

                      Column(modifier = Modifier.weight(1f)) {
                        val epNumber = episode.episode?.let { "EP $it - " } ?: ""
                        val epTitle = episode.name ?: "Episode ${episode.episode ?: 1}"
                        Text(
                          text = "$epNumber$epTitle",
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        episode.description?.let { desc ->
                          if (desc.isNotBlank()) {
                            Text(
                              text = desc,
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              maxLines = 2,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Resolving Stream Loading Overlay
        if (isResolvingStream) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
          ) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
              ),
            ) {
              Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Text(
                  text = "Resolving streaming sources...",
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        }

        // Multiple Sources Selector Dialog
        sourcesDialogData?.let { (title, sources) ->
          AlertDialog(
            onDismissRequest = { sourcesDialogData = null },
            title = { Text("Select Stream Quality / Mirror") },
            text = {
              LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                items(sources) { source ->
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        viewModel.selectSource(title, source)
                      },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column {
                        Text(
                          text = source.name,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.SemiBold,
                        )
                        val qualityLabel = if (source.quality > 0) "${source.quality}p" else "Auto"
                        Text(
                          text = "Quality: $qualityLabel",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                      Icon(Icons.Outlined.PlayArrow, contentDescription = "Play")
                    }
                  }
                }
              }
            },
            confirmButton = {},
            dismissButton = {
              TextButton(onClick = { sourcesDialogData = null }) {
                Text("Cancel")
              }
            },
          )
        }
      }
    }
  }
}
