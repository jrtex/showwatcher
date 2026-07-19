package com.example.showwatcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.showwatcher.ui.nav.ShowWatcherNavHost
import com.example.showwatcher.ui.theme.ShowWatcherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShowWatcherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShowWatcherNavHost()
                }
            }
        }
    }
}
