package com.nocturne.audio.ui.home

import com.nocturne.audio.model.Song
import java.util.Calendar

data class LanguageOption(
    val displayName: String,
    val code: String
)

val defaultLanguages = listOf(
    LanguageOption("All", "all"),
    LanguageOption("English", "en"),
    LanguageOption("Hindi", "hi"),
    LanguageOption("Punjabi", "pa"),
    LanguageOption("Spanish", "es"),
    LanguageOption("Tamil", "ta"),
    LanguageOption("Korean", "ko"),
    LanguageOption("Japanese", "ja")
)

val currentCalendarYear = Calendar.getInstance().get(Calendar.YEAR)
val initialEightYears = (currentCalendarYear downTo (currentCalendarYear - 7)).toList()
val past100Years = (currentCalendarYear downTo (currentCalendarYear - 99)).toList()

data class HomeUiState(
    val userName: String = "",
    val selectedLanguage: LanguageOption = defaultLanguages.first(),
    val selectedYear: Int = currentCalendarYear,
    val songs: List<Song> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val currentOffset: Int = 0,
    val errorMessage: String? = null,
    val languages: List<LanguageOption> = defaultLanguages,
    val initialYears: List<Int> = initialEightYears,
    val allYears: List<Int> = past100Years,
    val showYearPickerDialog: Boolean = false
)
