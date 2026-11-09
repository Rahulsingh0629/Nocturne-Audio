package com.nocturne.audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nocturne.audio.data.repository.PlayerRepositoryImpl
import com.nocturne.audio.ui.navigation.AppNavigation
import com.nocturne.audio.ui.theme.NocturneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlayerRepositoryImpl.getInstance().initialize(this)
        enableEdgeToEdge()
        setContent {
            NocturneTheme {
                AppNavigation()
            }
        }
    }
}
