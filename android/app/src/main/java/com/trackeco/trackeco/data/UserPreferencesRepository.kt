package com.trackeco.trackeco.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a singleton instance of DataStore for the whole app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
    }

    // A flow that emits the current user ID, or null if not logged in.
    // The UI will reactively observe this flow.
    val loggedInUserId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[Keys.USER_ID]
        }

    // Saves the user ID to DataStore after a successful login
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_ID] = userId
        }
    }

    // Clears the user ID on logout
    suspend fun clearUserId() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.USER_ID)
        }
    }
}