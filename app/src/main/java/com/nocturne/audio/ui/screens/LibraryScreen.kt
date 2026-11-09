package com.nocturne.audio.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocturne.audio.model.Song
import com.nocturne.audio.ui.components.AmbientBackground
import com.nocturne.audio.ui.components.FilterChipRow
import com.nocturne.audio.ui.components.SongCard
import com.nocturne.audio.ui.library.DeviceSongsViewModel
import com.nocturne.audio.ui.library.LibraryViewModel
import com.nocturne.audio.ui.theme.AccentGreen
import com.nocturne.audio.ui.theme.AccentPurple
import com.nocturne.audio.ui.theme.BackgroundDark
import com.nocturne.audio.ui.theme.BorderSubtle
import com.nocturne.audio.ui.theme.SurfaceBrightDark
import com.nocturne.audio.ui.theme.SurfaceDark
import com.nocturne.audio.ui.theme.TextPrimary
import com.nocturne.audio.ui.theme.TextSecondary
import com.nocturne.audio.ui.theme.TextTertiary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onSongSelected: (Song) -> Unit,
    onNavigateToUpload: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
    deviceViewModel: DeviceSongsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deviceState by deviceViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val subTabs = listOf("Favorites", "On this Phone", "My Uploads", "Playlists")

    // Permission check for Device Library (MediaStore)
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        deviceViewModel.updatePermissionState(isGranted, context)
    }

    LaunchedEffect(uiState.selectedSubTab) {
        if (uiState.selectedSubTab == "On this Phone") {
            val hasPerm = ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
            deviceViewModel.updatePermissionState(hasPerm, context)
        }
    }

    var newPlaylistNameInput by remember { mutableStateOf("") }
    var renameInput by remember { mutableStateOf("") }

    AmbientBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = TextPrimary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // Sub-tabs Row
            FilterChipRow(
                items = subTabs,
                selectedItem = uiState.selectedSubTab,
                onItemSelected = { viewModel.onSubTabSelected(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState.selectedSubTab) {
                    "Favorites" -> {
                        if (uiState.favoriteSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No favorites yet",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap the heart icon on any song to save it to your private library.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.favoriteSongs) { song ->
                                    SongCard(
                                        song = song,
                                        onSongClick = { onSongSelected(it) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(it) }
                                    )
                                }
                            }
                        }
                    }

                    "On this Phone" -> {
                        if (!deviceState.hasPermission) {
                            // Permission Card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                                        .padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Audio Permission Required",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Grant audio access to stream local files stored on your phone via ExoPlayer.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(requiredPermission) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = BackgroundDark)
                                    ) {
                                        Text("Grant Permission")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { openAppSettings(context) }) {
                                        Text("Open App Settings", color = TextSecondary)
                                    }
                                }
                            }
                        } else {
                            // Device Library View with Search, Format Filters, & Sticky Headers
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = deviceState.searchQuery,
                                    onValueChange = { deviceViewModel.onSearchQueryChanged(it) },
                                    placeholder = { Text("Search local tracks...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentPurple,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                )

                                // Format Filters
                                val formats = listOf("All", "MP3", "FLAC", "WAV", "AAC")
                                FilterChipRow(
                                    items = formats,
                                    selectedItem = deviceState.selectedFormat,
                                    onItemSelected = { deviceViewModel.onFormatSelected(it) },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                )

                                if (deviceState.isLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = AccentPurple)
                                    }
                                } else if (deviceState.filteredGroupedSongs.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No audio files found on your device", color = TextSecondary)
                                    }
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        deviceState.filteredGroupedSongs.forEach { (yearGroup, songsInGroup) ->
                                            stickyHeader {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(BackgroundDark)
                                                        .padding(vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "Year: $yearGroup",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = AccentPurple
                                                    )
                                                }
                                            }

                                            items(songsInGroup) { song ->
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

                    "My Uploads" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Private Uploads",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Upload your personal audio files to Firebase Storage. Private only to you.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onNavigateToUpload,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = BackgroundDark),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Song", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    "Playlists" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Playlists",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Button(
                                    onClick = {
                                        newPlaylistNameInput = ""
                                        viewModel.toggleCreatePlaylistDialog(true)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = BackgroundDark),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Playlist", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            if (uiState.playlists.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No playlists created yet", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(uiState.playlists) { playlist ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SurfaceBrightDark),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = AccentPurple)
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = playlist.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${playlist.songIds.size} songs",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }

                                            IconButton(onClick = {
                                                renameInput = playlist.name
                                                viewModel.setPlaylistToRename(playlist)
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary)
                                            }

                                            IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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

        // Create Playlist Dialog
        if (uiState.showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleCreatePlaylistDialog(false) },
                title = { Text("Create New Playlist", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistNameInput,
                        onValueChange = { newPlaylistNameInput = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.createPlaylist(newPlaylistNameInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Create", color = BackgroundDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleCreatePlaylistDialog(false) }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // Rename Playlist Dialog
        uiState.playlistToRename?.let { playlist ->
            AlertDialog(
                onDismissRequest = { viewModel.setPlaylistToRename(null) },
                title = { Text("Rename Playlist", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.renamePlaylist(playlist.id, renameInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save", color = BackgroundDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setPlaylistToRename(null) }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
