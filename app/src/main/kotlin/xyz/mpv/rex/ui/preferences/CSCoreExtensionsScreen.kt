package xyz.mpv.rex.ui.preferences

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream4.viewmodels.ExtensionsAction
import com.lagradost.cloudstream4.viewmodels.ExtensionsViewModel2
import com.lagradost.cloudstream4.viewmodels.Plugin
import com.lagradost.cloudstream4.viewmodels.PluginStatus
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.utils.LocalBackStack
import kotlin.uuid.ExperimentalUuidApi

@OptIn(InternalAPI::class, ExperimentalUuidApi::class)
@Serializable
object CSCoreExtensionsScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val viewModel = koinInject<ExtensionsViewModel2>()
    val uiState by viewModel.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTvType by remember { mutableStateOf<TvType?>(null) }
    var pluginToUninstall by remember { mutableStateOf<Plugin?>(null) }

    LaunchedEffect(Unit) {
      viewModel.onAction(ExtensionsAction.Init)
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Provider Extensions",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
          actions = {
            IconButton(onClick = { viewModel.onAction(ExtensionsAction.UpdateAllPlugins) }) {
              Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Update all plugins",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          },
        )
      },
    ) { padding ->
      val navBarHeight = LocalNavigationBarHeight.current
      val allPlugins = uiState.plugins.data.values.toList()

      val filteredPlugins = remember(allPlugins, searchQuery, selectedTvType) {
        allPlugins.filter { plugin ->
          val matchesSearch = searchQuery.isBlank() ||
            plugin.info.name.contains(searchQuery, ignoreCase = true) ||
            (plugin.info.description?.contains(searchQuery, ignoreCase = true) == true)
          val matchesType = selectedTvType == null || plugin.info.tvTypes.contains(selectedTvType)
          matchesSearch && matchesType
        }
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        // Search bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = {
            searchQuery = it
            viewModel.onAction(ExtensionsAction.Search(it))
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          placeholder = { Text("Search extensions by name...") },
          leadingIcon = { Icon(Icons.Outlined.Extension, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
        )

        // Category Filter Chips
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            FilterChip(
              selected = selectedTvType == null,
              onClick = { selectedTvType = null },
              label = { Text("All") },
            )
          }
          items(TvType.entries.filter { it != TvType.NSFW }) { type ->
            FilterChip(
              selected = selectedTvType == type,
              onClick = {
                selectedTvType = if (selectedTvType == type) null else type
              },
              label = { Text(type.name) },
            )
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = navBarHeight + 24.dp,
          ),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (filteredPlugins.isEmpty()) {
            item {
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    Icons.Outlined.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline,
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    "No Extensions Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    "Try syncing configured repositories or adjusting your search query.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                  )
                }
              }
            }
          } else {
            items(filteredPlugins, key = { it.uuid.toString() }) { plugin ->
              val isInstalled = plugin.downloadLocation != null

              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                  containerColor = if (isInstalled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                ),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = plugin.info.name,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          color = MaterialTheme.colorScheme.secondaryContainer,
                          shape = RoundedCornerShape(4.dp),
                        ) {
                          Text(
                            text = "v${plugin.info.version}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                          )
                        }
                      }

                      plugin.info.description?.let { desc ->
                        Text(
                          text = desc,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.outline,
                          maxLines = 2,
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Badges row
                  FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    plugin.info.language?.let { lang ->
                      Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                      ) {
                        Text(
                          text = lang.uppercase(),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                        )
                      }
                    }

                    plugin.info.tvTypes.forEach { type ->
                      Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp),
                      ) {
                        Text(
                          text = type.name,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Action Button & Download Progress
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    when (val status = plugin.status) {
                      is PluginStatus.Downloading -> {
                        Column(modifier = Modifier.weight(1f)) {
                          val total = status.total
                          LinearProgressIndicator(
                            progress = {
                              if (total != null && total > 0) {
                                (status.progress.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                              } else 0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                          )
                          Text(
                            text = "Downloading... ${status.progress / 1024} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                          )
                        }
                      }

                      is PluginStatus.Failed -> {
                        Text(
                          text = "Failed: ${status.cause.message}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.weight(1f),
                          maxLines = 1,
                        )
                        Button(
                          onClick = { viewModel.onAction(ExtensionsAction.DownloadPlugin(plugin.uuid)) },
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                          Text("Retry")
                        }
                      }

                      is PluginStatus.Idle -> {
                        if (isInstalled) {
                          Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            "Installed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                          )

                          Spacer(modifier = Modifier.weight(1f))

                          OutlinedButton(
                            onClick = { pluginToUninstall = plugin },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                          ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uninstall")
                          }
                        } else {
                          Spacer(modifier = Modifier.weight(1f))

                          Button(
                            onClick = { viewModel.onAction(ExtensionsAction.DownloadPlugin(plugin.uuid)) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                          ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install")
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

    // Uninstall Confirmation Dialog
    pluginToUninstall?.let { plugin ->
      AlertDialog(
        onDismissRequest = { pluginToUninstall = null },
        title = { Text("Uninstall Extension?") },
        text = {
          Text("Are you sure you want to uninstall '${plugin.info.name}'? Its registered providers will be removed from the registry.")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.onAction(ExtensionsAction.DeletePlugin(plugin.uuid))
              pluginToUninstall = null
              Toast.makeText(context, "Extension uninstalled", Toast.LENGTH_SHORT).show()
            },
          ) {
            Text("Uninstall", color = MaterialTheme.colorScheme.onError)
          }
        },
        dismissButton = {
          TextButton(onClick = { pluginToUninstall = null }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }
  }
}
