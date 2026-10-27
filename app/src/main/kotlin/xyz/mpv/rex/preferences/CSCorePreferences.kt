package xyz.mpv.rex.preferences

import com.lagradost.cloudstream3.AllLanguagesName
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.TvType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.mpv.rex.preferences.preference.PreferenceStore

@Serializable
data class CSCoreRepo(
  val name: String,
  val url: String,
  val iconUrl: String? = null,
)

class CSCorePreferences(
  private val preferenceStore: PreferenceStore,
) {
  companion object {
    val DEFAULT_PREFERRED_MEDIA: Set<String> =
      TvType.entries.filter { it != TvType.NSFW }.map { it.ordinal.toString() }.toSet()
    val DEFAULT_DUB_STATUS: Set<String> =
      DubStatus.entries.map { it.name }.toSet()

    const val KEY_REPOSITORIES = "REPOSITORIES_KEY"
    const val KEY_AUTO_UPDATE_PLUGINS = "auto_update_plugins"
    const val KEY_AUTO_DOWNLOAD_PLUGINS = "auto_download_plugins_key2"
    const val KEY_PREFER_MEDIA_TYPE = "prefer_media_type_key_2"
    const val KEY_PROVIDER_LANG = "provider_lang_key"
    const val KEY_DISPLAY_SUB = "display_sub_key"
    const val KEY_DNS = "dns_key"
    const val KEY_JSDELIVR_PROXY = "jsdelivr_proxy_key"
    const val KEY_DOWNLOAD_PARALLEL = "download_parallel_key"
    const val KEY_DOWNLOAD_CONCURRENT = "download_concurrent_key"
    const val KEY_USER_PROVIDER_API = "USER_PROVIDER_API"

    const val KEY_OPENSUBTITLES_USERNAME = "cs_opensubtitles_username"
    const val KEY_OPENSUBTITLES_PASSWORD = "cs_opensubtitles_password"
    const val KEY_ANILIST_TOKEN = "cs_anilist_token"
    const val KEY_MAL_TOKEN = "cs_mal_token"
    const val KEY_KITSU_TOKEN = "cs_kitsu_token"
    const val KEY_SIMKL_TOKEN = "cs_simkl_token"
    const val KEY_ANIMESKIP_TOKEN = "cs_animeskip_token"

    val PREBUILT_REPOSITORIES: List<CSCoreRepo> = listOf(
      CSCoreRepo(
        name = "MegaRepo",
        url = "https://raw.githubusercontent.com/recloudstream/megarepo/master/repo.json",
      ),
      CSCoreRepo(
        name = "English Providers",
        url = "https://raw.githubusercontent.com/recloudstream/english-providers/master/repo.json",
      ),
      CSCoreRepo(
        name = "Multilingual Providers",
        url = "https://raw.githubusercontent.com/recloudstream/multilingual-providers/master/repo.json",
      ),
    )

    private val jsonFormat = Json {
      ignoreUnknownKeys = true
      isLenient = true
      encodeDefaults = true
    }
  }

  val autoUpdatePlugins = preferenceStore.getBoolean(KEY_AUTO_UPDATE_PLUGINS, true)
  val autoDownloadPlugins = preferenceStore.getInt(KEY_AUTO_DOWNLOAD_PLUGINS, 0)
  val repositoriesJson = preferenceStore.getString(KEY_REPOSITORIES, "[]")

  val preferredMediaTypes = preferenceStore.getStringSet(KEY_PREFER_MEDIA_TYPE, DEFAULT_PREFERRED_MEDIA)
  val providerLanguages = preferenceStore.getStringSet(KEY_PROVIDER_LANG, setOf(AllLanguagesName))
  val displayDubSub = preferenceStore.getStringSet(KEY_DISPLAY_SUB, DEFAULT_DUB_STATUS)

  val dnsProvider = preferenceStore.getInt(KEY_DNS, 0)
  val jsdelivrProxy = preferenceStore.getBoolean(KEY_JSDELIVR_PROXY, false)
  val parallelDownloads = preferenceStore.getInt(KEY_DOWNLOAD_PARALLEL, 3)
  val concurrentConnections = preferenceStore.getInt(KEY_DOWNLOAD_CONCURRENT, 3)
  val customSites = preferenceStore.getString(KEY_USER_PROVIDER_API, "[]")

  val openSubtitlesUsername = preferenceStore.getString(KEY_OPENSUBTITLES_USERNAME, "")
  val openSubtitlesPassword = preferenceStore.getString(KEY_OPENSUBTITLES_PASSWORD, "")
  val anilistToken = preferenceStore.getString(KEY_ANILIST_TOKEN, "")
  val malToken = preferenceStore.getString(KEY_MAL_TOKEN, "")
  val kitsuToken = preferenceStore.getString(KEY_KITSU_TOKEN, "")
  val simklToken = preferenceStore.getString(KEY_SIMKL_TOKEN, "")
  val animeSkipToken = preferenceStore.getString(KEY_ANIMESKIP_TOKEN, "")

  fun isNsfwAllowed(): Boolean {
    return preferredMediaTypes.get().contains(TvType.NSFW.ordinal.toString())
  }

  fun setNsfwAllowed(allowed: Boolean) {
    val current = preferredMediaTypes.get().toMutableSet()
    val nsfwOrdinal = TvType.NSFW.ordinal.toString()
    if (allowed) {
      current.add(nsfwOrdinal)
    } else {
      current.remove(nsfwOrdinal)
    }
    preferredMediaTypes.set(current)
  }

  fun getRepositories(): List<CSCoreRepo> {
    val raw = repositoriesJson.get()
    val customRepos = if (raw.isNotBlank() && raw != "[]") {
      runCatching {
        jsonFormat.decodeFromString<List<CSCoreRepo>>(raw)
      }.getOrElse { emptyList() }
    } else {
      emptyList()
    }
    return (PREBUILT_REPOSITORIES + customRepos).distinctBy { it.url }
  }

  fun addCustomRepository(name: String, url: String) {
    val trimmedUrl = url.trim()
    val trimmedName = name.trim().ifEmpty { trimmedUrl }
    if (trimmedUrl.isBlank()) return

    val currentRaw = repositoriesJson.get()
    val currentCustom = if (currentRaw.isNotBlank() && currentRaw != "[]") {
      runCatching {
        jsonFormat.decodeFromString<List<CSCoreRepo>>(currentRaw)
      }.getOrElse { emptyList() }
    } else {
      emptyList()
    }.toMutableList()

    if (currentCustom.none { it.url.equals(trimmedUrl, ignoreCase = true) }) {
      currentCustom.add(CSCoreRepo(name = trimmedName, url = trimmedUrl))
      repositoriesJson.set(jsonFormat.encodeToString(currentCustom))
    }
  }

  fun removeCustomRepository(url: String) {
    val currentRaw = repositoriesJson.get()
    if (currentRaw.isBlank() || currentRaw == "[]") return
    val currentCustom = runCatching {
      jsonFormat.decodeFromString<List<CSCoreRepo>>(currentRaw)
    }.getOrElse { emptyList() }.toMutableList()

    val removed = currentCustom.removeAll { it.url.equals(url, ignoreCase = true) }
    if (removed) {
      repositoriesJson.set(jsonFormat.encodeToString(currentCustom))
    }
  }
}
