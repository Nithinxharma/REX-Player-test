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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lagradost.cloudstream3.SearchResponse
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.preferences.CSCorePreferences
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.utils.LocalBackStack

@Serializable
class CSSearchScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backStack = LocalBackStack.current
    val focusManager = LocalFocusManager.current
    val pluginCoordinator = koinInject<CloudStreamPluginCoordinator>()
    val preferences = koinInject<CSCorePreferences>()

    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
      CSSearchViewModel(pluginCoordinator, preferences)
    }

    val state by viewModel.uiState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    var searchInput by remember { mutableStateOf("") }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            OutlinedTextField(
              value = searchInput,
              onValueChange = { searchInput = it },
              placeholder = { Text("Search movies, TV shows, anime...") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(
                onSearch = {
                  focusManager.clearFocus()
                  viewModel.search(searchInput)
                }
              ),
              trailingIcon = {
                if (searchInput.isNotEmpty()) {
                  IconButton(onClick = { searchInput = "" }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                  }
                }
              },
              shape = RoundedCornerShape(24.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
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
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      ) {
        // Providers Filter Bar
        val providers = viewModel.availableProviders
        if (providers.isNotEmpty()) {
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            item {
              FilterChip(
                selected = selectedProvider == null,
                onClick = { viewModel.selectProvider(null) },
                label = { Text("All Providers") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              )
            }
            items(providers) { provider ->
              val isSelected = selectedProvider?.name == provider.name
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

        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        ) {
          when (val uiState = state) {
            is CSSearchUiState.Idle -> {
              if (searchHistory.isNotEmpty()) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Recent Searches",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { viewModel.clearHistory() }) {
                      Text("Clear")
                    }
                  }

                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    items(searchHistory) { query ->
                      FilterChip(
                        selected = false,
                        onClick = {
                          searchInput = query
                          viewModel.search(query)
                        },
                        leadingIcon = {
                          Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(query) },
                      )
                    }
                  }
                }
              } else {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center,
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Search,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                      modifier = Modifier.size(64.dp),
                    )
                    Text(
                      text = "Search across CloudStream providers",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }

            is CSSearchUiState.Loading -> {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator()
              }
            }

            is CSSearchUiState.Empty -> {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "No results found for \"${uiState.query}\"",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            is CSSearchUiState.Error -> {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(24.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = uiState.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.error,
                )
              }
            }

            is CSSearchUiState.Results -> {
              LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(uiState.items) { media ->
                  CSMediaGridCard(
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

@Composable
fun CSMediaGridCard(
  media: SearchResponse,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
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
        if (media.apiName.isNotBlank()) {
          Text(
            text = media.apiName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
          )
        }
      }
    }
  }
}
