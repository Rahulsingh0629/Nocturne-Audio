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
import androidx.compose.runtime.collectAsState
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
import com.nocturne.audio.data.repository.PlayerRepository
import com.nocturne.audio.data.repository.PlayerRepositoryImpl
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
    onSignOut: () -> Unit,
    playerRepository: PlayerRepository = PlayerRepositoryImpl.getInstance()
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavTab.Home.route

    // Direct binding to singleton PlayerRepository
    val activeSong by playerRepository.currentSong.collectAsState()
    val isPlaying by playerRepository.isPlaying.collectAsState()
    val progressFraction by playerRepository.progressFraction.collectAsState()

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
                            onPlayPauseClick = { playerRepository.togglePlayPause() },
                            onNextClick = { playerRepository.skipToNext() },
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
                            playerRepository.playSong(song)
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
                            playerRepository.playSong(song)
                        }
                    )
                }

                composable(NavTab.TimeMachine.route) {
                    TimeMachineScreen(
                        onSongSelected = { song ->
                            playerRepository.playSong(song)
                        }
                    )
                }

                composable(NavTab.Library.route) {
                    LibraryScreen(
                        onSongSelected = { song ->
                            playerRepository.playSong(song)
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
                    onPlayPauseToggle = { playerRepository.togglePlayPause() },
                    onDismiss = { showNowPlaying = false }
                )
            }
        }
    }
}
