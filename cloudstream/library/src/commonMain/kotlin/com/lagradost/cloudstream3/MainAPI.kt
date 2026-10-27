package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

enum class TvType {
    TvSeries, Anime, OVA, AnimeMovie, Cartoon, Documentary, Movie, Torrent, AsianDrama, Live, Others, NSFW, Music, AudioBook, CustomMedia, Audio, Podcast, Video
}

enum class DubStatus {
    None, Dubbed, Subbed
}

enum class SearchQuality {
    BlueRay, Cam, CamRip, DVD, FourK, HD, HDR, HQ, HdCam, SD, SDR, Telecine, Telesync, UHD, WebRip, WorkPrint
}

@Serializable
open class MainAPI {
    open val name: String = ""
    open val mainUrl: String = ""
    open val lang: String = "en"
    open val hasMainPage: Boolean = false
    open val canBeOverridden: Boolean = true
    open val providerType: ProviderType = ProviderType.OTHER
    open val supportedTypes: Set<TvType> = setOf(TvType.Movie, TvType.TvSeries)
    open val mainPage: List<MainPageRequest> = emptyList()
    open val usesWebView: Boolean = false
    @Transient open val supportedSyncNames: Set<Any> = emptySet()

    open suspend fun getLoadUrl(name: Any, id: String): String? = null
    open suspend fun getLoadUrl(name: String, id: String): String? = null

    open suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        return null
    }

    open suspend fun search(query: String): List<SearchResponse>? {
        return null
    }

    open suspend fun search(query: String, page: Int): SearchResponseList? {
        return null
    }

    open suspend fun load(url: String): LoadResponse? {
        return null
    }

    open suspend fun loadLinks(
        data: String,
        isCasting: Boolean = false,
        subtitleCallback: (SubtitleFile) -> Unit = {},
        callback: (ExtractorLink) -> Unit = {}
    ): Boolean {
        return false
    }

    companion object {
        var settingsForProvider: SettingsJson? = null
    }
}

enum class ProviderType {
    ANIME, MOVIE, TV_SERIES, ASIAN_DRAMA, CARTOON, DOCUMENTARY, LIVE, MUSIC, MetaProvider, OTHER
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
    open var id: Int? = null
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
    open var showStatus: ShowStatus? = null,
    open var score: Score? = null,
    open var tags: List<String>? = null,
    open var duration: Int? = null,
    open var recommendations: List<SearchResponse>? = null,
    open var actors: List<ActorData>? = null,
    open var comingSoon: Boolean = false,
    open var backgroundPosterUrl: String? = null,
    open var contentRating: String? = null,
    open var nextAiring: NextAiring? = null,
    open var logoUrl: String? = null,
    open var uniqueUrl: String? = null
) {
    companion object {
        fun LoadResponse.addTrailer(url: String?) {}
        fun LoadResponse.addTrailer(urls: List<String>?) {}
        fun LoadResponse.addTrailer(url: String?, addRaw: Boolean = false, headers: Map<String, String>? = null) {}
        fun LoadResponse.addImdbId(id: String?) {}
        fun LoadResponse.addTMDbId(id: String?) {}
        fun LoadResponse.addActors(actors: List<ActorData>?) {}
        @JvmName("addActorsPairs")
        fun LoadResponse.addActors(actors: List<Pair<Actor, String?>>?) {}
        fun LoadResponse.addDate(date: String?, format: String? = null) {}
        fun LoadResponse.addDate(date: Long?) {}
    }
}

object APIHolder {
    suspend fun getCaptchaToken(url: String, key: String? = null, referer: String? = null): String? = null
    val unixTimeMS: Long get() = System.currentTimeMillis()
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
