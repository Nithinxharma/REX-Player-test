package com.lagradost.cloudstream3

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal to Cloudstream and should not be used outside of it."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
annotation class InternalAPI

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental or prerelease."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Prerelease

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API uses unsafe SSL settings."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class UnsafeSSL

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

val mapper = jacksonObjectMapper().apply {
    configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

class ErrorLoadingException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

@Serializable
data class SubtitleFile(
    val lang: String = "",
    val url: String = ""
)

fun newSubtitleFile(lang: String, url: String): SubtitleFile {
    return SubtitleFile(lang = lang, url = url)
}

@Serializable
data class AudioFile(
    val url: String,
    val lang: String? = null
)

fun newAudioFile(url: String, lang: String? = null): AudioFile {
    return AudioFile(url = url, lang = lang)
}

interface IDownloadableMinimum {
    val url: String
    var referer: String
    var headers: Map<String, String>
}

suspend fun getCaptchaToken(url: String, key: String? = null, referer: String? = null): String? = null

fun base64Decode(s: String?): String {
    if (s == null) return ""
    return try {
        String(Base64.getDecoder().decode(s), Charsets.UTF_8)
    } catch (_: Exception) {
        s
    }
}

fun base64DecodeArray(s: String?): ByteArray {
    if (s == null) return ByteArray(0)
    return try {
        Base64.getDecoder().decode(s)
    } catch (_: Exception) {
        ByteArray(0)
    }
}

fun base64DecodeArray(b: ByteArray?): ByteArray = b ?: ByteArray(0)

fun base64Encode(s: String?): String {
    if (s == null) return ""
    return Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
}

fun base64Encode(b: ByteArray?): String {
    if (b == null) return ""
    return Base64.getEncoder().encodeToString(b)
}

@JvmName("base64DecodeExt")
fun String?.base64Decode(): String = base64Decode(this)
@JvmName("base64DecodeArrayExt")
fun String?.base64DecodeArray(): ByteArray = base64DecodeArray(this)
@JvmName("base64EncodeExt")
fun String?.base64Encode(): String = base64Encode(this)
@JvmName("base64DecodeByteArrayExt")
fun ByteArray?.base64DecodeArray(): ByteArray = base64DecodeArray(this)
@JvmName("base64EncodeByteArrayExt")
fun ByteArray?.base64Encode(): String = base64Encode(this)

// Responses & Data Models

enum class ShowStatus {
    Completed, Ongoing, Upcoming, Offline
}

@Serializable
data class Score(
    val score: Double? = null,
    val maxScore: Double? = null
) {
    companion object {
        fun from10(score: Double?): Score = Score(score, 10.0)
    }
}

@Serializable
data class Actor(
    val name: String,
    val image: String? = null
)

@Serializable
data class ActorData(
    val actor: Actor,
    val role: String? = null,
    val roleString: String? = null,
    val voiceActor: Actor? = null
)

@Serializable
data class Episode(
    var name: String? = null,
    var season: Int? = null,
    var episode: Int? = null,
    var data: String = "",
    var posterUrl: String? = null,
    var description: String? = null,
    var date: Long? = null,
    var score: Score? = null,
    var runTime: Int? = null
)

@Serializable
data class NextAiring(
    val episode: Int = 0,
    val unixTime: Long = 0L,
    val date: Long = 0L,
    val season: Int? = null
)

open class MovieSearchResponse(
    name: String,
    url: String,
    apiName: String,
    type: TvType? = TvType.Movie,
    posterUrl: String? = null,
    year: Int? = null,
    id: Int? = null,
    var score: Score? = null
) : SearchResponse(name, url, apiName, type, posterUrl, year, id)

open class TvSeriesSearchResponse(
    name: String,
    url: String,
    apiName: String,
    type: TvType? = TvType.TvSeries,
    posterUrl: String? = null,
    year: Int? = null,
    id: Int? = null,
    var score: Score? = null
) : SearchResponse(name, url, apiName, type, posterUrl, year, id)

open class MovieLoadResponse(
    name: String,
    url: String,
    apiName: String,
    type: TvType = TvType.Movie,
    posterUrl: String? = null,
    plot: String? = null,
    year: Int? = null,
    var dataUrl: String = ""
) : LoadResponse(name, url, apiName, type, posterUrl, plot, year)

open class TvSeriesLoadResponse(
    name: String,
    url: String,
    apiName: String,
    type: TvType = TvType.TvSeries,
    posterUrl: String? = null,
    plot: String? = null,
    year: Int? = null,
    var dataUrl: String = "",
    episodes: List<Episode> = emptyList()
) : LoadResponse(name, url, apiName, type, posterUrl, plot, year) {
    var episodes: List<Episode> = episodes
}

data class SearchResponseList(
    val items: List<SearchResponse> = emptyList()
)

fun newSearchResponseList(items: List<SearchResponse>): SearchResponseList = SearchResponseList(items)
fun List<SearchResponse>.toNewSearchResponseList(): SearchResponseList = SearchResponseList(this)

inline fun newMovieLoadResponse(
    name: String,
    url: String,
    type: TvType,
    dataUrl: String = "",
    builder: MovieLoadResponse.() -> Unit = {}
): MovieLoadResponse {
    val res = MovieLoadResponse(name, url, "", type, dataUrl = dataUrl)
    res.builder()
    return res
}

inline fun newMovieSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.Movie,
    fix: Boolean = true,
    builder: MovieSearchResponse.() -> Unit = {}
): MovieSearchResponse {
    val res = MovieSearchResponse(name, url, "", type)
    res.builder()
    return res
}

inline fun newTvSeriesLoadResponse(
    name: String,
    url: String,
    type: TvType,
    dataUrl: String = "",
    builder: TvSeriesLoadResponse.() -> Unit = {}
): TvSeriesLoadResponse {
    val res = TvSeriesLoadResponse(name, url, "", type, dataUrl = dataUrl)
    res.builder()
    return res
}

inline fun newTvSeriesLoadResponse(
    name: String,
    url: String,
    type: TvType,
    episodes: List<Episode>,
    builder: TvSeriesLoadResponse.() -> Unit = {}
): TvSeriesLoadResponse {
    val res = TvSeriesLoadResponse(name, url, "", type, episodes = episodes)
    res.builder()
    return res
}

inline fun newTvSeriesSearchResponse(
    name: String,
    url: String,
    type: TvType = TvType.TvSeries,
    fix: Boolean = true,
    builder: TvSeriesSearchResponse.() -> Unit = {}
): TvSeriesSearchResponse {
    val res = TvSeriesSearchResponse(name, url, "", type)
    res.builder()
    return res
}

inline fun newEpisode(data: String, builder: Episode.() -> Unit = {}): Episode {
    val ep = Episode(data = data)
    ep.builder()
    return ep
}

fun newHomePageResponse(name: String, items: List<SearchResponse>, hasNext: Boolean = false): HomePageResponse {
    return HomePageResponse(listOf(HomePageList(name, items)))
}

fun newHomePageResponse(items: List<HomePageList>, hasNext: Boolean = false): HomePageResponse {
    return HomePageResponse(items)
}

fun mainPageOf(vararg pairs: Pair<String, String>): List<MainPageRequest> {
    return pairs.map { MainPageRequest(it.first, it.second) }
}

fun isUpcoming(date: String?): Boolean = false

fun Episode.addDate(date: String?, format: String? = null) {}
fun Episode.addDate(date: Long?) {}

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

var LoadResponse.uniqueUrl: String
    get() = url
    set(value) {}
