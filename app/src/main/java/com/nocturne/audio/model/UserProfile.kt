package com.nocturne.audio.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val preferredLanguages: List<String> = emptyList(),
    val preferredDecades: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
