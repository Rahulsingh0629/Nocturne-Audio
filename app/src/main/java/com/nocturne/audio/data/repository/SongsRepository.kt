package com.nocturne.audio.data.repository

import android.text.Html
import android.util.Base64
import com.nocturne.audio.model.Song
import com.nocturne.audio.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

interface SongsRepository {
    suspend fun getSongsByYearAndLanguage(year: Int, languageCode: String, offset: Int = 0, limit: Int = 20): Result<List<Song>>
    suspend fun searchSongs(query: String, year: Int? = null, languageCode: String? = null, genre: String? = null, offset: Int = 0, limit: Int = 20): Result<List<Song>>
    suspend fun getTop10ForYear(year: Int): Result<List<Song>>
}

class SongsRepositoryImpl : SongsRepository {

    override suspend fun getSongsByYearAndLanguage(
        year: Int,
        languageCode: String,
        offset: Int,
        limit: Int
    ): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                val langName = mapLanguageCodeToName(languageCode)
                val saavnQuery = if (langName.isBlank() || langName.equals("all", ignoreCase = true)) {
                    "Bollywood $year"
                } else {
                    "$langName $year"
                }

                val page = (offset / limit) + 1
                val songs = fetchJioSaavnSongs(query = saavnQuery, page = page, limit = limit, fallbackYear = year, fallbackLang = langName)

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
                val queryParts = mutableListOf<String>()
                if (query.isNotBlank()) queryParts.add(query.trim())
                if (!genre.isNullOrBlank() && !genre.equals("all", ignoreCase = true)) queryParts.add(genre.trim())
                if (!languageCode.isNullOrBlank() && !languageCode.equals("all", ignoreCase = true)) {
                    queryParts.add(mapLanguageCodeToName(languageCode))
                }
                if (year != null && year in 1900..2100) queryParts.add(year.toString())

                val saavnQuery = if (queryParts.isNotEmpty()) queryParts.joinToString(" ") else "Bollywood 2026"
                val page = (offset / limit) + 1

                val songs = fetchJioSaavnSongs(query = saavnQuery, page = page, limit = limit, fallbackYear = year ?: 0, fallbackLang = languageCode ?: "")
                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTop10ForYear(year: Int): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                val query = "Bollywood $year"
                val songs = fetchJioSaavnSongs(query = query, page = 1, limit = 10, fallbackYear = year, fallbackLang = "")
                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun fetchJioSaavnSongs(query: String, page: Int, limit: Int, fallbackYear: Int, fallbackLang: String): List<Song> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urlDev = "https://saavn.dev/api/search/songs?query=$encodedQuery&page=$page&limit=$limit"

        val devResults = tryFetchUrl(urlDev)
        if (!devResults.isNullOrBlank()) {
            val songs = parseSaavnDevJson(devResults, fallbackYear, fallbackLang)
            if (songs.isNotEmpty()) return songs
        }

        val urlOfficial = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&q=$encodedQuery&n=$limit&p=$page"
        val officialResults = tryFetchUrl(urlOfficial)
        if (!officialResults.isNullOrBlank()) {
            val songs = parseJioSaavnOfficialJson(officialResults, fallbackYear, fallbackLang)
            if (songs.isNotEmpty()) return songs
        }

        return emptyList()
    }

    private fun tryFetchUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSaavnDevJson(responseText: String, fallbackYear: Int, fallbackLang: String): List<Song> {
        val jsonObject = JSONObject(responseText)
        val dataObj = jsonObject.optJSONObject("data")
        val resultsArray = dataObj?.optJSONArray("results")
            ?: jsonObject.optJSONArray("results")
            ?: jsonObject.optJSONArray("data")
            ?: return emptyList()

        val songsList = mutableListOf<Song>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.optJSONObject(i) ?: continue

            val id = item.optString("id", "")
            val rawName = item.optString("name", "").ifBlank { item.optString("title", "Untitled") }
            val title = cleanHtmlText(rawName)

            var artist = item.optString("primaryArtists", "").ifBlank { item.optString("artist", "") }
            if (artist.isBlank()) {
                val artistsObj = item.optJSONObject("artists")
                val primaryArr = artistsObj?.optJSONArray("primary")
                if (primaryArr != null && primaryArr.length() > 0) {
                    val names = mutableListOf<String>()
                    for (j in 0 until primaryArr.length()) {
                        val name = primaryArr.optJSONObject(j)?.optString("name", "") ?: ""
                        if (name.isNotBlank()) names.add(name)
                    }
                    artist = names.joinToString(", ")
                }
            }
            artist = cleanHtmlText(artist.ifBlank { "Unknown Artist" })

            val imageArray = item.optJSONArray("image")
            var albumArtUrl = extractUrlFromMediaArray(imageArray)
            if (albumArtUrl.isNotBlank()) {
                albumArtUrl = albumArtUrl.replace("150x150", "500x500").replace("50x50", "500x500")
            }

            val downloadUrlArray = item.optJSONArray("downloadUrl")
            var audioUrl = extractUrlFromMediaArray(downloadUrlArray)

            if (audioUrl.isBlank()) {
                audioUrl = item.optJSONObject("more_info")?.optString("vlink", "") ?: ""
            }

            if (audioUrl.isBlank()) continue

            if (audioUrl.startsWith("http://")) {
                audioUrl = audioUrl.replace("http://", "https://")
            }

            val durationSeconds = item.optInt("duration", 0)
            val itemYear = item.optInt("year", 0)
            val safeYear = if (itemYear in 1900..2100) itemYear else fallbackYear

            songsList.add(
                Song(
                    id = "saavn_$id",
                    title = title,
                    artist = artist,
                    albumArtUrl = albumArtUrl,
                    audioUrl = audioUrl,
                    durationSeconds = durationSeconds,
                    year = safeYear,
                    language = fallbackLang,
                    source = SongSource.SAAVN,
                    isFavorite = false
                )
            )
        }
        return songsList
    }

    private fun parseJioSaavnOfficialJson(responseText: String, fallbackYear: Int, fallbackLang: String): List<Song> {
        val jsonObject = JSONObject(responseText)
        val resultsArray = jsonObject.optJSONArray("results") ?: return emptyList()

        val songsList = mutableListOf<Song>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.optJSONObject(i) ?: continue

            val id = item.optString("id", "")
            val rawTitle = item.optString("title", "").ifBlank { item.optString("name", "Untitled") }
            val title = cleanHtmlText(rawTitle)

            val rawSubtitle = item.optString("subtitle", "")
            val artist = cleanHtmlText(rawSubtitle.ifBlank { "Unknown Artist" })

            var albumArtUrl = item.optString("image", "").replace("http://", "https://")
            if (albumArtUrl.isNotBlank()) {
                albumArtUrl = albumArtUrl.replace("150x150", "500x500").replace("50x50", "500x500")
            }

            val moreInfo = item.optJSONObject("more_info")
            var audioUrl = moreInfo?.optString("vlink", "") ?: ""

            if (audioUrl.isBlank()) {
                val encUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                if (encUrl.isNotBlank()) {
                    audioUrl = decryptJioSaavnMediaUrl(encUrl)
                }
            }

            if (audioUrl.isBlank()) continue

            if (audioUrl.startsWith("http://")) {
                audioUrl = audioUrl.replace("http://", "https://")
            }

            val durationStr = moreInfo?.optString("duration", "0") ?: "0"
            val durationSeconds = durationStr.toIntOrNull() ?: 0

            val itemYear = item.optString("year", "0").toIntOrNull() ?: 0
            val safeYear = if (itemYear in 1900..2100) itemYear else fallbackYear

            songsList.add(
                Song(
                    id = "saavn_$id",
                    title = title,
                    artist = artist,
                    albumArtUrl = albumArtUrl,
                    audioUrl = audioUrl,
                    durationSeconds = durationSeconds,
                    year = safeYear,
                    language = fallbackLang,
                    source = SongSource.SAAVN,
                    isFavorite = false
                )
            )
        }
        return songsList
    }

    private fun decryptJioSaavnMediaUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        return try {
            val keyBytes = "38588500".toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val urlStr = String(decryptedBytes, Charsets.UTF_8).trim()
            if (urlStr.startsWith("http://")) urlStr.replace("http://", "https://") else urlStr
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractUrlFromMediaArray(mediaArray: JSONArray?): String {
        if (mediaArray == null || mediaArray.length() == 0) return ""
        for (i in mediaArray.length() - 1 downTo 0) {
            val obj = mediaArray.optJSONObject(i) ?: continue
            val url = obj.optString("url", "").replace("http://", "https://")
            if (url.isNotBlank()) return url
        }
        return ""
    }

    private fun cleanHtmlText(rawText: String): String {
        if (rawText.isBlank()) return ""
        return try {
            Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (_: Exception) {
            rawText.replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#039;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim()
        }
    }

    private fun mapLanguageCodeToName(code: String): String {
        return when (code.lowercase().trim()) {
            "en", "english" -> "English"
            "hi", "hindi" -> "Hindi"
            "pa", "punjabi" -> "Punjabi"
            "es", "spanish" -> "Spanish"
            "ta", "tamil" -> "Tamil"
            "ko", "korean" -> "Korean"
            "ja", "japanese" -> "Japanese"
            "all", "" -> ""
            else -> code.replaceFirstChar { it.uppercase() }
        }
    }
}
