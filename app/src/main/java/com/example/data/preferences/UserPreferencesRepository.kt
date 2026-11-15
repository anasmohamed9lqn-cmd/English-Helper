package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "english_helper_settings"
)

data class AppPreferences(
    val languageCode: String = "en",
    val prioritizeUnmastered: Boolean = true,
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("ui_language_code")
        val PRIORITIZE_UNMASTERED = booleanPreferencesKey("prioritize_unmastered")
    }

    val preferencesFlow: Flow<AppPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        val rawLang = prefs[Keys.LANGUAGE_CODE] ?: "en"
        val normalizedLang = if (rawLang == "ar") "ar" else "en"
        AppPreferences(
            languageCode = normalizedLang,
            prioritizeUnmastered = prefs[Keys.PRIORITIZE_UNMASTERED] ?: true,
        )
    }

    suspend fun getSnapshot(): AppPreferences = preferencesFlow.first()

    suspend fun setLanguageCode(languageCode: String) {
        val normalized = if (languageCode == "ar") "ar" else "en"
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = normalized
        }
    }

    suspend fun toggleLanguage() {
        context.userPreferencesDataStore.edit { prefs ->
            val current = prefs[Keys.LANGUAGE_CODE] ?: "en"
            prefs[Keys.LANGUAGE_CODE] = if (current == "ar") "en" else "ar"
        }
    }

    suspend fun setPrioritizeUnmastered(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.PRIORITIZE_UNMASTERED] = enabled
        }
    }
}
