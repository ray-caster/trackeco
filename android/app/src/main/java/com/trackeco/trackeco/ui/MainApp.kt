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

/**
 * This is the single, top-level composable for the entire application.
 * Its only responsibility is to act as a router, deciding whether to show
 * the authentication flow or the main, logged-in application screen.
 */
@Composable
fun MainApp() {
    val context = LocalContext.current
    // We remember the repository so it's not recreated on every recomposition
    val userPreferences = remember { UserPreferencesRepository(context) }
    
    // This flow reactively listens to the user's login state in DataStore
    val loggedInUserId by userPreferences.loggedInUserId.collectAsState(initial = null)
    
    TrackecoTheme {
        // Use a Crossfade for a smooth transition when the user logs in or out
        Crossfade(targetState = loggedInUserId != null, label = "AuthCrossfade") { isLoggedIn ->
            if (isLoggedIn) {
                // User IS logged in, show the main application screen.
                // We pass the non-null user ID to the MainScreen.
                MainScreen(userId = loggedInUserId!!)
            } else {
                // User is NOT logged in, show the authentication flow.
                AuthScreen()
            }
        }
    }
}