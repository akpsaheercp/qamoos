package com.example.qamoos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    private object PreferencesKeys {
        val FONT_SIZE_MULTIPLIER = floatPreferencesKey("font_size_multiplier")
        val DARK_THEME_CONFIG = stringPreferencesKey("dark_theme_config")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
    }

    val fontSizeMultiplier: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FONT_SIZE_MULTIPLIER] ?: 1.0f
        }

    val darkThemeConfig: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DARK_THEME_CONFIG] ?: "system"
        }

    val appLanguage: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE]
        }

    val isFirstRun: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_FIRST_RUN] ?: true
        }

    suspend fun updateFontSizeMultiplier(multiplier: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE_MULTIPLIER] = multiplier
        }
    }

    suspend fun updateDarkThemeConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME_CONFIG] = config
        }
    }

    suspend fun updateAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
            preferences[PreferencesKeys.IS_FIRST_RUN] = false
        }
    }

    suspend fun setFirstRunCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_RUN] = false
        }
    }
}
