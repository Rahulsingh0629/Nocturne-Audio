package com.nocturne.audio.model

enum class SongSource {
    JAMENDO,
    LOCAL,
    UPLOAD,
    SAAVN
}

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val albumArtUrl: String = "",
    val audioUrl: String = "",
    val durationSeconds: Int = 0,
    val year: Int = 0,
    val language: String = "",
    val source: SongSource = SongSource.SAAVN,
    val isFavorite: Boolean = false
)
