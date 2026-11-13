package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalAwakenPalette = staticCompositionLocalOf { AwakenDarkPalette }

object AwakenThemeTokens {
  val colors: AwakenPalette
    @Composable
    get() = LocalAwakenPalette.current
}

private val AwakenDarkColorScheme = darkColorScheme(
  primary = AwakenDarkPalette.accent,
  onPrimary = AwakenDarkPalette.onAccent,
  primaryContainer = AwakenDarkPalette.accentContainer,
  onPrimaryContainer = AwakenDarkPalette.accentBright,
  secondary = AwakenDarkPalette.textSecondary,
  onSecondary = AwakenDarkPalette.background,
  secondaryContainer = AwakenDarkPalette.elevated,
  onSecondaryContainer = AwakenDarkPalette.textPrimary,
  tertiary = AwakenDarkPalette.statusCompleted,
  onTertiary = AwakenDarkPalette.background,
  tertiaryContainer = AwakenDarkPalette.statusCompletedContainer,
  onTertiaryContainer = AwakenDarkPalette.statusCompleted,
  background = AwakenDarkPalette.background,
  onBackground = AwakenDarkPalette.textPrimary,
  surface = AwakenDarkPalette.surface,
  onSurface = AwakenDarkPalette.textPrimary,
  surfaceVariant = AwakenDarkPalette.card,
  onSurfaceVariant = AwakenDarkPalette.textSecondary,
  surfaceContainerLow = AwakenDarkPalette.surface,
  surfaceContainer = AwakenDarkPalette.card,
  surfaceContainerHigh = AwakenDarkPalette.elevated,
  outline = AwakenDarkPalette.border,
  outlineVariant = AwakenDarkPalette.border,
  error = AwakenDarkPalette.statusMissed,
  onError = AwakenDarkPalette.background,
  errorContainer = AwakenDarkPalette.statusMissedContainer,
  onErrorContainer = AwakenDarkPalette.statusMissed
)

private val AwakenLightColorScheme = lightColorScheme(
  primary = AwakenLightPalette.accent,
  onPrimary = AwakenLightPalette.onAccent,
  primaryContainer = AwakenLightPalette.accentContainer,
  onPrimaryContainer = AwakenLightPalette.accentBright,
  secondary = AwakenLightPalette.textSecondary,
  onSecondary = AwakenLightPalette.card,
  secondaryContainer = AwakenLightPalette.elevated,
  onSecondaryContainer = AwakenLightPalette.textPrimary,
  tertiary = AwakenLightPalette.statusCompleted,
  onTertiary = AwakenLightPalette.card,
  tertiaryContainer = AwakenLightPalette.statusCompletedContainer,
  onTertiaryContainer = AwakenLightPalette.statusCompleted,
  background = AwakenLightPalette.background,
  onBackground = AwakenLightPalette.textPrimary,
  surface = AwakenLightPalette.surface,
  onSurface = AwakenLightPalette.textPrimary,
  surfaceVariant = AwakenLightPalette.card,
  onSurfaceVariant = AwakenLightPalette.textSecondary,
  surfaceContainerLow = AwakenLightPalette.surface,
  surfaceContainer = AwakenLightPalette.card,
  surfaceContainerHigh = AwakenLightPalette.elevated,
  outline = AwakenLightPalette.border,
  outlineVariant = AwakenLightPalette.border,
  error = AwakenLightPalette.statusMissed,
  onError = AwakenLightPalette.card,
  errorContainer = AwakenLightPalette.statusMissedContainer,
  onErrorContainer = AwakenLightPalette.statusMissed
)

val AwakenShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AwakenTheme(
  themeMode: AwakenThemeMode = AwakenThemeMode.DARK,
  content: @Composable () -> Unit
) {
  val palette = when (themeMode) {
    AwakenThemeMode.DARK -> AwakenDarkPalette
    AwakenThemeMode.LIGHT -> AwakenLightPalette
  }
  val colorScheme = when (themeMode) {
    AwakenThemeMode.DARK -> AwakenDarkColorScheme
    AwakenThemeMode.LIGHT -> AwakenLightColorScheme
  }

  CompositionLocalProvider(LocalAwakenPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AwakenTypography,
      shapes = AwakenShapes,
      content = content
    )
  }
}
