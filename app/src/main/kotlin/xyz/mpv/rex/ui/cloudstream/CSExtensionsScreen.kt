package xyz.mpv.rex.ui.cloudstream

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.preferences.CSCorePreferences
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.utils.LocalBackStack

@Serializable
class CSExtensionsScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val pluginCoordinator = koinInject<CloudStreamPluginCoordinator>()
    val preferences = koinInject<CSCorePreferences>()

    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
      CSExtensionsViewModel(pluginCoordinator, preferences)
    }

    val installedPlugins by viewModel.installedPlugins.collectAsState()
    val repositories by viewModel.repositories.collectAsState()
    val availablePlugins by viewModel.availablePlugins.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var newRepoName by remember { mutableStateOf("") }
    var newRepoUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
      viewModel.operationMessages.collect { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Extensions & Repositories",
              fontWeight = FontWeight.Bold,
            )
          },
          navigationIcon = {
            IconButton(onClick = { backStack.removeLastOrNull() }) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            IconButton(onClick = { viewModel.reloadAllPlugins() }) {
              Icon(Icons.Outlined.Refresh, contentDescription = "Reload Plugins")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        )
      },
      floatingActionButton = {
        if (selectedTabIndex == 1) {
          FloatingActionButton(
            onClick = { showAddRepoDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
          ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add Repository")
          }
        }
      },
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      ) {
        TabRow(selectedTabIndex = selectedTabIndex) {
          Tab(
            selected = selectedTabIndex == 0,
            onClick = { selectedTabIndex = 0 },
            text = { Text("Installed (${installedPlugins.size})") },
          )
          Tab(
            selected = selectedTabIndex == 1,
            onClick = { selectedTabIndex = 1 },
            text = { Text("Catalog & Repos (${repositories.size})") },
          )
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        ) {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          } else {
            when (selectedTabIndex) {
              0 -> {
                // Installed Plugins List
                if (installedPlugins.isEmpty()) {
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
                      Icon(
                        imageVector = Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp),
                      )
                      Text(
                        text = "No Extensions Installed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "Browse the Catalog tab to install plugins from repositories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Button(onClick = { selectedTabIndex = 1 }) {
                        Text("Open Catalog")
                      }
                    }
                  }
                } else {
                  LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    items(installedPlugins) { plugin ->
                      Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                          containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Column(
                          modifier = Modifier.padding(16.dp),
                          verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            Column(modifier = Modifier.weight(1f)) {
                              Text(
                                text = plugin.manifest.name ?: plugin.file.nameWithoutExtension,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                              )
                              Text(
                                text = "v${plugin.manifest.version ?: 1} • ${plugin.manifest.author ?: "Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                              )
                            }
                            IconButton(onClick = { viewModel.uninstallPlugin(plugin) }) {
                              Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Uninstall",
                                tint = MaterialTheme.colorScheme.error,
                              )
                            }
                          }

                          plugin.manifest.description?.let { desc ->
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

                          if (plugin.registeredProviders.isNotEmpty()) {
                            Text(
                              text = "Providers: ${plugin.registeredProviders.joinToString { it.name }}",
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.primary,
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

              1 -> {
                // Repository Catalog & Repositories
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  // Repositories Section
                  item {
                    Text(
                      text = "Configured Repositories",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }

                  items(repositories) { repo ->
                    Card(
                      shape = RoundedCornerShape(12.dp),
                      colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                      ),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = repo.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                          )
                          Text(
                            text = repo.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                        IconButton(onClick = { viewModel.removeRepository(repo.url) }) {
                          Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Repo",
                            tint = MaterialTheme.colorScheme.error,
                          )
                        }
                      }
                    }
                  }

                  // Available Plugins Section
                  item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                      text = "Available Extensions (${availablePlugins.size})",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }

                  if (availablePlugins.isEmpty()) {
                    item {
                      Text(
                        text = "Loading repository catalog or no extensions found...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  } else {
                    items(availablePlugins) { available ->
                      val isAlreadyInstalled = installedPlugins.any {
                        it.manifest.name.equals(available.name, ignoreCase = true) ||
                          it.file.name.contains(available.name, ignoreCase = true)
                      }

                      Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                          containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Row(
                          modifier = Modifier.padding(16.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = available.name,
                              style = MaterialTheme.typography.bodyLarge,
                              fontWeight = FontWeight.SemiBold,
                            )
                            if (available.description.isNotBlank()) {
                              Text(
                                text = available.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                              )
                            }
                          }

                          if (isAlreadyInstalled) {
                            Text(
                              text = "Installed",
                              style = MaterialTheme.typography.labelMedium,
                              color = MaterialTheme.colorScheme.primary,
                              fontWeight = FontWeight.Bold,
                              modifier = Modifier.padding(horizontal = 8.dp),
                            )
                          } else {
                            IconButton(onClick = { viewModel.installPlugin(available) }) {
                              Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = "Install Plugin",
                                tint = MaterialTheme.colorScheme.primary,
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

      // Add Repository Dialog
      if (showAddRepoDialog) {
        AlertDialog(
          onDismissRequest = { showAddRepoDialog = false },
          title = { Text("Add Custom Repository") },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedTextField(
                value = newRepoName,
                onValueChange = { newRepoName = it },
                label = { Text("Repository Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = newRepoUrl,
                onValueChange = { newRepoUrl = it },
                label = { Text("Repository JSON URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          },
          confirmButton = {
            Button(
              onClick = {
                viewModel.addRepository(newRepoName, newRepoUrl)
                newRepoName = ""
                newRepoUrl = ""
                showAddRepoDialog = false
              },
            ) {
              Text("Add")
            }
          },
          dismissButton = {
            TextButton(onClick = { showAddRepoDialog = false }) {
              Text("Cancel")
            }
          },
        )
      }
    }
  }
}
