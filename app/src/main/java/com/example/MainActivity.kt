package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ai.configureAppCheck
import com.example.auth.GoogleAuthHelper
import com.example.ui.AwakenApp
import com.example.ui.theme.AwakenTheme
import com.example.ui.theme.AwakenThemeMode
import com.example.ui.theme.ThemePreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check before any UI, ViewModels, or Firebase operations
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    val credentialManager = CredentialManager.create(this)
    GoogleAuthHelper.attemptSilentSignIn(
      context = this,
      credentialManager = credentialManager,
      scope = lifecycleScope,
      onSuccess = {},
      onDoneUnauthenticated = {}
    )

    setContent {
      val themePrefs = remember { ThemePreferences(applicationContext) }
      val themeMode by themePrefs.themeModeFlow.collectAsStateWithLifecycle(
        initialValue = AwakenThemeMode.DARK
      )
      val scope = rememberCoroutineScope()

      LaunchedEffect(themeMode) {
        if (themeMode == AwakenThemeMode.DARK) {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
          )
        } else {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
          )
        }
      }

      AwakenTheme(themeMode = themeMode) {
        AwakenApp(
          themeMode = themeMode,
          onThemeModeChange = { nextMode ->
            scope.launch { themePrefs.setThemeMode(nextMode) }
          }
        )
      }
    }
  }
}
