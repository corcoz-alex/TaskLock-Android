package com.corcozalex.tasklock.network

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The vault file creator stays outside
val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val dataStore: DataStore<Preferences>) {

    companion object {
        val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[JWT_TOKEN_KEY] = token
            Log.d("VaultTest", "ACTION: Saved Token -> ${token.take(15)}...")
        }
    }

    val getToken: Flow<String?> = dataStore.data.map { preferences ->
        val token = preferences[JWT_TOKEN_KEY]
        Log.d("VaultTest", "ACTION: Read Token -> ${token?.take(15) ?: "VAULT IS EMPTY (NULL)"}")
        token
    }

    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN_KEY)
            Log.d("VaultTest", "ACTION: Cleared Token")
        }
    }
}