package com.nocturne.audio.data.repository

import com.nocturne.audio.BuildConfig
import com.nocturne.audio.model.Song
import com.nocturne.audio.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface SongsRepository {
    suspend fun getSongsByYearAndLanguage(year: Int, languageCode: String, offset: Int = 0, limit: Int = 20): Result<List<Song>>
    suspend fun searchSongs(query: String, year: Int? = null, languageCode: String? = null, genre: String? = null, offset: Int = 0, limit: Int = 20): Result<List<Song>>
    suspend fun getTop10ForYear(year: Int): Result<List<Song>>
}

class SongsRepositoryImpl : SongsRepository {

    override suspend fun getSongsByYearAndLanguage(year: Int, languageCode: String, offset: Int, limit: Int): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                val clientId = BuildConfig.JAMENDO_CLIENT_ID.ifBlank { "743b7b4c" }
                val dateBetween = "${year}-01-01_${year}-12-31"

                val queryParams = mutableMapOf(
                    "client_id" to clientId,
                    "format" to "json",
                    "limit" to limit.toString(),
                    "offset" to offset.toString(),
                    "datebetween" to dateBetween,
                    "order" to "popularity_total",
                    "audioformat" to "mp32"
                )

                if (languageCode.isNotBlank() && languageCode != "all") {
                    queryParams["lang"] = languageCode
                }

                val urlString = buildUrl("https://api.jamendo.com/v3.0/tracks/", queryParams)
                val songs = executeJamendoRequest(urlString, fallbackYear = year, fallbackLang = languageCode)
                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchSongs(
        query: String,
        year: Int?,
        languageCode: String?,
        genre: String?,
        offset: Int,
        limit: Int
    ): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                val clientId = BuildConfig.JAMENDO_CLIENT_ID.ifBlank { "743b7b4c" }
                val queryParams = mutableMapOf(
                    "client_id" to clientId,
                    "format" to "json",
                    "limit" to limit.toString(),
                    "offset" to offset.toString(),
                    "order" to "popularity_total",
                    "audioformat" to "mp32"
                )

                if (query.isNotBlank()) {
                    queryParams["search"] = query.trim()
                }

                if (year != null && year in 1900..2100) {
                    queryParams["datebetween"] = "${year}-01-01_${year}-12-31"
                }

                if (!languageCode.isNullOrBlank() && languageCode != "all") {
                    queryParams["lang"] = languageCode
                }

                if (!genre.isNullOrBlank() && genre != "all") {
                    queryParams["tags"] = genre.trim()
                }

                val urlString = buildUrl("https://api.jamendo.com/v3.0/tracks/", queryParams)
                val songs = executeJamendoRequest(urlString, fallbackYear = year ?: 0, fallbackLang = languageCode ?: "")
                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTop10ForYear(year: Int): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                val clientId = BuildConfig.JAMENDO_CLIENT_ID.ifBlank { "743b7b4c" }
                val dateBetween = "${year}-01-01_${year}-12-31"

                val queryParams = mapOf(
                    "client_id" to clientId,
                    "format" to "json",
                    "limit" to "10",
                    "datebetween" to dateBetween,
                    "order" to "popularity_total",
                    "audioformat" to "mp32"
                )

                val urlString = buildUrl("https://api.jamendo.com/v3.0/tracks/", queryParams)
                val songs = executeJamendoRequest(urlString, fallbackYear = year, fallbackLang = "")
                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun executeJamendoRequest(urlString: String, fallbackYear: Int, fallbackLang: String): List<Song> {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Jamendo API request failed with HTTP $responseCode")
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(responseText)
        val resultsArray = jsonObject.optJSONArray("results") ?: return emptyList()

        val songsList = mutableListOf<Song>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.getJSONObject(i)
            val id = item.optString("id", "")
            val title = item.optString("name", "Untitled")
            val artist = item.optString("artist_name", "Unknown Artist")
            val albumArtUrl = item.optString("album_image", "").replace("http://", "https://")
            var audioUrl = item.optString("audio", "").replace("http://", "https://")
            val duration = item.optInt("duration", 0)
            val releaseDate = item.optString("releasedate", "")

            val parsedYear = if (releaseDate.length >= 4) {
                releaseDate.substring(0, 4).toIntOrNull() ?: fallbackYear
            } else {
                fallbackYear
            }

            // Enforce HTTPS stream URL rule (Section 8)
            if (audioUrl.startsWith("http://")) {
                audioUrl = audioUrl.replace("http://", "https://")
            }

            if (id.isNotBlank() && audioUrl.isNotBlank()) {
                songsList.add(
                    Song(
                        id = "jamendo_$id",
                        title = title,
                        artist = artist,
                        albumArtUrl = albumArtUrl,
                        audioUrl = audioUrl,
                        durationSeconds = duration,
                        year = parsedYear,
                        language = fallbackLang,
                        source = SongSource.JAMENDO,
                        isFavorite = false
                    )
                )
            }
        }
        return songsList
    }

    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        val sb = StringBuilder(baseUrl)
        sb.append("?")
        params.entries.forEachIndexed { index, entry ->
            if (index > 0) sb.append("&")
            sb.append(URLEncoder.encode(entry.key, "UTF-8"))
            sb.append("=")
            sb.append(URLEncoder.encode(entry.value, "UTF-8"))
        }
        return sb.toString()
    }
}
