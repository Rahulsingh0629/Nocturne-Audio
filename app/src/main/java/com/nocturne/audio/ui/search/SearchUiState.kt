package com.nocturne.audio.ui.search

import com.nocturne.audio.model.RecentSearch
import com.nocturne.audio.model.Song

val trendingGenresList = listOf("All", "Pop", "Rock", "Electronic", "Ambient", "Jazz", "HipHop", "Classical", "Acoustic")

data class SearchUiState(
    val query: String = "",
    val selectedGenre: String = "All",
    val selectedYear: Int? = null,
    val selectedLanguage: String = "all",
    val searchResults: List<Song> = emptyList(),
    val recentSearches: List<RecentSearch> = emptyList(),
    val trendingGenres: List<String> = trendingGenresList,
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
