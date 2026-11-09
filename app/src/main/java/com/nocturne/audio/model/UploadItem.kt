package com.nocturne.audio.model

data class UploadItem(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val year: Int = 0,
    val language: String = "",
    val storagePath: String = "",
    val sizeBytes: Long = 0,
    val uploadedAt: Long = System.currentTimeMillis()
)
