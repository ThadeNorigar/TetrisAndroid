package com.tetris.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tetris_preferences")

/**
 * Manages game preferences using DataStore
 */
class GamePreferences(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val HIGH_SCORE_KEY = intPreferencesKey("high_score")
        private val SOUND_ENABLED_KEY = stringPreferencesKey("sound_enabled")
    }

    /**
     * Get the selected theme name
     */
    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "Minimalistic"
    }

    /**
     * Save the selected theme
     */
    suspend fun saveTheme(themeName: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeName
        }
    }

    /**
     * Get the high score
     */
    val highScore: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HIGH_SCORE_KEY] ?: 0
    }

    /**
     * Save the high score if it's higher than the current one
     */
    suspend fun saveHighScore(score: Int) {
        context.dataStore.edit { preferences ->
            val currentHighScore = preferences[HIGH_SCORE_KEY] ?: 0
            if (score > currentHighScore) {
                preferences[HIGH_SCORE_KEY] = score
            }
        }
    }
}
