package com.nocturne.audio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nocturne.audio.data.repository.FavoritesRepository
import com.nocturne.audio.data.repository.FavoritesRepositoryImpl
import com.nocturne.audio.data.repository.SongsRepository
import com.nocturne.audio.data.repository.SongsRepositoryImpl
import com.nocturne.audio.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val songsRepository: SongsRepository = SongsRepositoryImpl(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchSongsJob: Job? = null

    init {
        loadUserProfile()
        observeFavorites()
        fetchSongs()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.getString("name") ?: ""
                    val firstName = name.split(" ").firstOrNull() ?: name
                    _uiState.update { it.copy(userName = firstName) }
                }
            }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.getFavoriteSongIds().collectLatest { favIds ->
                _uiState.update { state ->
                    val updatedSongs = state.songs.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id))
                    }
                    state.copy(favoriteIds = favIds, songs = updatedSongs)
                }
            }
        }
    }

    fun onLanguageSelected(languageOption: LanguageOption) {
        if (_uiState.value.selectedLanguage == languageOption) return
        _uiState.update { it.copy(selectedLanguage = languageOption, currentOffset = 0, canLoadMore = true) }
        fetchSongs()
    }

    fun onYearSelected(year: Int) {
        if (_uiState.value.selectedYear == year) return
        _uiState.update { it.copy(selectedYear = year, currentOffset = 0, canLoadMore = true, showYearPickerDialog = false) }
        fetchSongs()
    }

    fun toggleYearPickerDialog(show: Boolean) {
        _uiState.update { it.copy(showYearPickerDialog = show) }
    }

    fun fetchSongs() {
        fetchSongsJob?.cancel()

        fetchSongsJob = viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null, currentOffset = 0, canLoadMore = true) }

            val result = songsRepository.getSongsByYearAndLanguage(
                year = state.selectedYear,
                languageCode = state.selectedLanguage.code,
                offset = 0,
                limit = 20
            )

            result.fold(
                onSuccess = { songsList ->
                    val favIds = state.favoriteIds
                    val mappedSongs = songsList.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id))
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            songs = mappedSongs,
                            currentOffset = mappedSongs.size,
                            canLoadMore = mappedSongs.size >= 20,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load songs from JioSaavn."
                        )
                    }
                }
            )
        }
    }

    fun loadMoreSongs() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val result = songsRepository.getSongsByYearAndLanguage(
                year = state.selectedYear,
                languageCode = state.selectedLanguage.code,
                offset = state.currentOffset,
                limit = 20
            )

            result.fold(
                onSuccess = { newSongs ->
                    val favIds = state.favoriteIds
                    val mappedNewSongs = newSongs.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id))
                    }

                    // Avoid duplicate songs by ID
                    val existingIds = state.songs.map { it.id }.toSet()
                    val filteredNewSongs = mappedNewSongs.filter { !existingIds.contains(it.id) }
                    val updatedList = state.songs + filteredNewSongs

                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            songs = updatedList,
                            currentOffset = it.currentOffset + newSongs.size,
                            canLoadMore = newSongs.size >= 20
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(song)
        }
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedLanguage = defaultLanguages.first(),
                selectedYear = currentCalendarYear,
                currentOffset = 0,
                canLoadMore = true
            )
        }
        fetchSongs()
    }
}
