package com.nocturne.audio.ui.timemachine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class TimeMachineViewModel(
    private val songsRepository: SongsRepository = SongsRepositoryImpl(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeMachineUiState())
    val uiState: StateFlow<TimeMachineUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        observeFavorites()
        fetchTop10ForYear(_uiState.value.selectedYear)
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.getFavoriteSongIds().collectLatest { favIds ->
                _uiState.update { state ->
                    val updatedSongs = state.top10Songs.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id))
                    }
                    state.copy(favoriteIds = favIds, top10Songs = updatedSongs)
                }
            }
        }
    }

    fun onYearSelected(year: Int) {
        if (_uiState.value.selectedYear == year) return

        val era = eraList.find { year in it.yearRange } ?: eraList.last()
        _uiState.update {
            it.copy(
                selectedYear = year,
                currentEra = era
            )
        }
        fetchTop10ForYear(year)
    }

    fun stepYear(delta: Int) {
        val newYear = (_uiState.value.selectedYear + delta).coerceIn(1920, currentYear)
        onYearSelected(newYear)
    }

    private fun fetchTop10ForYear(year: Int) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = songsRepository.getTop10ForYear(year)

            result.fold(
                onSuccess = { songs ->
                    val favIds = _uiState.value.favoriteIds
                    val mappedSongs = songs.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id))
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            top10Songs = mappedSongs,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load Top 10 for $year."
                        )
                    }
                }
            )
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(song)
        }
    }
}
