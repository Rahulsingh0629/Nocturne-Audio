package com.nocturne.audio.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocturne.audio.model.Song
import com.nocturne.audio.ui.components.AmbientBackground
import com.nocturne.audio.ui.components.ShimmerSongCard
import com.nocturne.audio.ui.components.SongCard
import com.nocturne.audio.ui.theme.AccentPurple
import com.nocturne.audio.ui.theme.BackgroundDark
import com.nocturne.audio.ui.theme.BorderSubtle
import com.nocturne.audio.ui.theme.SurfaceDark
import com.nocturne.audio.ui.theme.TextPrimary
import com.nocturne.audio.ui.theme.TextSecondary
import com.nocturne.audio.ui.theme.TextTertiary
import com.nocturne.audio.ui.timemachine.TimeMachineViewModel
import com.nocturne.audio.ui.timemachine.currentYear

@Composable
fun TimeMachineScreen(
    onSongSelected: (Song) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimeMachineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialListState = rememberLazyListState()

    val animatedPrimaryColor by animateColorAsState(
        targetValue = uiState.currentEra.primaryColor,
        animationSpec = tween(700),
        label = "eraPrimary"
    )
    val animatedSecondaryColor by animateColorAsState(
        targetValue = uiState.currentEra.secondaryColor,
        animationSpec = tween(700),
        label = "eraSecondary"
    )

    val yearsList = (currentYear downTo 1950).toList()

    LaunchedEffect(uiState.selectedYear) {
        val index = yearsList.indexOf(uiState.selectedYear)
        if (index >= 0) {
            dialListState.animateScrollToItem(index)
        }
    }

    AmbientBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Title
            Text(
                text = "Time Machine",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = TextPrimary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // Animated Era Banner Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                animatedPrimaryColor.copy(alpha = 0.25f),
                                animatedSecondaryColor.copy(alpha = 0.15f),
                                SurfaceDark
                            )
                        )
                    )
                    .border(1.dp, animatedPrimaryColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = uiState.currentEra.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = animatedPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.currentEra.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Year Dial Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.stepYear(-1) }) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Year",
                        tint = AccentPurple
                    )
                }

                LazyRow(
                    state = dialListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(yearsList) { yr ->
                        val isSelected = yr == uiState.selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) animatedPrimaryColor else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isSelected) animatedPrimaryColor else BorderSubtle,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.onYearSelected(yr) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = yr.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) BackgroundDark else TextSecondary
                            )
                        }
                    }
                }

                IconButton(onClick = { viewModel.stepYear(1) }) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Year",
                        tint = AccentPurple
                    )
                }
            }

            // Controls & Header for Top 10
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Top 10 on JioSaavn (${uiState.selectedYear})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Most popular releases for this year",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }

                if (uiState.top10Songs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                uiState.top10Songs.firstOrNull()?.let { onSongSelected(it) }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = animatedPrimaryColor,
                                contentColor = BackgroundDark
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                uiState.top10Songs.shuffled().firstOrNull()?.let { onSongSelected(it) }
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

            // Main Content: Top 10 Ranked List
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
                                    onClick = { viewModel.onYearSelected(uiState.selectedYear) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = BackgroundDark)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    uiState.top10Songs.isEmpty() -> {
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
                                    text = "No tracks found for ${uiState.selectedYear}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try selecting a different year on the dial.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.top10Songs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank Number Badge (1..10)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (index < 3) animatedPrimaryColor.copy(alpha = 0.25f) else SurfaceDark
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (index < 3) animatedPrimaryColor else TextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(modifier = Modifier.weight(1f)) {
                                        SongCard(
                                            song = song,
                                            onSongClick = { onSongSelected(it) },
                                            onFavoriteToggle = { viewModel.toggleFavorite(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
