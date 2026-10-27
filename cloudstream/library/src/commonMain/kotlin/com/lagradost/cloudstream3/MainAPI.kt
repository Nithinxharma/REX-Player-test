package com.lagradost.cloudstream3

import kotlinx.serialization.Serializable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

enum class TvType {
    TvSeries, Anime, OVA, AnimeMovie, Cartoon, Documentary, Movie, Torrent, AsianDrama, Live, Others, NSFW, Music, AudioBook, CustomMedia, Audio, Podcast, Video
}

enum class DubStatus {
    None, Dubbed, Subbed
}

@Serializable
open class MainAPI {
    open var name: String = ""
    open var mainUrl: String = ""
    open var lang: String = "en"
    open var hasMainPage: Boolean = false
    open var canBeOverridden: Boolean = true
    open var providerType: ProviderType = ProviderType.OTHER
    open var supportedTypes: Set<TvType> = setOf(TvType.Movie, TvType.TvSeries)

    open suspend fun getMainPage(page: Int = 1, request: MainPageRequest? = null): HomePageResponse? {
        return null
    }

    open suspend fun search(query: String): List<SearchResponse>? {
        return null
    }

    open suspend fun load(url: String): LoadResponse? {
        return null
    }

    companion object {
        var settingsForProvider: SettingsJson? = null
    }
}

enum class ProviderType {
    ANIME, MOVIE, TV_SERIES, ASIAN_DRAMA, CARTOON, DOCUMENTARY, LIVE, MUSIC, OTHER
}

@Serializable
data class SettingsJson(
    var enableAdult: Boolean = false
)

@Serializable
open class SearchResponse(
    open var name: String,
    open var url: String,
    open var apiName: String,
    open var type: TvType? = null,
    open var posterUrl: String? = null,
    open var year: Int? = null,
)

@Serializable
open class HomePageList(
    val name: String,
    val items: List<SearchResponse>,
)

@Serializable
open class HomePageResponse(
    val items: List<HomePageList>,
)

@Serializable
data class MainPageRequest(
    val name: String,
    val data: String,
)

@Serializable
open class LoadResponse(
    open var name: String,
    open var url: String,
    open var apiName: String,
    open var type: TvType,
    open var posterUrl: String? = null,
    open var plot: String? = null,
    open var year: Int? = null,
)

object APIHolder {
    val allProviders = CopyOnWriteArrayList<MainAPI>()
    var apis: List<MainAPI> = emptyList()
    var apiMap: ConcurrentHashMap<String, MainAPI>? = null
    private val pluginMapping = ConcurrentHashMap<MainAPI, String>()

    fun getApiFromNameNull(name: String): MainAPI? {
        return allProviders.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun getApiFromUrlNull(url: String): MainAPI? {
        return allProviders.firstOrNull { it.mainUrl.equals(url, ignoreCase = true) }
    }

    fun removePluginMapping(api: MainAPI) {
        pluginMapping.remove(api)
    }

    fun initAll() {
        apis = allProviders.toList()
    }
}

const val AllLanguagesName = "all"
const val USER_PROVIDER_API = "USER_PROVIDER_API"
