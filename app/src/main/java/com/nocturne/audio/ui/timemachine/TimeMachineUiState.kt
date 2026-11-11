package com.nocturne.audio.ui.timemachine

import androidx.compose.ui.graphics.Color
import com.nocturne.audio.model.Song
import java.util.Calendar

data class EraInfo(
    val title: String,
    val yearRange: IntRange,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

val currentYear = Calendar.getInstance().get(Calendar.YEAR)

val eraList = listOf(
    EraInfo(
        title = "2020s • The Modern Streaming Era",
        yearRange = 2020..2029,
        description = "Top hit tracks from the decade of streaming algorithms and viral soundscapes on JioSaavn.",
        primaryColor = Color(0xFFB39DFF),
        secondaryColor = Color(0xFFEC4899)
    ),
    EraInfo(
        title = "2010s • The Digital & Electronic Wave",
        yearRange = 2010..2019,
        description = "The Golden Age of digital music creation and popular regional releases.",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF3B82F6)
    ),
    EraInfo(
        title = "2000s • The Indie & Y2K Boom",
        yearRange = 2000..2009,
        description = "Acoustic jams, classic soundtracks, and millennium anthems.",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFEF4444)
    ),
    EraInfo(
        title = "1990s • The Alt & Retro Underground",
        yearRange = 1990..1999,
        description = "Lo-fi synth tracks, golden 90s melodies, and nostalgic classic vibes.",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669)
    ),
    EraInfo(
        title = "1980s • The Synthwave & Retro Decade",
        yearRange = 1980..1989,
        description = "Retro 80s drum machines, analog synthesizers, and disco grooves.",
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFFD946EF)
    ),
    EraInfo(
        title = "Classic Era • Vintage Soundscapes",
        yearRange = 1900..1979,
        description = "Vintage acoustic tracks, legendary melodies, and classic retro instrumentals.",
        primaryColor = Color(0xFFD97706),
        secondaryColor = Color(0xFFB45309)
    )
)

data class TimeMachineUiState(
    val selectedYear: Int = currentYear,
    val currentEra: EraInfo = eraList.first(),
    val top10Songs: List<Song> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
