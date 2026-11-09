package com.nocturne.audio.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.audio.data.repository.FavoritesRepository
import com.nocturne.audio.data.repository.FavoritesRepositoryImpl
import com.nocturne.audio.data.repository.SearchHistoryRepository
import com.nocturne.audio.data.repository.SearchHistoryRepositoryImpl
import com.nocturne.audio.data.repository.SongsRepository
import com.nocturne.audio.data.repository.SongsRepositoryImpl
import com.nocturne.audio.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val songsRepository: SongsRepository = SongsRepositoryImpl(),
    private val searchHistoryRepository: SearchHistoryRepository = SearchHistoryRepositoryImpl(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeRecentSearches()
        observeFavorites()
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            searchHistoryRepository.getRecentSearches().collectLatest { recents ->
                _uiState.update { it.copy(recentSearches = recents) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.getFavoriteSongIds().collectLatest { favIds ->
                _uiState.update { state ->
                    val updatedResults = state.searchResults.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id))
                    }
                    state.copy(favoriteIds = favIds, searchResults = updatedResults)
                }
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        val sanitized = newQuery.take(100) // Sanitize & length limit max 100 chars
        _uiState.update { it.copy(query = sanitized) }
        triggerDebouncedSearch()
    }

    fun onGenreSelected(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
        triggerDebouncedSearch()
    }

    fun onYearSelected(year: Int?) {
        _uiState.update { it.copy(selectedYear = year) }
        triggerDebouncedSearch()
    }

    private fun triggerDebouncedSearch() {
        searchJob?.cancel()

        val state = _uiState.value
        val query = state.query.trim()
        val genre = if (state.selectedGenre == "All") null else state.selectedGenre

        if (query.isBlank() && genre == null && state.selectedYear == null) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // 350 ms debounce delay per strict specification
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (query.isNotBlank()) {
                searchHistoryRepository.addRecentSearch(query)
            }

            val result = songsRepository.searchSongs(
                query = query,
                year = state.selectedYear,
                genre = genre
            )

            result.fold(
                onSuccess = { songs ->
                    val favIds = state.favoriteIds
                    val mappedSongs = songs.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id))
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            searchResults = mappedSongs,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to perform search."
                        )
                    }
                }
            )
        }
    }

    fun removeRecentSearch(searchId: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeRecentSearch(searchId)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            searchHistoryRepository.clearAllRecentSearches()
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(song)
        }
    }
}
