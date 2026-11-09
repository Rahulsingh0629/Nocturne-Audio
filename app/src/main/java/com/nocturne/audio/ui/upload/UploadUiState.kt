package com.nocturne.audio.ui.upload

import android.net.Uri
import java.util.Calendar

val currentYear = Calendar.getInstance().get(Calendar.YEAR)

data class UploadUiState(
    val selectedFileUri: Uri? = null,
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val year: Int = currentYear,
    val language: String = "English",
    val isOwnershipChecked: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null,
    val isUploadSuccess: Boolean = false
)
