package com.nocturne.audio.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nocturne.audio.model.Song
import com.nocturne.audio.ui.components.MiniPlayer
import com.nocturne.audio.ui.screens.HomeScreen
import com.nocturne.audio.ui.screens.LibraryScreen
import com.nocturne.audio.ui.screens.NowPlayingScreen
import com.nocturne.audio.ui.screens.ProfileScreen
import com.nocturne.audio.ui.screens.SearchScreen
import com.nocturne.audio.ui.screens.TimeMachineScreen
import com.nocturne.audio.ui.screens.UploadSongScreen

@Composable
fun MainContainer(
    rootNavController: NavHostController,
    onSignOut: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavTab.Home.route

    // State for player and now playing modal
    var activeSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableStateOf(0.35f) }
    var showNowPlaying by remember { mutableStateOf(false) }

    val hideBottomBarRoutes = listOf("upload_song")
    val shouldShowBottomBar = currentRoute !in hideBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                Column {
                    AnimatedVisibility(
                        visible = activeSong != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            currentSong = activeSong,
                            isPlaying = isPlaying,
                            progressFraction = progressFraction,
                            onPlayPauseClick = { isPlaying = !isPlaying },
                            onNextClick = { /* Handle next track */ },
                            onMiniPlayerClick = { showNowPlaying = true }
                        )
                    }

                    BottomNavBar(
                        currentRoute = currentRoute,
                        onTabSelected = { tab ->
                            bottomNavController.navigate(tab.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = NavTab.Home.route
            ) {
                composable(NavTab.Home.route) {
                    HomeScreen(
                        onSongSelected = { song ->
                            activeSong = song
                            isPlaying = true
                        },
                        onProfileClick = {
                            bottomNavController.navigate(NavTab.Profile.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(NavTab.Search.route) {
                    SearchScreen(
                        onSongSelected = { song ->
                            activeSong = song
                            isPlaying = true
                        }
                    )
                }

                composable(NavTab.TimeMachine.route) {
                    TimeMachineScreen(
                        onSongSelected = { song ->
                            activeSong = song
                            isPlaying = true
                        }
                    )
                }

                composable(NavTab.Library.route) {
                    LibraryScreen(
                        onSongSelected = { song ->
                            activeSong = song
                            isPlaying = true
                        },
                        onNavigateToUpload = {
                            bottomNavController.navigate("upload_song")
                        }
                    )
                }

                composable(NavTab.Profile.route) {
                    ProfileScreen(
                        onSignOut = onSignOut
                    )
                }

                composable("upload_song") {
                    UploadSongScreen(
                        onUploadSuccess = {
                            bottomNavController.popBackStack()
                        },
                        onBack = {
                            bottomNavController.popBackStack()
                        }
                    )
                }
            }

            // Full screen Now Playing overlay above tabs
            if (showNowPlaying) {
                NowPlayingScreen(
                    song = activeSong,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { isPlaying = !isPlaying },
                    onDismiss = { showNowPlaying = false }
                )
            }
        }
    }
}
