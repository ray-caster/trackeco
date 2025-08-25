package com.trackeco.trackeco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.trackeco.trackeco.ui.theme.TrackEcoTheme
import com.trackeco.trackeco.ui.MainApp
import com.trackeco.trackeco.ui.auth.AuthScreen
import com.trackeco.trackeco.viewmodel.AuthViewModel
import com.trackeco.trackeco.viewmodel.AuthState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            TrackEcoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrackEcoApp()
                }
            }
        }
    }
}

@Composable
fun TrackEcoApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    when (authState) {
        is AuthState.Loading -> {
            SplashLoadingScreen()
        }
        is AuthState.Authenticated -> {
            MainApp()
        }
        is AuthState.NotAuthenticated -> {
            AuthScreen(authViewModel)
        }
    }
}

@Composable
fun SplashLoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        // Splash content implemented in individual screens
    }
}