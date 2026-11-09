package com.nocturne.audio.model

data class Playlist(
    val id: String = "",
    val name: String = "",
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
