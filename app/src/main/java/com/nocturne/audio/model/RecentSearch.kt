package com.nocturne.audio.model

data class RecentSearch(
    val id: String = "",
    val query: String = "",
    val at: Long = System.currentTimeMillis()
)
