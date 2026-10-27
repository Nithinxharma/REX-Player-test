package xyz.mpv.rex.ui.preferences

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream4.viewmodels.ExtensionsAction
import com.lagradost.cloudstream4.viewmodels.ExtensionsViewModel2
import com.lagradost.cloudstream4.viewmodels.Repository
import com.lagradost.cloudstream4.viewmodels.RepositoryStatus
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.CSCorePreferences
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.utils.LocalBackStack
import kotlin.uuid.ExperimentalUuidApi

@OptIn(InternalAPI::class, ExperimentalUuidApi::class)
@Serializable
object CSCoreRepositoriesScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val preferences = koinInject<CSCorePreferences>()
    val viewModel = koinInject<ExtensionsViewModel2>()
    val uiState by viewModel.state.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var repositoryToDelete by remember { mutableStateOf<Repository?>(null) }

    LaunchedEffect(Unit) {
      viewModel.onAction(ExtensionsAction.Init)
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Extension Repositories",
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
            IconButton(onClick = { viewModel.onAction(ExtensionsAction.Init) }) {
              Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Refresh repositories",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          },
        )
      },
      floatingActionButton = {
        FloatingActionButton(
          onClick = { showAddDialog = true },
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
          Icon(Icons.Outlined.Add, contentDescription = "Add Repository")
        }
      },
    ) { padding ->
      val navBarHeight = LocalNavigationBarHeight.current
      val repositoriesList = uiState.repositories.data.values.toList()

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 12.dp,
          bottom = navBarHeight + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (repositoriesList.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  Icons.Outlined.Hub,
                  contentDescription = null,
                  modifier = Modifier.size(48.dp),
                  tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  "No Repositories Configured",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  "Tap the + button to add a CloudStream extension repository URL.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline,
                )
              }
            }
          }
        } else {
          items(repositoriesList, key = { it.uuid.toString() }) { repo ->
            val isPrebuilt = CSCorePreferences.PREBUILT_REPOSITORIES.any { it.url.equals(repo.info.url, ignoreCase = true) }

            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                  Icon(
                    Icons.Outlined.Hub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = repo.info.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = repo.info.url,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline,
                      maxLines = 1,
                    )
                  }

                  if (!isPrebuilt) {
                    IconButton(onClick = { repositoryToDelete = repo }) {
                      Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete repository",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  when (val status = repo.status) {
                    is RepositoryStatus.Loading -> {
                      CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        "Synchronizing...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }

                    is RepositoryStatus.Success -> {
                      Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        if (isPrebuilt) "Built-in • Active" else "Custom • Synchronized",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }

                    is RepositoryStatus.Failure -> {
                      Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        "Error: ${status.cause.message ?: "Unreachable"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                      )
                    }
                  }

                  Spacer(modifier = Modifier.weight(1f))

                  OutlinedButton(
                    onClick = { viewModel.onAction(ExtensionsAction.ReloadRepository(repo.uuid)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                  ) {
                    Icon(
                      Icons.Outlined.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync", style = MaterialTheme.typography.labelMedium)
                  }
                }
              }
            }
          }
        }
      }
    }

    // Add Repository Dialog
    if (showAddDialog) {
      var repoName by remember { mutableStateOf("") }
      var repoUrl by remember { mutableStateOf("") }

      AlertDialog(
        onDismissRequest = { showAddDialog = false },
        title = { Text("Add Extension Repository") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              "Enter a CloudStream repository JSON URL to discover available extension packages.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
            )
            OutlinedTextField(
              value = repoName,
              onValueChange = { repoName = it },
              label = { Text("Repository Name (Optional)") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
            )
            OutlinedTextField(
              value = repoUrl,
              onValueChange = { repoUrl = it },
              label = { Text("Repository URL") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (repoUrl.isNotBlank()) {
                preferences.addCustomRepository(repoName.ifBlank { repoUrl }, repoUrl)
                viewModel.onAction(ExtensionsAction.DownloadRepositoryWithDialog(repoUrl))
                showAddDialog = false
                Toast.makeText(context, "Repository added", Toast.LENGTH_SHORT).show()
              }
            },
          ) {
            Text("Add & Sync")
          }
        },
        dismissButton = {
          TextButton(onClick = { showAddDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }

    // Delete Repository Confirmation Dialog
    repositoryToDelete?.let { repo ->
      AlertDialog(
        onDismissRequest = { repositoryToDelete = null },
        title = { Text("Remove Repository?") },
        text = {
          Text("Are you sure you want to remove '${repo.info.name}'? Existing installed extensions will remain unless uninstalled.")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.onAction(ExtensionsAction.DeleteRepository(repo.uuid))
              repositoryToDelete = null
              Toast.makeText(context, "Repository removed", Toast.LENGTH_SHORT).show()
            },
          ) {
            Text("Remove", color = MaterialTheme.colorScheme.onError)
          }
        },
        dismissButton = {
          TextButton(onClick = { repositoryToDelete = null }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }
  }
}
