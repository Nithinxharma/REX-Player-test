package xyz.mpv.rex.ui.cloudstream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.cloudstream.LoadedPlugin
import xyz.mpv.rex.cloudstream.PluginLoadResult
import xyz.mpv.rex.preferences.CSCorePreferences
import xyz.mpv.rex.preferences.CSCoreRepo
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AvailablePlugin(
  val name: String,
  val url: String,
  val version: Int = 1,
  val description: String = "",
  val repositoryUrl: String = "",
)

class CSExtensionsViewModel(
  private val pluginCoordinator: CloudStreamPluginCoordinator,
  private val preferences: CSCorePreferences,
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build(),
) : ViewModel() {

  private val _installedPlugins = MutableStateFlow<List<LoadedPlugin>>(emptyList())
  val installedPlugins: StateFlow<List<LoadedPlugin>> = _installedPlugins.asStateFlow()

  private val _repositories = MutableStateFlow<List<CSCoreRepo>>(emptyList())
  val repositories: StateFlow<List<CSCoreRepo>> = _repositories.asStateFlow()

  private val _availablePlugins = MutableStateFlow<List<AvailablePlugin>>(emptyList())
  val availablePlugins: StateFlow<List<AvailablePlugin>> = _availablePlugins.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _operationMessages = MutableSharedFlow<String>()
  val operationMessages: SharedFlow<String> = _operationMessages.asSharedFlow()

  init {
    loadData()
  }

  fun loadData() {
    viewModelScope.launch {
      _isLoading.value = true
      if (!pluginCoordinator.isInitialized) {
        pluginCoordinator.initialize()
      }
      _installedPlugins.value = pluginCoordinator.loadedPlugins
      _repositories.value = preferences.getRepositories()
      _isLoading.value = false

      // Asynchronously fetch plugins catalog from repositories
      fetchAvailablePluginsFromRepos()
    }
  }

  fun addRepository(name: String, url: String) {
    val trimmedName = name.trim()
    val trimmedUrl = url.trim()
    if (trimmedName.isEmpty() || trimmedUrl.isEmpty()) {
      viewModelScope.launch {
        _operationMessages.emit("Repository name and URL cannot be empty")
      }
      return
    }

    preferences.addCustomRepository(trimmedName, trimmedUrl)
    _repositories.value = preferences.getRepositories()
    viewModelScope.launch {
      _operationMessages.emit("Repository '$trimmedName' added")
      fetchAvailablePluginsFromRepos()
    }
  }

  fun removeRepository(url: String) {
    preferences.removeCustomRepository(url)
    _repositories.value = preferences.getRepositories()
    viewModelScope.launch {
      _operationMessages.emit("Repository removed")
      fetchAvailablePluginsFromRepos()
    }
  }

  fun reloadAllPlugins() {
    viewModelScope.launch {
      _isLoading.value = true
      pluginCoordinator.reload()
      _installedPlugins.value = pluginCoordinator.loadedPlugins
      _isLoading.value = false
      _operationMessages.emit("Reloaded ${pluginCoordinator.loadedPlugins.size} plugins")
    }
  }

  fun uninstallPlugin(plugin: LoadedPlugin) {
    viewModelScope.launch {
      _isLoading.value = true
      pluginCoordinator.unloadPlugin(plugin.file)
      runCatching {
        plugin.file.delete()
      }
      _installedPlugins.value = pluginCoordinator.loadedPlugins
      _isLoading.value = false
      _operationMessages.emit("Uninstalled ${plugin.manifest.name ?: plugin.file.name}")
    }
  }

  fun installPlugin(plugin: AvailablePlugin) {
    viewModelScope.launch {
      _isLoading.value = true
      val result = withContext(Dispatchers.IO) {
        runCatching {
          val fileName = plugin.name.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".cs3"
          val targetFile = File(pluginCoordinator.pluginsDirectory, fileName)

          val request = Request.Builder().url(plugin.url).build()
          val response = okHttpClient.newCall(request).execute()
          if (!response.isSuccessful) {
            return@runCatching "Download failed with HTTP ${response.code}"
          }

          val body = response.body ?: return@runCatching "Empty response body"
          body.byteStream().use { input ->
            FileOutputStream(targetFile).use { output ->
              input.copyTo(output)
            }
          }

          val loadResult = pluginCoordinator.loadPluginArchive(targetFile)
          when (loadResult) {
            is PluginLoadResult.Success -> "Successfully installed ${plugin.name}"
            is PluginLoadResult.Failure -> "Plugin loaded with error: ${loadResult.error.message}"
          }
        }.getOrElse { e ->
          "Failed to install ${plugin.name}: ${e.message}"
        }
      }

      _installedPlugins.value = pluginCoordinator.loadedPlugins
      _isLoading.value = false
      _operationMessages.emit(result)
    }
  }

  private suspend fun fetchAvailablePluginsFromRepos() = withContext(Dispatchers.IO) {
    val repos = preferences.getRepositories()
    val discovered = mutableListOf<AvailablePlugin>()

    for (repo in repos) {
      runCatching {
        val request = Request.Builder().url(repo.url).build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
          val content = response.body?.string() ?: ""
          if (content.startsWith("[")) {
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
              val item = arr.optJSONObject(i) ?: continue
              val name = item.optString("name", "Unknown")
              val url = item.optString("url", "")
              if (url.isNotBlank()) {
                discovered.add(
                  AvailablePlugin(
                    name = name,
                    url = url,
                    version = item.optInt("version", 1),
                    description = item.optString("description", ""),
                    repositoryUrl = repo.url,
                  )
                )
              }
            }
          } else if (content.startsWith("{")) {
            val obj = JSONObject(content)
            val pluginLists = obj.optJSONArray("pluginLists")
            if (pluginLists != null) {
              for (j in 0 until pluginLists.length()) {
                val listUrl = pluginLists.optString(j)
                if (listUrl.isNotBlank()) {
                  runCatching {
                    val subResp = okHttpClient.newCall(Request.Builder().url(listUrl).build()).execute()
                    if (subResp.isSuccessful) {
                      val subContent = subResp.body?.string() ?: ""
                      val subArr = JSONArray(subContent)
                      for (k in 0 until subArr.length()) {
                        val subItem = subArr.optJSONObject(k) ?: continue
                        val name = subItem.optString("name", "Unknown")
                        val url = subItem.optString("url", "")
                        if (url.isNotBlank()) {
                          discovered.add(
                            AvailablePlugin(
                              name = name,
                              url = url,
                              version = subItem.optInt("version", 1),
                              description = subItem.optString("description", ""),
                              repositoryUrl = repo.url,
                            )
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

    _availablePlugins.value = discovered.distinctBy { it.url }
  }
}
