package com.nocturne.audio.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.audio.data.repository.DeviceSongsRepository
import com.nocturne.audio.data.repository.DeviceSongsRepositoryImpl
import com.nocturne.audio.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceSongsUiState(
    val hasPermission: Boolean = false,
    val searchQuery: String = "",
    val selectedFormat: String = "All",
    val allSongs: List<Song> = emptyList(),
    val filteredGroupedSongs: Map<String, List<Song>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DeviceSongsViewModel(
    private val repository: DeviceSongsRepository = DeviceSongsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceSongsUiState())
    val uiState: StateFlow<DeviceSongsUiState> = _uiState.asStateFlow()

    fun updatePermissionState(hasPermission: Boolean, context: Context? = null) {
        _uiState.update { it.copy(hasPermission = hasPermission) }
        if (hasPermission && context != null) {
            loadDeviceSongs(context)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onFormatSelected(format: String) {
        _uiState.update { it.copy(selectedFormat = format) }
        applyFilters()
    }

    fun loadDeviceSongs(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getDeviceSongs(context)

            result.fold(
                onSuccess = { songs ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allSongs = songs
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to query device songs."
                        )
                    }
                }
            )
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val format = state.selectedFormat.lowercase()

        var filtered = state.allSongs

        if (query.isNotBlank()) {
            filtered = filtered.filter { song ->
                song.title.lowercase().contains(query) || song.artist.lowercase().contains(query)
            }
        }

        if (format != "all") {
            filtered = filtered.filter { song ->
                song.audioUrl.lowercase().contains(format)
            }
        }

        // Group songs by year with sticky headers. Unknown year (0) -> "Unknown year"
        val grouped = filtered.groupBy { song ->
            if (song.year in 1900..2100) song.year.toString() else "Unknown year"
        }

        _uiState.update { it.copy(filteredGroupedSongs = grouped) }
    }
}
