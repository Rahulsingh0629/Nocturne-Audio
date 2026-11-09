package com.nocturne.audio.ui.library

import com.nocturne.audio.model.Playlist
import com.nocturne.audio.model.Song

data class LibraryUiState(
    val selectedSubTab: String = "Favorites",
    val favoriteSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoadingFavorites: Boolean = false,
    val isLoadingPlaylists: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val playlistToRename: Playlist? = null,
    val errorMessage: String? = null
)
