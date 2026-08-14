package com.mcodeproject.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Instancja DataStore powiązana z kontekstem aplikacji
private val Context.dataStore by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val fontSize: String = "Średnia", // Mała, Średnia, Duża
    val autoCopy: Boolean = false,
    val hapticEnabled: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val FONT_SIZE = stringPreferencesKey("font_size")
        val AUTO_COPY = booleanPreferencesKey("auto_copy")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                fontSize = preferences[PreferencesKeys.FONT_SIZE] ?: "Średnia",
                autoCopy = preferences[PreferencesKeys.AUTO_COPY] ?: false,
                hapticEnabled = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true
            )
        }

    suspend fun setFontSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = size
        }
    }

    suspend fun setAutoCopy(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_COPY] = enabled
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_ENABLED] = enabled
        }
    }
}
