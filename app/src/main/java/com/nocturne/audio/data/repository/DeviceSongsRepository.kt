package com.nocturne.audio.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.nocturne.audio.model.Song
import com.nocturne.audio.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface DeviceSongsRepository {
    suspend fun getDeviceSongs(context: Context): Result<List<Song>>
}

class DeviceSongsRepositoryImpl : DeviceSongsRepository {

    override suspend fun getDeviceSongs(context: Context): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                val songsList = mutableListOf<Song>()
                val contentResolver = context.contentResolver

                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.MIME_TYPE
                )

                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.YEAR} DESC, ${MediaStore.Audio.Media.TITLE} ASC"

                val cursor = contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val yearColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                    while (c.moveToNext()) {
                        val id = c.getLong(idColumn)
                        val title = c.getString(titleColumn) ?: "Unknown Track"
                        val artist = c.getString(artistColumn) ?: "Unknown Artist"
                        val durationMs = c.getLong(durationColumn)
                        val year = c.getInt(yearColumn)

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        // If year is invalid or 0, keep 0 so UI displays "Unknown year"
                        val safeYear = if (year in 1900..2100) year else 0

                        songsList.add(
                            Song(
                                id = "local_$id",
                                title = title,
                                artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                                albumArtUrl = "",
                                audioUrl = contentUri.toString(),
                                durationSeconds = (durationMs / 1000).toInt(),
                                year = safeYear,
                                language = "Local",
                                source = SongSource.LOCAL,
                                isFavorite = false
                            )
                        )
                    }
                }

                Result.success(songsList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
