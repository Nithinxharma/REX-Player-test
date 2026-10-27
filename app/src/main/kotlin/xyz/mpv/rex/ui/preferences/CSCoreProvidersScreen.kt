package xyz.mpv.rex.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.MainAPI
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.utils.LocalBackStack

@Serializable
object CSCoreProvidersScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val pluginCoordinator = koinInject<CloudStreamPluginCoordinator>()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf<MainAPI?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val registeredProviders = remember(refreshTrigger) {
      pluginCoordinator.registeredProviders
    }

    val filteredProviders = remember(registeredProviders, searchQuery) {
      if (searchQuery.isBlank()) {
        registeredProviders
      } else {
        registeredProviders.filter { provider ->
          provider.name.contains(searchQuery, ignoreCase = true) ||
            provider.mainUrl.contains(searchQuery, ignoreCase = true) ||
            provider.lang.contains(searchQuery, ignoreCase = true)
        }
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Registered Providers",
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
            IconButton(onClick = { refreshTrigger++ }) {
              Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Refresh registry",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          },
        )
      },
    ) { padding ->
      val navBarHeight = LocalNavigationBarHeight.current

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        // Search bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          placeholder = { Text("Filter active providers by name or URL...") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
        )

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
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${filteredProviders.size} Active Provider(s) Registered in APIHolder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          if (filteredProviders.isEmpty()) {
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
                    Icons.Outlined.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline,
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    "No Active Providers Registered",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    "Install extension packages from the Extensions screen to register providers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                  )
                }
              }
            }
          } else {
            items(filteredProviders, key = { provider -> "${provider.name}_${provider.mainUrl}" }) { provider ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedProvider = provider },
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
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = provider.mainUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                      )
                    }

                    Surface(
                      color = MaterialTheme.colorScheme.primaryContainer,
                      shape = RoundedCornerShape(12.dp),
                    ) {
                      Text(
                        text = provider.lang.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Supported TvTypes chips
                  FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    provider.supportedTypes.forEach { type ->
                      Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                      ) {
                        Text(
                          text = type.name,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          style = MaterialTheme.typography.labelSmall,
                        )
                      }
                    }
                  }

                  provider.sourcePlugin?.let { source ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "Source: ${source.substringAfterLast('/')}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.outline,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Provider Details Dialog
    selectedProvider?.let { provider ->
      AlertDialog(
        onDismissRequest = { selectedProvider = null },
        title = { Text(provider.name) },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Provider Details & Configuration",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()
            Text("Main URL: ${provider.mainUrl}", style = MaterialTheme.typography.bodySmall)
            Text("Language: ${provider.lang}", style = MaterialTheme.typography.bodySmall)
            Text("Supported Types: ${provider.supportedTypes.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
            Text("Search Enabled: ${provider.hasMainPage}", style = MaterialTheme.typography.bodySmall)
            provider.sourcePlugin?.let {
              Text("Source Plugin: $it", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
          }
        },
        confirmButton = {
          Button(onClick = { selectedProvider = null }) {
            Text(stringResource(R.string.generic_ok))
          }
        },
      )
    }
  }
}
