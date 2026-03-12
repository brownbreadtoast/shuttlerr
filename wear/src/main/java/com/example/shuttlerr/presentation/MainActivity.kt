package com.example.shuttlerr.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.shuttlerr.presentation.navigation.WearNavGraph
import com.example.shuttlerr.presentation.theme.ShuttlerrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            ShuttlerrTheme {
                WearNavGraph()
            }
        }
    }
}
