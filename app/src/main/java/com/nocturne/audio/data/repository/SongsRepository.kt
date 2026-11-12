package com.nocturne.audio.data.repository

import android.text.Html
import android.util.Base64
import com.nocturne.audio.model.Song
import com.nocturne.audio.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

interface SongsRepository {
    suspend fun getSongsByYearAndLanguage(year: Int, languageCode: String, offset: Int = 0, limit: Int = 30): Result<List<Song>>
    suspend fun searchSongs(query: String, year: Int? = null, languageCode: String? = null, genre: String? = null, offset: Int = 0, limit: Int = 30): Result<List<Song>>
    suspend fun getTop10ForYear(year: Int): Result<List<Song>>
}

class SongsRepositoryImpl : SongsRepository {

    private val dynamicSearchPool = listOf(
        "Top Hindi English Songs 2026", "Trending Hits 2026", "Bollywood Classics",
        "Top Bollywood Songs", "Punjabi Hits 2026", "Romantic Hits 2026", "Arijit Singh Hits"
    )

    override suspend fun getSongsByYearAndLanguage(
        year: Int,
        languageCode: String,
        offset: Int,
        limit: Int
    ): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                val langName = mapLanguageCodeToName(languageCode)
                val isAllLanguages = langName.isBlank() || langName.equals("all", ignoreCase = true)

                val songs = if (isAllLanguages && offset == 0) {
                    fetchDynamicFreshMix(year, limit)
                } else {
                    val saavnQuery = if (isAllLanguages) {
                        "Trending Hits $year"
                    } else {
                        "$langName $year"
                    }
                    val page = (offset / limit) + 1
                    fetchJioSaavnSongs(query = saavnQuery, page = page, limit = limit, fallbackYear = year, fallbackLang = langName)
                }

                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchDynamicFreshMix(year: Int, limit: Int): List<Song> = coroutineScope {
        val pickedQueries = dynamicSearchPool.shuffled().take(2)
        val deferred1 = async { fetchJioSaavnSongs(pickedQueries[0], page = 1, limit = 20, fallbackYear = year, fallbackLang = "") }
        val deferred2 = async { fetchJioSaavnSongs(pickedQueries[1], page = 1, limit = 20, fallbackYear = year, fallbackLang = "") }

        val list1 = deferred1.await()
        val list2 = deferred2.await()

        val combined = (list1 + list2).distinctBy { it.id }.shuffled()
        if (combined.isNotEmpty()) combined.take(limit) else fetchJioSaavnSongs("Top Hindi English Songs $year", page = 1, limit = limit, fallbackYear = year, fallbackLang = "")
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

                val saavnQuery = if (queryParts.isNotEmpty()) queryParts.joinToString(" ") else "Top Songs 2026"
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

        // 1. Primary Endpoint: saavn.sumit.co
        val urlSumit = "https://saavn.sumit.co/api/search/songs?query=$encodedQuery&limit=$limit&page=$page"
        val sumitResults = tryFetchUrl(urlSumit)
        if (!sumitResults.isNullOrBlank()) {
            val songs = parseSaavnDevJson(sumitResults, fallbackYear, fallbackLang)
            if (songs.isNotEmpty()) return songs
        }

        // 2. Fallback Endpoint 1: jiosaavn-api-privatecvc2.vercel.app
        val urlPrivate = "https://jiosaavn-api-privatecvc2.vercel.app/search/songs?query=$encodedQuery&limit=$limit&page=$page"
        val privateResults = tryFetchUrl(urlPrivate)
        if (!privateResults.isNullOrBlank()) {
            val songs = parseSaavnDevJson(privateResults, fallbackYear, fallbackLang)
            if (songs.isNotEmpty()) return songs
        }

        // 3. Fallback Endpoint 2: jiosaavn-api-2.vercel.app
        val urlVercel2 = "https://jiosaavn-api-2.vercel.app/search/songs?query=$encodedQuery&limit=$limit&page=$page"
        val vercel2Results = tryFetchUrl(urlVercel2)
        if (!vercel2Results.isNullOrBlank()) {
            val songs = parseSaavnDevJson(vercel2Results, fallbackYear, fallbackLang)
            if (songs.isNotEmpty()) return songs
        }

        // 4. Fallback Endpoint 3: Official JioSaavn API
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
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
            ?: dataObj?.optJSONArray("data")
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

            // Strictly extract full-length song audio URL and fallback download URLs (never 3-second preview/Jiotune clip)
            val extraction = extractAudioUrls(item)
            var audioUrl = extraction.audioUrl
            val downloadUrls = extraction.downloadUrls
            if (audioUrl.isBlank() || audioUrl.contains("jiotunepreview")) continue

            if (audioUrl.startsWith("http://")) {
                audioUrl = audioUrl.replace("http://", "https://")
            }

            val durationRaw = item.optInt("duration", 0)
            val durationStr = item.optString("duration", "0").toIntOrNull() ?: 0
            val moreInfoDuration = item.optJSONObject("more_info")?.optString("duration", "0")?.toIntOrNull() ?: 0
            val durationSeconds = if (durationRaw > 0) durationRaw else if (durationStr > 0) durationStr else moreInfoDuration

            val itemYear = item.optInt("year", 0)
            val safeYear = if (itemYear in 1900..2100) itemYear else fallbackYear

            songsList.add(
                Song(
                    id = "saavn_$id",
                    title = title,
                    artist = artist,
                    albumArtUrl = albumArtUrl,
                    audioUrl = audioUrl,
                    downloadUrls = downloadUrls,
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

            // Strictly extract full-length audio URL and fallback download URLs (never jiotunepreview)
            val extraction = extractAudioUrls(item)
            var audioUrl = extraction.audioUrl
            val downloadUrls = extraction.downloadUrls
            if (audioUrl.isBlank() || audioUrl.contains("jiotunepreview")) continue

            if (audioUrl.startsWith("http://")) {
                audioUrl = audioUrl.replace("http://", "https://")
            }

            val moreInfo = item.optJSONObject("more_info")
            val durationStr = moreInfo?.optString("duration", "0") ?: "0"
            val durationSeconds = durationStr.toIntOrNull() ?: item.optInt("duration", 0)

            val itemYear = item.optString("year", "0").toIntOrNull() ?: item.optInt("year", 0)
            val safeYear = if (itemYear in 1900..2100) itemYear else fallbackYear

            songsList.add(
                Song(
                    id = "saavn_$id",
                    title = title,
                    artist = artist,
                    albumArtUrl = albumArtUrl,
                    audioUrl = audioUrl,
                    downloadUrls = downloadUrls,
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

    data class AudioExtractionResult(val audioUrl: String, val downloadUrls: List<String>)

    private fun extractAudioUrls(item: JSONObject): AudioExtractionResult {
        val urlsList = mutableListOf<String>()

        val downloadUrlArray = item.optJSONArray("downloadUrl")
        if (downloadUrlArray != null && downloadUrlArray.length() > 0) {
            for (i in 0 until downloadUrlArray.length()) {
                val obj = downloadUrlArray.optJSONObject(i) ?: continue
                val url = obj.optString("url", "").ifBlank { obj.optString("link", "") }.replace("http://", "https://")
                if (url.isNotBlank() && !url.contains("jiotunepreview") && !urlsList.contains(url)) {
                    urlsList.add(url)
                }
            }
        }

        val moreInfo = item.optJSONObject("more_info")
        val encUrl = moreInfo?.optString("encrypted_media_url", "")?.ifBlank { item.optString("encrypted_media_url", "") } ?: ""
        if (encUrl.isNotBlank()) {
            val decrypted = decryptJioSaavnMediaUrl(encUrl)
            if (decrypted.isNotBlank() && !decrypted.contains("jiotunepreview") && !urlsList.contains(decrypted)) {
                urlsList.add(decrypted)
            }
        }

        val directUrl = (moreInfo?.optString("media_url", "") ?: "").ifBlank { item.optString("media_url", "") }.replace("http://", "https://")
        if (directUrl.isNotBlank() && !directUrl.contains("jiotunepreview") && !urlsList.contains(directUrl)) {
            urlsList.add(directUrl)
        }

        val best320 = urlsList.find { it.contains("320") || it.contains("high") }
        val best160 = urlsList.find { it.contains("160") || it.contains("128") }
        val primaryUrl = best320 ?: best160 ?: urlsList.firstOrNull() ?: ""

        val finalDownloadUrls = mutableListOf<String>()
        if (primaryUrl.isNotBlank()) finalDownloadUrls.add(primaryUrl)
        for (url in urlsList) {
            if (!finalDownloadUrls.contains(url)) {
                finalDownloadUrls.add(url)
            }
        }

        return AudioExtractionResult(primaryUrl, finalDownloadUrls)
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
            val url = obj.optString("url", "").ifBlank { obj.optString("link", "") }.replace("http://", "https://")
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
                .replace("&nbsp;", " ")
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
