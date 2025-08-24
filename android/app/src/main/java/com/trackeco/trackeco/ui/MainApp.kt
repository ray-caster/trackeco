package com.trackeco.trackeco.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.trackeco.trackeco.data.UserPreferencesRepository
import com.trackeco.trackeco.ui.auth.AuthScreen
import com.trackeco.trackeco.ui.theme.TrackecoTheme

@Composable
fun MainApp() {
    val context = LocalContext.current
    // remember the repository so it doesn't get created on every recomposition
    val userPreferences = remember { UserPreferencesRepository(context) }
    
    // Observe the logged-in user ID from DataStore. This is the core of the logic.
    val loggedInUserId by userPreferences.loggedInUserId.collectAsState(initial = null)
    
    TrackecoTheme {
        // Use Crossfade for a smooth transition when the login state changes.
        Crossfade(targetState = loggedInUserId != null, label = "AuthCrossfade") { isLoggedIn ->
            if (isLoggedIn) {
                // User is logged in, show the main application screen.
                // We pass the non-null user ID to the MainScreen.
                MainScreen(userId = loggedInUserId!!)
            } else {
                // User is not logged in, show the authentication flow.
                AuthScreen()
            }
        }
    }
}