package com.nocturne.audio.ui.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.audio.data.repository.UploadRepository
import com.nocturne.audio.data.repository.UploadRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadViewModel(
    private val repository: UploadRepository = UploadRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun onFileSelected(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        var name = "track.mp3"
        var size = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        val autoTitle = name.substringBeforeLast(".")

        _uiState.update {
            it.copy(
                selectedFileUri = uri,
                fileName = name,
                fileSizeBytes = size,
                title = autoTitle,
                errorMessage = null
            )
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onArtistChanged(artist: String) {
        _uiState.update { it.copy(artist = artist) }
    }

    fun onYearChanged(year: Int) {
        _uiState.update { it.copy(year = year) }
    }

    fun onLanguageChanged(language: String) {
        _uiState.update { it.copy(language = language) }
    }

    fun toggleOwnershipCheck(checked: Boolean) {
        _uiState.update { it.copy(isOwnershipChecked = checked) }
    }

    fun upload(context: Context) {
        val state = _uiState.value
        val uri = state.selectedFileUri ?: return

        if (!state.isOwnershipChecked) {
            _uiState.update { it.copy(errorMessage = "You must confirm ownership to upload audio.") }
            return
        }

        if (state.title.trim().isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, errorMessage = null) }

            val result = repository.uploadAudioFile(
                context = context,
                fileUri = uri,
                title = state.title,
                artist = state.artist.ifBlank { "Unknown Artist" },
                year = state.year,
                language = state.language,
                onProgress = { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadProgress = 1.0f,
                            isUploadSuccess = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = error.message ?: "Upload failed. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun cancelUpload() {
        repository.cancelCurrentUpload()
        _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
    }

    fun reset() {
        _uiState.update { UploadUiState() }
    }
}
