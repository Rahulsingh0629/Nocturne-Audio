package com.nocturne.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocturne.audio.model.Song
import com.nocturne.audio.ui.components.AmbientBackground
import com.nocturne.audio.ui.components.ShimmerSongCard
import com.nocturne.audio.ui.components.SongCard
import com.nocturne.audio.ui.home.HomeViewModel
import com.nocturne.audio.ui.theme.AccentPurple
import com.nocturne.audio.ui.theme.BackgroundDark
import com.nocturne.audio.ui.theme.BorderSubtle
import com.nocturne.audio.ui.theme.SurfaceBrightDark
import com.nocturne.audio.ui.theme.SurfaceDark
import com.nocturne.audio.ui.theme.TextPrimary
import com.nocturne.audio.ui.theme.TextSecondary
import com.nocturne.audio.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    onSongSelected: (Song) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Detect scrolling near bottom to trigger infinite scroll pagination
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoading && !uiState.isLoadingMore && uiState.canLoadMore) {
            viewModel.loadMoreSongs()
        }
    }

    AmbientBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header: Big Profile Avatar Icon + Hello Greeting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Big Profile Icon on Top Left
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AccentPurple.copy(alpha = 0.2f))
                        .border(1.5.dp, AccentPurple, CircleShape)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = AccentPurple,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val greetingName = if (uiState.userName.isNotBlank()) uiState.userName else "Listener"
                    Text(
                        text = "Hello, $greetingName",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Browse songs by Year & Language",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Language Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(uiState.languages) { lang ->
                    val isSelected = lang == uiState.selectedLanguage
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AccentPurple else SurfaceDark)
                            .border(1.dp, if (isSelected) AccentPurple else BorderSubtle, RoundedCornerShape(20.dp))
                            .clickable { viewModel.onLanguageSelected(lang) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = lang.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) BackgroundDark else TextSecondary
                        )
                    }
                }
            }

            // Year Selector Row: Shows initial 8 years + "+ More" chip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                val displayYears = if (uiState.selectedYear !in uiState.initialYears) {
                    listOf(uiState.selectedYear) + uiState.initialYears
                } else {
                    uiState.initialYears
                }

                items(displayYears) { yr ->
                    val isSelected = yr == uiState.selectedYear
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentPurple.copy(alpha = 0.2f) else SurfaceDark)
                            .border(1.dp, if (isSelected) AccentPurple else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { viewModel.onYearSelected(yr) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = yr.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) AccentPurple else TextSecondary
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceBrightDark)
                            .border(1.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleYearPickerDialog(true) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ More Years",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentPurple
                        )
                    }
                }
            }

            // Section Header & Play All / Shuffle Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Top songs of ${uiState.selectedYear}",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.songs.size} tracks loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }

                if (uiState.songs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                uiState.songs.firstOrNull()?.let { onSongSelected(it) }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPurple,
                                contentColor = BackgroundDark
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play All", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                uiState.songs.shuffled().firstOrNull()?.let { onSongSelected(it) }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area: Loading, Error, Empty, or List with Infinite Scroll
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(6) {
                                ShimmerSongCard()
                            }
                        }
                    }

                    uiState.errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = uiState.errorMessage ?: "An error occurred",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.fetchSongs() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = BackgroundDark)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    uiState.songs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No songs found for ${uiState.selectedYear}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Language filter: ${uiState.selectedLanguage.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "Note: JioSaavn features top Bollywood, Regional, and International releases. Select a year or language filter to stream.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextTertiary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.resetFilters() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = BackgroundDark)
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = uiState.songs,
                                key = { it.id }
                            ) { song ->
                                SongCard(
                                    song = song,
                                    onSongClick = { onSongSelected(it) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) }
                                )
                            }

                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = AccentPurple,
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // All 100 Years Picker Dialog
        if (uiState.showYearPickerDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleYearPickerDialog(false) },
                title = {
                    Text(
                        text = "Select Year (Past 100 Years)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.allYears) { yr ->
                                val isSelected = yr == uiState.selectedYear
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AccentPurple.copy(alpha = 0.2f) else SurfaceDark)
                                        .border(1.dp, if (isSelected) AccentPurple else BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.onYearSelected(yr) }
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = yr.toString(),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) AccentPurple else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleYearPickerDialog(false) }) {
                        Text("Close", color = AccentPurple)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
