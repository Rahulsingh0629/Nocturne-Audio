package com.nocturne.audio.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.audio.data.repository.FavoritesRepository
import com.nocturne.audio.data.repository.FavoritesRepositoryImpl
import com.nocturne.audio.data.repository.PlaylistRepository
import com.nocturne.audio.data.repository.PlaylistRepositoryImpl
import com.nocturne.audio.model.Playlist
import com.nocturne.audio.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(),
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        observePlaylists()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.getFavoriteSongs().collectLatest { songs ->
                val ids = songs.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteSongs = songs, favoriteIds = ids, isLoadingFavorites = false) }
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistRepository.getPlaylists().collectLatest { playlists ->
                _uiState.update { it.copy(playlists = playlists, isLoadingPlaylists = false) }
            }
        }
    }

    fun onSubTabSelected(tab: String) {
        _uiState.update { it.copy(selectedSubTab = tab) }
    }

    fun toggleCreatePlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showCreatePlaylistDialog = show) }
    }

    fun setPlaylistToRename(playlist: Playlist?) {
        _uiState.update { it.copy(playlistToRename = playlist) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val result = playlistRepository.createPlaylist(name)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(showCreatePlaylistDialog = false, errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            val result = playlistRepository.renamePlaylist(playlistId, newName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(playlistToRename = null, errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(song)
        }
    }
}
