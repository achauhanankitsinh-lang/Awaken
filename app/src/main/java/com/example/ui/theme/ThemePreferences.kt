package com.example.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AwakenThemeMode(val wireValue: String, val label: String) {
  DARK("dark", "Dark"),
  LIGHT("light", "Light");

  companion object {
    fun fromWire(value: String?): AwakenThemeMode {
      return entries.firstOrNull { it.wireValue == value } ?: DARK
    }
  }
}

private val Context.awakenThemeDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "awaken_theme_prefs"
)

class ThemePreferences(context: Context) {
  private val appContext = context.applicationContext
  private val themeKey = stringPreferencesKey("theme_mode")

  val themeModeFlow: Flow<AwakenThemeMode> = appContext.awakenThemeDataStore.data
    .map { prefs -> AwakenThemeMode.fromWire(prefs[themeKey]) }

  suspend fun setThemeMode(mode: AwakenThemeMode) {
    appContext.awakenThemeDataStore.edit { prefs ->
      prefs[themeKey] = mode.wireValue
    }
  }
}
