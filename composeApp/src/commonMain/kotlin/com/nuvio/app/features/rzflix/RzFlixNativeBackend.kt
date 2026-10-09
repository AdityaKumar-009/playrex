package com.nuvio.app.features.rzflix

import com.nuvio.app.features.addons.fetchAddonResponseText
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamSubtitle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Typed, ad-SDK-free boundary for a *verified* RzFlix-compatible JSON service.
 *
 * The uploaded RzFlix Flutter APK contains possible remote configuration URLs,
 * but not a verified catalog/metadata/stream HTTP contract. Callers supply the
 * exact confirmed HTTPS resource URLs. No assumed routes, credentials,
 * WebViews, ad redirects, or stream extraction bypasses are embedded here.
 *
 * Once the live contract is known, the resulting MetaPreview / MetaDetails /
 * StreamItem values can feed Nuvio's existing catalog and player pipelines.
 */
class RzFlixNativeBackend(
    private val fetchText: suspend (String) -> String = { fetchAddonResponseText(it) },
) {
    suspend fun discover(configurationUrl: String): RzFlixConfiguration {
        val url = requireHttpsUrl(configurationUrl)
        val text = fetchText(url).trim()
        val directUrl = text.takeIf { it.startsWith("https://", ignoreCase = true) }
        if (directUrl != null) {
            return RzFlixConfiguration.Endpoint(requireHttpsUrl(directUrl))
        }
        val root = runCatching { RzFlixPayloadMapper.parse(text) }.getOrNull()
            ?: return RzFlixConfiguration.Unknown("Response was neither an HTTPS URL nor valid JSON")
        if (root is JsonObject) {
            if (root["resources"] is JsonArray && root["id"] is JsonPrimitive) {
                return RzFlixConfiguration.StremioManifest(url)
            }
            val candidate = root.text("apiBaseUrl", "base_url", "api_url", "endpoint", "api", "url")
                ?: (root["data"] as? JsonObject)?.text(
                    "apiBaseUrl", "base_url", "api_url", "endpoint", "api", "url"
                )
            if (candidate != null) {
                return runCatching {
                    RzFlixConfiguration.Endpoint(requireHttpsUrl(candidate))
                }.getOrElse {
                    RzFlixConfiguration.Unknown("Remote configuration did not contain a valid HTTPS endpoint")
                }
            }
        }
        return RzFlixConfiguration.Unknown("Unknown remote configuration format")
    }

    suspend fun catalog(verifiedCatalogUrl: String): List<MetaPreview> =
        RzFlixPayloadMapper.catalog(fetchText(requireHttpsUrl(verifiedCatalogUrl)))

    suspend fun search(verifiedSearchUrl: String): List<MetaPreview> =
        catalog(verifiedSearchUrl)

    suspend fun details(verifiedDetailUrl: String): MetaDetails? =
        RzFlixPayloadMapper.details(fetchText(requireHttpsUrl(verifiedDetailUrl)))

    suspend fun streams(verifiedStreamUrl: String): List<StreamItem> =
        RzFlixPayloadMapper.streams(fetchText(requireHttpsUrl(verifiedStreamUrl)))

    companion object {
        /** Unverified URLs found in the user's RzFlix APK; configuration probes only. */
        val observedConfigurationUrls = listOf(
            "https://mainapi.yomoviesapk.com/r",
            "https://api.ringzstudio.com/apis/r.txt",
        )
    }
}

sealed interface RzFlixConfiguration {
    data class Endpoint(val url: String) : RzFlixConfiguration
    data class StremioManifest(val manifestUrl: String) : RzFlixConfiguration
    data class Unknown(val reason: String) : RzFlixConfiguration
}

/** Validate URLs before fetching or handing a link to the native player. */
internal fun requireHttpsUrl(candidate: String): String {
    val url = candidate.trim()
    require(url.startsWith("https://", ignoreCase = true)) { "HTTPS required" }
    val authority = url.substringAfter("://").substringBefore('/').substringBefore('?')
    require(authority.isNotBlank() && '.' in authority && '@' !in authority) {
        "Expected a hostname without embedded credentials"
    }
    require('#' !in url && ' ' !in url && '\n' !in url && '\r' !in url) {
        "Invalid URL"
    }
    return url
}

internal object RzFlixPayloadMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): JsonElement = json.parseToJsonElement(raw)

    fun catalog(raw: String): List<MetaPreview> =
        records(parse(raw), "metas", "results", "items", "movies", "shows", "content")
            .mapNotNull { entry ->
                val id = entry.text("id", "_id", "imdb_id", "imdbId", "tmdb_id", "slug")
                    ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = entry.text("name", "title", "movie_name")
                    ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                MetaPreview(
                    id = id,
                    type = normalizeType(entry.text("type", "content_type", "media_type")),
                    name = name,
                    poster = entry.text("poster", "posterUrl", "poster_path", "image", "thumbnail"),
                    banner = entry.text("background", "backdrop", "banner", "backdrop_path"),
                    description = entry.text("description", "overview", "plot"),
                    releaseInfo = entry.text("releaseInfo", "year", "release_year"),
                    imdbRating = entry.text("imdbRating", "imdb_rating", "rating"),
                    genres = entry.array("genres").mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
                )
            }
            .distinctBy { it.type + ":" + it.id }

    fun details(raw: String): MetaDetails? {
        val root = parse(raw)
        val item = ((root as? JsonObject)?.get("meta") as? JsonObject)
            ?: ((root as? JsonObject)?.get("data") as? JsonObject)
            ?: (root as? JsonObject) ?: return null
        val id = item.text("id", "_id", "imdb_id", "imdbId", "tmdb_id", "slug")
            ?: return null
        val name = item.text("name", "title", "movie_name") ?: return null
        val type = normalizeType(item.text("type", "content_type", "media_type"))
        val videos = records(item, "videos", "episodes")
            .mapNotNull { episode ->
                val videoId = episode.text("id", "videoId", "episode_id") ?: return@mapNotNull null
                MetaVideo(
                    id = videoId,
                    title = episode.text("title", "name") ?: "Episode",
                    season = episode.text("season", "season_number")?.toIntOrNull(),
                    episode = episode.text("episode", "episode_number")?.toIntOrNull(),
                    thumbnail = episode.text("thumbnail", "still", "poster"),
                    overview = episode.text("overview", "description"),
                )
            }
        return MetaDetails(
            id = id,
            type = type,
            name = name,
            imdbId = item.text("imdb_id", "imdbId"),
            poster = item.text("poster", "posterUrl", "image"),
            background = item.text("background", "backdrop", "banner"),
            description = item.text("description", "overview", "plot"),
            releaseInfo = item.text("releaseInfo", "year", "release_year"),
            imdbRating = item.text("imdbRating", "imdb_rating", "rating"),
            genres = item.array("genres").mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
            videos = videos,
        )
    }

    fun streams(raw: String): List<StreamItem> =
        records(parse(raw), "streams", "sources", "links", "results")
            .mapNotNull { source ->
                // Only native playable URLs. No external ad-click pages, iframe
                // destinations, JavaScript execution or interstitial redirects.
                val url = source.text("url", "file", "streamUrl", "stream_url", "src")
                    ?: return@mapNotNull null
                val safeUrl = runCatching { requireHttpsUrl(url) }.getOrNull()
                    ?: return@mapNotNull null
                val subtitleRecords = records(source, "subtitles", "captions")
                StreamItem(
                    addonName = "RzFlix (configured API)",
                    addonId = "rzflix:native",
                    name = source.text("name", "quality", "label") ?: "Stream",
                    title = source.text("title", "description"),
                    url = safeUrl,
                    externalSubtitles = subtitleRecords.mapNotNull { sub ->
                        val subUrl = sub.text("url", "src") ?: return@mapNotNull null
                        val secure = runCatching { requireHttpsUrl(subUrl) }.getOrNull()
                            ?: return@mapNotNull null
                        StreamSubtitle(
                            url = secure,
                            language = sub.text("lang", "language", "label") ?: "und",
                        )
                    },
                )
            }
            .distinctBy { it.url }

    private fun records(root: JsonElement, vararg keys: String): List<JsonObject> {
        val direct = root as? JsonArray
        if (direct != null) return direct.mapNotNull { it as? JsonObject }
        val obj = root as? JsonObject ?: return emptyList()
        keys.forEach { key ->
            val values = obj[key] as? JsonArray
            if (values != null) return values.mapNotNull { it as? JsonObject }
        }
        val nested = obj["data"]
        return if (nested != null && nested !== root) records(nested, *keys) else emptyList()
    }

    private fun normalizeType(raw: String?): String =
        when (raw?.trim()?.lowercase()) {
            "tv", "series", "show", "tvshow", "webseries", "web_series" -> "series"
            else -> "movie"
        }

    private fun JsonObject.text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }

    private fun JsonObject.array(key: String): JsonArray =
        this[key] as? JsonArray ?: JsonArray(emptyList())
}
