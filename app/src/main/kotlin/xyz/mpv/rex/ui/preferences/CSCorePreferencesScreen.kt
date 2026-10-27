package xyz.mpv.rex.ui.preferences

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.TvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.TextFieldPreference
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.preferences.CSCorePreferences
import xyz.mpv.rex.preferences.CSCoreRepo
import xyz.mpv.rex.preferences.SubtitlesPreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.presentation.components.GroupPosition
import xyz.mpv.rex.presentation.components.GroupedListColumn
import xyz.mpv.rex.presentation.components.GroupedListItem
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.preferences.components.SwitchPreference
import xyz.mpv.rex.ui.utils.LocalBackStack

@Serializable
object CSCorePreferencesScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<CSCorePreferences>()
    val subtitlesPreferences = koinInject<SubtitlesPreferences>()
    val pluginCoordinator = koinInject<CloudStreamPluginCoordinator>()
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Dialog state controllers
    var showRepositoriesDialog by remember { mutableStateOf(false) }
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var showTrackersDialog by remember { mutableStateOf(false) }
    var showOpenSubtitlesDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showMediaTypesDialog by remember { mutableStateOf(false) }
    var isReloadingPlugins by remember { mutableStateOf(false) }

    // Coordinator summary state
    var discoveredCount by remember { mutableIntStateOf(0) }
    var loadedCount by remember { mutableIntStateOf(pluginCoordinator.loadedPlugins.size) }
    var providerCount by remember { mutableIntStateOf(pluginCoordinator.registeredProviders.size) }

    LaunchedEffect(Unit) {
      withContext(Dispatchers.IO) {
        val archives = pluginCoordinator.discoverPluginArchives(pluginCoordinator.pluginsDirectory)
        discoveredCount = archives.size
        loadedCount = pluginCoordinator.loadedPlugins.size
        providerCount = pluginCoordinator.registeredProviders.size
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.pref_cscore_title),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
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
        )
      },
    ) { padding ->
      val navBarHeight = LocalNavigationBarHeight.current
      ProvidePreferenceLocals {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = navBarHeight + 24.dp,
          ),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

          // ==========================================
          // 1. REPOSITORIES & EXTENSIONS
          // ==========================================
          item {
            PreferenceSectionHeader(title = stringResource(R.string.cscore_category_repositories))
          }

          item {
            val autoUpdate by preferences.autoUpdatePlugins.collectAsState()
            val autoDownload by preferences.autoDownloadPlugins.collectAsState()
            val repos = preferences.getRepositories()

            GroupedListColumn {
              GroupedPreferenceCard(position = GroupPosition.FIRST) {
                SwitchPreference(
                  value = autoUpdate,
                  onValueChange = preferences.autoUpdatePlugins::set,
                  title = { Text(stringResource(R.string.cscore_auto_update_plugins_title)) },
                  summary = { Text(stringResource(R.string.cscore_auto_update_plugins_summary)) },
                  icon = {
                    Icon(
                      Icons.Outlined.Extension,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                val autoDownloadLabels = mapOf(
                  0 to stringResource(R.string.cscore_auto_download_off),
                  1 to stringResource(R.string.cscore_auto_download_always),
                  2 to stringResource(R.string.cscore_auto_download_wifi),
                )
                ListPreference(
                  value = autoDownload,
                  onValueChange = preferences.autoDownloadPlugins::set,
                  values = listOf(0, 1, 2),
                  title = { Text(stringResource(R.string.cscore_auto_download_title)) },
                  summary = {
                    Text(
                      autoDownloadLabels[autoDownload] ?: stringResource(R.string.cscore_auto_download_off),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  valueToText = { AnnotatedString(autoDownloadLabels[it] ?: "") },
                  icon = {
                    Icon(
                      Icons.Outlined.Wifi,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_repositories_title)) },
                  summary = {
                    Text(
                      stringResource(R.string.cscore_repositories_summary, repos.size),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      Icons.Outlined.Hub,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = { backstack.add(CSCoreRepositoriesScreen) },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                Preference(
                  title = { Text("Provider Extensions") },
                  summary = {
                    Text(
                      "$loadedCount plugin(s) loaded • Tap to browse & install extensions",
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      Icons.Outlined.Extension,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = { backstack.add(CSCoreExtensionsScreen) },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_plugin_dir_title)) },
                  summary = {
                    Text(
                      pluginCoordinator.pluginsDirectory.absolutePath,
                      color = MaterialTheme.colorScheme.outline,
                      style = MaterialTheme.typography.bodySmall,
                    )
                  },
                  icon = {
                    Icon(
                      Icons.Outlined.Folder,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = {
                    clipboardManager.setText(AnnotatedString(pluginCoordinator.pluginsDirectory.absolutePath))
                    Toast.makeText(context, "Directory path copied", Toast.LENGTH_SHORT).show()
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.LAST) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_reload_plugins_title)) },
                  summary = {
                    if (isReloadingPlugins) {
                      Text("Scanning and reloading plugins...", color = MaterialTheme.colorScheme.primary)
                    } else {
                      Text(stringResource(R.string.cscore_reload_plugins_summary), color = MaterialTheme.colorScheme.outline)
                    }
                  },
                  icon = {
                    if (isReloadingPlugins) {
                      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                      Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                      )
                    }
                  },
                  enabled = !isReloadingPlugins,
                  onClick = {
                    scope.launch {
                      isReloadingPlugins = true
                      val summary = pluginCoordinator.reload()
                      discoveredCount = summary.discoveredFiles
                      loadedCount = summary.loadedCount
                      providerCount = summary.totalRegisteredProviders
                      isReloadingPlugins = false
                      Toast.makeText(
                        context,
                        "Reloaded: ${summary.loadedCount} plugins, ${summary.totalRegisteredProviders} providers",
                        Toast.LENGTH_SHORT,
                      ).show()
                    }
                  },
                )
              }
            }
          }

          // ==========================================
          // 2. CONTENT & PROVIDERS
          // ==========================================
          item {
            PreferenceSectionHeader(title = stringResource(R.string.cscore_category_content))
          }

          item {
            val preferredMedia by preferences.preferredMediaTypes.collectAsState()
            val displayDubSub by preferences.displayDubSub.collectAsState()
            val isNsfw = preferences.isNsfwAllowed()

            GroupedListColumn {
              GroupedPreferenceCard(position = GroupPosition.FIRST) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_preferred_media_title)) },
                  summary = {
                    val count = preferredMedia.size
                    Text(
                      "$count media categories active",
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      Icons.Outlined.Movie,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = { showMediaTypesDialog = true },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                val dubSubOptions = listOf("All", "Subbed", "Dubbed", "None")
                val currentDubSub = when {
                  displayDubSub.contains("Dubbed") && displayDubSub.contains("Subbed") -> "All"
                  displayDubSub.contains("Subbed") -> "Subbed"
                  displayDubSub.contains("Dubbed") -> "Dubbed"
                  else -> "None"
                }

                ListPreference(
                  value = currentDubSub,
                  onValueChange = { selected ->
                    when (selected) {
                      "All" -> preferences.displayDubSub.set(setOf("Dubbed", "Subbed"))
                      "Subbed" -> preferences.displayDubSub.set(setOf("Subbed"))
                      "Dubbed" -> preferences.displayDubSub.set(setOf("Dubbed"))
                      else -> preferences.displayDubSub.set(setOf("None"))
                    }
                  },
                  values = dubSubOptions,
                  title = { Text(stringResource(R.string.cscore_dub_sub_title)) },
                  summary = {
                    Text(
                      currentDubSub,
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  valueToText = { AnnotatedString(it) },
                  icon = {
                    Icon(
                      Icons.Outlined.Subtitles,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.LAST) {
                SwitchPreference(
                  value = isNsfw,
                  onValueChange = { preferences.setNsfwAllowed(it) },
                  title = { Text(stringResource(R.string.cscore_nsfw_title)) },
                  summary = { Text(stringResource(R.string.cscore_nsfw_summary)) },
                  icon = {
                    Icon(
                      Icons.Outlined.Security,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                )
              }
            }
          }

          // ==========================================
          // 3. NETWORK & EXTRACTION
          // ==========================================
          item {
            PreferenceSectionHeader(title = stringResource(R.string.cscore_category_network))
          }

          item {
            val currentDns by preferences.dnsProvider.collectAsState()
            val jsdelivrProxy by preferences.jsdelivrProxy.collectAsState()
            val parallelDownloads by preferences.parallelDownloads.collectAsState()
            val concurrentConnections by preferences.concurrentConnections.collectAsState()

            val dnsOptions = listOf(0, 1, 2, 4, 6, 7)
            val dnsLabels = mapOf(
              0 to stringResource(R.string.cscore_dns_default),
              1 to stringResource(R.string.cscore_dns_google),
              2 to stringResource(R.string.cscore_dns_cloudflare),
              4 to stringResource(R.string.cscore_dns_adguard),
              6 to stringResource(R.string.cscore_dns_quad9),
              7 to stringResource(R.string.cscore_dns_dnssb),
            )

            GroupedListColumn {
              GroupedPreferenceCard(position = GroupPosition.FIRST) {
                ListPreference(
                  value = currentDns,
                  onValueChange = preferences.dnsProvider::set,
                  values = dnsOptions,
                  title = { Text(stringResource(R.string.cscore_dns_title)) },
                  summary = {
                    Text(
                      dnsLabels[currentDns] ?: stringResource(R.string.cscore_dns_default),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  valueToText = { AnnotatedString(dnsLabels[it] ?: "") },
                  icon = {
                    Icon(
                      Icons.Outlined.Dns,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                SwitchPreference(
                  value = jsdelivrProxy,
                  onValueChange = preferences.jsdelivrProxy::set,
                  title = { Text(stringResource(R.string.cscore_jsdelivr_proxy_title)) },
                  summary = { Text(stringResource(R.string.cscore_jsdelivr_proxy_summary)) },
                  icon = {
                    Icon(
                      Icons.Outlined.Language,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                SliderPreference(
                  value = parallelDownloads.toFloat(),
                  onValueChange = { preferences.parallelDownloads.set(it.toInt()) },
                  sliderValue = parallelDownloads.toFloat(),
                  onSliderValueChange = { preferences.parallelDownloads.set(it.toInt()) },
                  valueRange = 1f..10f,
                  valueSteps = 8,
                  title = { Text(stringResource(R.string.cscore_parallel_downloads_title)) },
                  summary = {
                    Text(
                      stringResource(R.string.cscore_parallel_downloads_summary, parallelDownloads),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.LAST) {
                SliderPreference(
                  value = concurrentConnections.toFloat(),
                  onValueChange = { preferences.concurrentConnections.set(it.toInt()) },
                  sliderValue = concurrentConnections.toFloat(),
                  onSliderValueChange = { preferences.concurrentConnections.set(it.toInt()) },
                  valueRange = 1f..10f,
                  valueSteps = 8,
                  title = { Text(stringResource(R.string.cscore_concurrent_connections_title)) },
                  summary = {
                    Text(
                      stringResource(R.string.cscore_concurrent_connections_summary, concurrentConnections),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }
            }
          }

          // ==========================================
          // 4. SYNC & TRACKER ACCOUNTS
          // ==========================================
          item {
            PreferenceSectionHeader(title = stringResource(R.string.cscore_category_accounts))
          }

          item {
            val subdlKey by subtitlesPreferences.subdlApiKey.collectAsState()
            val openSubtitlesUser by preferences.openSubtitlesUsername.collectAsState()

            GroupedListColumn {
              GroupedPreferenceCard(position = GroupPosition.FIRST) {
                Preference(
                  title = { Text("SubDL Subtitles") },
                  summary = {
                    if (subdlKey.isNotBlank()) {
                      Text("Configured (Linked with REX Subtitle Preferences)", color = MaterialTheme.colorScheme.primary)
                    } else {
                      Text("Not configured • Tap to set API Key", color = MaterialTheme.colorScheme.outline)
                    }
                  },
                  icon = {
                    Icon(
                      Icons.Outlined.Subtitles,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = {
                    backstack.add(SubtitlesPreferencesScreen)
                  },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_opensubtitles_title)) },
                  summary = {
                    if (openSubtitlesUser.isNotBlank()) {
                      Text("Logged in as $openSubtitlesUser", color = MaterialTheme.colorScheme.primary)
                    } else {
                      Text(stringResource(R.string.cscore_opensubtitles_summary), color = MaterialTheme.colorScheme.outline)
                    }
                  },
                  icon = {
                    Icon(
                      Icons.Outlined.Subtitles,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = { showOpenSubtitlesDialog = true },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.LAST) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_trackers_title)) },
                  summary = { Text(stringResource(R.string.cscore_trackers_summary), color = MaterialTheme.colorScheme.outline) },
                  icon = {
                    Icon(
                      Icons.Outlined.Sync,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = { showTrackersDialog = true },
                )
              }
            }
          }

          // ==========================================
          // 5. DIAGNOSTICS & STATUS
          // ==========================================
          item {
            PreferenceSectionHeader(title = stringResource(R.string.cscore_category_diagnostics))
          }

          item {
            GroupedListColumn {
              GroupedPreferenceCard(position = GroupPosition.FIRST) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_diag_status_title)) },
                  summary = {
                    Text(
                      stringResource(R.string.cscore_diag_status_summary, loadedCount, providerCount) + " • Tap to inspect active providers",
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      Icons.Outlined.CheckCircle,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = { backstack.add(CSCoreProvidersScreen) },
                )
              }

              GroupedPreferenceCard(position = GroupPosition.LAST) {
                Preference(
                  title = { Text(stringResource(R.string.cscore_diag_logcat_title)) },
                  summary = { Text(stringResource(R.string.cscore_diag_logcat_summary), color = MaterialTheme.colorScheme.outline) },
                  icon = {
                    Icon(
                      Icons.Outlined.Info,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = { showDiagnosticsDialog = true },
                )
              }
            }
          }
        }
      }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Repositories Management Dialog
    if (showRepositoriesDialog) {
      val repos = preferences.getRepositories()
      AlertDialog(
        onDismissRequest = { showRepositoriesDialog = false },
        title = { Text(stringResource(R.string.cscore_repositories_title)) },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            repos.forEach { repo ->
              val isPrebuilt = CSCorePreferences.PREBUILT_REPOSITORIES.any { it.url == repo.url }
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = repo.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = repo.url,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline,
                      maxLines = 1,
                    )
                    if (isPrebuilt) {
                      Text(
                        text = "Built-in",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                  if (!isPrebuilt) {
                    IconButton(
                      onClick = {
                        preferences.removeCustomRepository(repo.url)
                      },
                    ) {
                      Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          Button(onClick = { showAddRepoDialog = true }) {
            Text(stringResource(R.string.cscore_add_repository))
          }
        },
        dismissButton = {
          TextButton(onClick = { showRepositoriesDialog = false }) {
            Text(stringResource(R.string.generic_ok))
          }
        },
      )
    }

    // 2. Add Custom Repository Dialog
    if (showAddRepoDialog) {
      var repoName by remember { mutableStateOf("") }
      var repoUrl by remember { mutableStateOf("") }

      AlertDialog(
        onDismissRequest = { showAddRepoDialog = false },
        title = { Text(stringResource(R.string.cscore_add_repository)) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = repoName,
              onValueChange = { repoName = it },
              label = { Text(stringResource(R.string.cscore_repo_name_hint)) },
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = repoUrl,
              onValueChange = { repoUrl = it },
              label = { Text(stringResource(R.string.cscore_repo_url_hint)) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (repoUrl.isNotBlank()) {
                preferences.addCustomRepository(repoName, repoUrl)
                showAddRepoDialog = false
              }
            },
          ) {
            Text(stringResource(R.string.generic_confirm))
          }
        },
        dismissButton = {
          TextButton(onClick = { showAddRepoDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }

    // 3. OpenSubtitles Credentials Dialog
    if (showOpenSubtitlesDialog) {
      var username by remember { mutableStateOf(preferences.openSubtitlesUsername.get()) }
      var password by remember { mutableStateOf(preferences.openSubtitlesPassword.get()) }

      AlertDialog(
        onDismissRequest = { showOpenSubtitlesDialog = false },
        title = { Text(stringResource(R.string.cscore_opensubtitles_title)) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = username,
              onValueChange = { username = it },
              label = { Text(stringResource(R.string.cscore_opensubtitles_user_hint)) },
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text(stringResource(R.string.cscore_opensubtitles_pass_hint)) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              preferences.openSubtitlesUsername.set(username)
              preferences.openSubtitlesPassword.set(password)
              showOpenSubtitlesDialog = false
              Toast.makeText(context, "OpenSubtitles credentials saved", Toast.LENGTH_SHORT).show()
            },
          ) {
            Text(stringResource(R.string.generic_ok))
          }
        },
        dismissButton = {
          TextButton(onClick = { showOpenSubtitlesDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
      )
    }

    // 4. Metadata Trackers Information Dialog
    if (showTrackersDialog) {
      AlertDialog(
        onDismissRequest = { showTrackersDialog = false },
        title = { Text(stringResource(R.string.cscore_trackers_title)) },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "CloudStream Sync & Tracker Integration Status",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "The eight account files (AccountAdapter, AccountHelper, AccountScreen, AccountSelectActivity/2, AccountViewModel/2) manage local on-device profiles and startup PIN locks, which are intentionally excluded to keep REX's native single-user host architecture intact.",
              style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
              text = "External tracker OAuth authentication (AniList, MyAnimeList, Kitsu, Simkl, AnimeSkip) requires the sync provider subsystem: AccountManager.kt, SyncRepo.kt, AuthAPI.kt, and provider OAuth handlers.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
            )
          }
        },
        confirmButton = {
          Button(onClick = { showTrackersDialog = false }) {
            Text(stringResource(R.string.generic_ok))
          }
        },
      )
    }

    // 5. Preferred Media Types Selection Dialog
    if (showMediaTypesDialog) {
      val selectedOrdinals = preferences.preferredMediaTypes.get().toMutableSet()
      val supportedTypes = TvType.entries.filter { it != TvType.NSFW }

      AlertDialog(
        onDismissRequest = { showMediaTypesDialog = false },
        title = { Text(stringResource(R.string.cscore_preferred_media_title)) },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            supportedTypes.forEach { type ->
              val ordinalStr = type.ordinal.toString()
              val isChecked = selectedOrdinals.contains(ordinalStr)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (isChecked) {
                      selectedOrdinals.remove(ordinalStr)
                    } else {
                      selectedOrdinals.add(ordinalStr)
                    }
                    preferences.preferredMediaTypes.set(selectedOrdinals.toSet())
                  }
                  .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  if (isChecked) Icons.Outlined.Check else Icons.Outlined.Movie,
                  contentDescription = null,
                  tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = type.name,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                )
              }
            }
          }
        },
        confirmButton = {
          Button(onClick = { showMediaTypesDialog = false }) {
            Text(stringResource(R.string.generic_ok))
          }
        },
      )
    }

    // 6. Diagnostics Log Dialog
    if (showDiagnosticsDialog) {
      val loaded = pluginCoordinator.loadedPlugins
      val registered = pluginCoordinator.registeredProviders

      AlertDialog(
        onDismissRequest = { showDiagnosticsDialog = false },
        title = { Text(stringResource(R.string.cscore_diag_logcat_title)) },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Registered MainAPI Providers (${registered.size}):",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            if (registered.isEmpty()) {
              Text("No external providers registered yet.", color = MaterialTheme.colorScheme.outline)
            } else {
              registered.forEach { provider ->
                Text(
                  "• ${provider.name} (${provider.mainUrl}) [${provider.lang}]",
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
              text = "Loaded Plugin Archives (${loaded.size}):",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            if (loaded.isEmpty()) {
              Text("No plugins loaded from ${pluginCoordinator.pluginsDirectory.name}.", color = MaterialTheme.colorScheme.outline)
            } else {
              loaded.forEach { plugin ->
                Text(
                  "• ${plugin.manifest.name ?: plugin.file.name} (v${plugin.manifest.version ?: 1})",
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        },
        confirmButton = {
          Button(onClick = { showDiagnosticsDialog = false }) {
            Text(stringResource(R.string.generic_ok))
          }
        },
      )
    }
  }
}
