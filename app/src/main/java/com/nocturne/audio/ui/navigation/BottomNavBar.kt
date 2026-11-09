package com.nocturne.audio.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.audio.ui.theme.AccentPurple
import com.nocturne.audio.ui.theme.BorderSubtle
import com.nocturne.audio.ui.theme.SurfaceDark
import com.nocturne.audio.ui.theme.TextPrimary
import com.nocturne.audio.ui.theme.TextSecondary
import com.nocturne.audio.ui.theme.TextTertiary

sealed class NavTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : NavTab("discover", "Discover", Icons.Default.Explore)
    object Search : NavTab("search", "Search", Icons.Default.Search)
    object TimeMachine : NavTab("time_machine", "Rewind", Icons.Default.Schedule)
    object Library : NavTab("library", "Library", Icons.Default.LibraryMusic)
    object Profile : NavTab("profile", "Profile", Icons.Default.Person)
}

val mainTabs = listOf(
    NavTab.Home,
    NavTab.Search,
    NavTab.TimeMachine,
    NavTab.Library,
    NavTab.Profile
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        mainTabs.forEach { tab ->
            val isSelected = currentRoute == tab.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentPurple,
                    unselectedIconColor = TextTertiary,
                    selectedTextColor = AccentPurple,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SurfaceDark
                )
            )
        }
    }
}
