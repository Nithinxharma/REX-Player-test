package xyz.mpv.rex.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream4.viewmodels.Plugin
import com.lagradost.cloudstream4.viewmodels.PluginDownloadSource
import com.lagradost.cloudstream4.viewmodels.PluginInfo
import com.lagradost.cloudstream4.viewmodels.PluginStatus
import com.lagradost.cloudstream4.viewmodels.RepositoryInfo
import com.lagradost.cloudstream4.viewmodels.RepositoryManager
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.mpv.rex.preferences.CSCorePreferences
import java.io.File
import java.io.FileOutputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(com.lagradost.cloudstream3.InternalAPI::class, ExperimentalUuidApi::class)
class REXRepositoryManager(
  private val context: Context,
  private val okHttpClient: OkHttpClient,
  private val pluginCoordinator: CloudStreamPluginCoordinator,
  private val preferences: CSCorePreferences,
) : RepositoryManager {

  private val jsonFormat = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  override fun getRepositories(): List<RepositoryInfo> {
    return preferences.getRepositories().map { repo ->
      RepositoryInfo(
        iconUrl = repo.iconUrl,
        name = repo.name,
        url = repo.url,
        description = null,
      )
    }
  }

  override suspend fun getPlugins(
    repository: RepositoryInfo,
    repositoryUuid: Uuid,
  ): Pair<RepositoryInfo, List<Plugin>> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
      .url(repository.url)
      .header("User-Agent", "Mozilla/5.0 (Android; CloudStream)")
      .build()

    val response = okHttpClient.newCall(request).execute()
    if (!response.isSuccessful) {
      throw IllegalStateException("Failed to fetch repository from ${repository.url}: HTTP ${response.code}")
    }

    val bodyString = response.body?.string()
      ?: throw IllegalStateException("Empty response from repository ${repository.url}")

    var updatedRepoInfo = repository
    val pluginsList = mutableListOf<Plugin>()

    runCatching {
      val parsedJson = jsonFormat.parseToJsonElement(bodyString)
      val (repoInfo, pluginEntries) = parseRepositoryJson(parsedJson, repository)
      updatedRepoInfo = repoInfo

      for (entry in pluginEntries) {
        val internalName = entry.internalName
        val targetFile = File(pluginCoordinator.pluginsDirectory, "$internalName.cs3")
        val isDownloaded = targetFile.exists() && targetFile.length() > 0L

        val plugin = Plugin(
          info = PluginInfo(
            name = entry.name,
            internalName = internalName,
            version = entry.version,
            tvTypes = entry.tvTypes.toPersistentList(),
            authors = entry.authors.toPersistentList(),
            status = entry.status,
            iconUrl = entry.iconUrl,
            language = entry.language,
            description = entry.description,
            repositoryUrl = repository.url,
          ),
          source = PluginDownloadSource(
            size = entry.fileSize,
            internalName = internalName,
            pluginUrl = entry.downloadUrl,
            repositoryUrl = repository.url,
            digest = null,
          ),
          status = PluginStatus.Idle,
          downloadLocation = if (isDownloaded) targetFile.absolutePath else null,
          repository = repositoryUuid,
        )
        pluginsList.add(plugin)
      }
    }.onFailure { throwable ->
      throw IllegalStateException("Failed to parse repository content from ${repository.url}: ${throwable.message}", throwable)
    }

    updatedRepoInfo to pluginsList
  }

  override fun getLocalPlugins(localUuid: Uuid): List<Plugin> {
    val files = pluginCoordinator.discoverPluginArchives(pluginCoordinator.pluginsDirectory)
    val loadedMap = pluginCoordinator.loadedPlugins.associateBy { it.file.canonicalPath }

    return files.map { file ->
      val loaded = loadedMap[file.canonicalPath]
      val manifest = loaded?.manifest ?: runCatching { pluginCoordinator.readManifest(file) }.getOrNull()

      val name = manifest?.name ?: file.nameWithoutExtension
      val internalName = manifest?.pluginClassName ?: file.nameWithoutExtension
      val version = manifest?.version ?: 1

      Plugin(
        info = PluginInfo(
          name = name,
          internalName = internalName,
          version = version,
          tvTypes = emptyList<TvType>().toPersistentList(),
          authors = emptyList<String>().toPersistentList(),
          status = 1,
          iconUrl = null,
          language = null,
          description = "Locally installed plugin",
          repositoryUrl = null,
        ),
        source = null,
        status = PluginStatus.Idle,
        downloadLocation = file.absolutePath,
        repository = localUuid,
      )
    }
  }

  override suspend fun deletePlugin(location: String) = withContext(Dispatchers.IO) {
    val file = File(location)
    pluginCoordinator.unloadPlugin(file)
    if (file.exists()) {
      file.delete()
    }
  }

  override suspend fun deleteRepository(repository: RepositoryInfo) = withContext(Dispatchers.IO) {
    preferences.removeCustomRepository(repository.url)
  }

  override suspend fun downloadPlugin(
    source: PluginDownloadSource,
    downloadProgress: (Long, Long?) -> Unit,
  ): String = withContext(Dispatchers.IO) {
    val request = Request.Builder()
      .url(source.pluginUrl)
      .header("User-Agent", "Mozilla/5.0 (Android; CloudStream)")
      .build()

    val response = okHttpClient.newCall(request).execute()
    if (!response.isSuccessful) {
      throw IllegalStateException("Failed to download plugin from ${source.pluginUrl}: HTTP ${response.code}")
    }

    val body = response.body
      ?: throw IllegalStateException("Empty body when downloading plugin from ${source.pluginUrl}")

    val totalBytes = body.contentLength().takeIf { it > 0 } ?: source.size
    val destinationFile = File(pluginCoordinator.pluginsDirectory, "${source.internalName}.cs3")
    val tempFile = File(pluginCoordinator.pluginsDirectory, "${source.internalName}.cs3.tmp")

    body.byteStream().use { input ->
      FileOutputStream(tempFile).use { output ->
        val buffer = ByteArray(8192)
        var downloadedBytes = 0L
        var read: Int

        while (input.read(buffer).also { read = it } != -1) {
          output.write(buffer, 0, read)
          downloadedBytes += read
          downloadProgress(downloadedBytes, totalBytes)
        }
        output.flush()
      }
    }

    if (destinationFile.exists()) {
      pluginCoordinator.unloadPlugin(destinationFile)
      destinationFile.delete()
    }
    tempFile.renameTo(destinationFile)

    when (val loadResult = pluginCoordinator.loadPluginArchive(destinationFile)) {
      is PluginLoadResult.Success -> destinationFile.absolutePath
      is PluginLoadResult.Failure -> throw IllegalStateException("Downloaded plugin failed to load: ${loadResult.error.message}", loadResult.error)
    }
  }

  override suspend fun updateRepositoryInfo(repository: RepositoryInfo) = withContext(Dispatchers.IO) {
    preferences.addCustomRepository(repository.name, repository.url)
  }

  private fun parseRepositoryJson(
    element: JsonElement,
    fallbackInfo: RepositoryInfo,
  ): Pair<RepositoryInfo, List<RawPluginEntry>> {
    return when {
      element is JsonArray -> {
        val plugins = element.mapNotNull { parsePluginEntry(it) }
        fallbackInfo to plugins
      }

      element is JsonObject -> {
        val repoName = element["name"]?.jsonPrimitive?.contentOrNull ?: fallbackInfo.name
        val iconUrl = element["iconUrl"]?.jsonPrimitive?.contentOrNull ?: fallbackInfo.iconUrl
        val description = element["description"]?.jsonPrimitive?.contentOrNull ?: fallbackInfo.description
        val repoInfo = RepositoryInfo(iconUrl, repoName, fallbackInfo.url, description)

        val pluginsList = mutableListOf<RawPluginEntry>()

        element["plugins"]?.jsonArray?.forEach { entry ->
          parsePluginEntry(entry)?.let { pluginsList.add(it) }
        }

        element["pluginLists"]?.jsonArray?.forEach { listUrlElem ->
          val listUrl = listUrlElem.jsonPrimitive.contentOrNull ?: return@forEach
          runCatching {
            val req = Request.Builder().url(listUrl).build()
            val res = okHttpClient.newCall(req).execute()
            if (res.isSuccessful) {
              res.body?.string()?.let { listJsonStr ->
                val listElement = jsonFormat.parseToJsonElement(listJsonStr)
                if (listElement is JsonArray) {
                  listElement.mapNotNull { parsePluginEntry(it) }.let { pluginsList.addAll(it) }
                }
              }
            }
          }
        }

        repoInfo to pluginsList
      }

      else -> fallbackInfo to emptyList()
    }
  }

  private fun parsePluginEntry(element: JsonElement): RawPluginEntry? {
    val obj = element as? JsonObject ?: return null
    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return null
    val internalName = obj["internalName"]?.jsonPrimitive?.contentOrNull
      ?: obj["pluginClassName"]?.jsonPrimitive?.contentOrNull
      ?: name
    val downloadUrl = obj["url"]?.jsonPrimitive?.contentOrNull
      ?: obj["pluginUrl"]?.jsonPrimitive?.contentOrNull
      ?: return null

    val version = obj["version"]?.jsonPrimitive?.intOrNull ?: 1
    val status = obj["status"]?.jsonPrimitive?.intOrNull ?: 1
    val iconUrl = obj["iconUrl"]?.jsonPrimitive?.contentOrNull
    val language = obj["language"]?.jsonPrimitive?.contentOrNull
      ?: obj["lang"]?.jsonPrimitive?.contentOrNull
    val description = obj["description"]?.jsonPrimitive?.contentOrNull
    val fileSize = obj["fileSize"]?.jsonPrimitive?.longOrNull
      ?: obj["size"]?.jsonPrimitive?.longOrNull

    val tvTypes = mutableListOf<TvType>()
    obj["tvTypes"]?.jsonArray?.forEach { typeElem ->
      val typeStr = typeElem.jsonPrimitive.contentOrNull ?: return@forEach
      TvType.entries.firstOrNull { it.name.equals(typeStr, ignoreCase = true) }?.let { tvTypes.add(it) }
    }

    val authors = mutableListOf<String>()
    obj["authors"]?.jsonArray?.forEach { authElem ->
      authElem.jsonPrimitive.contentOrNull?.let { authors.add(it) }
    }

    return RawPluginEntry(
      name = name,
      internalName = internalName,
      downloadUrl = downloadUrl,
      version = version,
      tvTypes = tvTypes,
      authors = authors,
      status = status,
      iconUrl = iconUrl,
      language = language,
      description = description,
      fileSize = fileSize,
    )
  }

  private data class RawPluginEntry(
    val name: String,
    val internalName: String,
    val downloadUrl: String,
    val version: Int,
    val tvTypes: List<TvType>,
    val authors: List<String>,
    val status: Int,
    val iconUrl: String?,
    val language: String?,
    val description: String?,
    val fileSize: Long?,
  )
}
