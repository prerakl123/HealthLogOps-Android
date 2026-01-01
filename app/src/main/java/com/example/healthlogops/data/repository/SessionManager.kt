package com.example.healthlogops.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {
    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    val userId: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[USER_ID_KEY]
        }

    suspend fun saveSession(userId: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
        }
    }
}
