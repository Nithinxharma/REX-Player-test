package xyz.mpv.rex.ui.cloudstream

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
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lagradost.cloudstream3.SearchResponse
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.utils.LocalBackStack

@Serializable
class CSDiscoverScreen(
  val initialProviderName: String? = null,
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backStack = LocalBackStack.current
    val pluginCoordinator = koinInject<CloudStreamPluginCoordinator>()
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
      CSDiscoverViewModel(pluginCoordinator)
    }

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
      viewModel.loadDiscover(initialProviderName)
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Discover",
              fontWeight = FontWeight.Bold,
            )
          },
          navigationIcon = {
            IconButton(onClick = { backStack.removeLastOrNull() }) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
              )
            }
          },
          actions = {
            IconButton(onClick = { backStack.add(CSSearchScreen()) }) {
              Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
              )
            }
            IconButton(onClick = { backStack.add(CSExtensionsScreen()) }) {
              Icon(
                imageVector = Icons.Outlined.Extension,
                contentDescription = "Extensions",
              )
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
          is CSDiscoverUiState.Loading -> {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }

          is CSDiscoverUiState.NoPlugins -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                  )
                  Text(
                    text = "No Extensions Installed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Install CloudStream extensions from repository catalog to discover and browse movies, TV shows, and anime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                  )
                  Button(
                    onClick = { backStack.add(CSExtensionsScreen()) },
                    shape = RoundedCornerShape(12.dp),
                  ) {
                    Text("Manage Extensions & Repos")
                  }
                }
              }
            }
          }

          is CSDiscoverUiState.Error -> {
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
                  text = "Error Loading Content",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = uiState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { viewModel.loadDiscover() }) {
                  Icon(Icons.Outlined.Refresh, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text("Retry")
                }
              }
            }
          }

          is CSDiscoverUiState.Success -> {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(bottom = 24.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              // Provider Selector Row
              item {
                LazyRow(
                  modifier = Modifier.fillMaxWidth(),
                  contentPadding = PaddingValues(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  items(uiState.providers) { provider ->
                    val isSelected = provider.name == uiState.selectedProvider.name
                    FilterChip(
                      selected = isSelected,
                      onClick = { viewModel.selectProvider(provider) },
                      label = { Text(provider.name) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      ),
                    )
                  }
                }
              }

              if (uiState.isLoadingSections) {
                item {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(200.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                  }
                }
              } else if (uiState.sections.isEmpty()) {
                item {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(32.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "No content available from ${uiState.selectedProvider.name}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              } else {
                items(uiState.sections) { section ->
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = section.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    LazyRow(
                      contentPadding = PaddingValues(horizontal = 16.dp),
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      items(section.list) { media ->
                        CSMediaPosterCard(
                          media = media,
                          onClick = {
                            backStack.add(CSDetailScreen(url = media.url, apiName = media.apiName))
                          },
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
  }
}

@Composable
fun CSMediaPosterCard(
  media: SearchResponse,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .width(130.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(2f / 3f)
          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        if (!media.posterUrl.isNullOrBlank()) {
          AsyncImage(
            model = media.posterUrl,
            contentDescription = media.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(36.dp),
          )
        }
      }

      Column(
        modifier = Modifier.padding(8.dp),
      ) {
        Text(
          text = media.name,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 16.sp,
        )
      }
    }
  }
}
