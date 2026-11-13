package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Dark Palette (Uploaded Cobalt-to-Ice Gradient: #03195B -> #0B2CB1 -> #1951FC -> #3781FC -> #CBE9FD)
val DeepAbyssBackground = Color(0xFF040814)
val MidnightNavySurface = Color(0xFF070E24)
val MidnightNavyCard = Color(0xFF0A1436)
val RoyalNavyElevated = Color(0xFF03195B)
val CobaltBorder = Color(0xFF19378F)

val IceWhiteText = Color(0xFFF1F7FF)
val MistBlueText = Color(0xFF9EC2EB)
val SlateBlueMuted = Color(0xFF6783AE)

val RoyalNavyDeep = Color(0xFF03195B)
val DeepCobalt = Color(0xFF0B2CB1)
val ElectricBlue = Color(0xFF1951FC)
val AzureAccent = Color(0xFF3781FC)
val IceBlueBright = Color(0xFFCBE9FD)
val OnAzureAccent = Color(0xFF021038)

// Light Palette (Warm Alabaster & Burnished Gold)
val AlabasterBackground = Color(0xFFF6F4EF)
val StoneSurface = Color(0xFFECE9E2)
val PaperCard = Color(0xFFFFFFFF)
val StoneElevated = Color(0xFFF1EEE8)
val StoneBorder = Color(0xFFDAD4C8)

val ObsidianInk = Color(0xFF17191E)
val SlateSecondary = Color(0xFF5A616C)
val SlateMuted = Color(0xFF7D8490)

val BurnishedGold = Color(0xFF9A742B)
val BurnishedGoldDeep = Color(0xFF7C5A1B)
val ChampagneContainer = Color(0xFFF3EAD7)
val OnBurnishedGold = Color(0xFFFFFFFF)

@Immutable
data class AwakenPalette(
  val isDark: Boolean,
  val background: Color,
  val surface: Color,
  val card: Color,
  val elevated: Color,
  val border: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textMuted: Color,
  val accent: Color,
  val accentBright: Color,
  val accentContainer: Color,
  val onAccent: Color,
  val gradientStart: Color,
  val gradientMid: Color,
  val gradientEnd: Color,
  val statusCompleted: Color,
  val statusCompletedContainer: Color,
  val statusInProgress: Color,
  val statusInProgressContainer: Color,
  val statusPlanned: Color,
  val statusPlannedContainer: Color,
  val statusRest: Color,
  val statusRestContainer: Color,
  val statusMissed: Color,
  val statusMissedContainer: Color,
  val statusCancelled: Color,
  val statusCancelledContainer: Color
)

val AwakenDarkPalette = AwakenPalette(
  isDark = true,
  background = DeepAbyssBackground,
  surface = MidnightNavySurface,
  card = MidnightNavyCard,
  elevated = RoyalNavyElevated,
  border = CobaltBorder,
  textPrimary = IceWhiteText,
  textSecondary = MistBlueText,
  textMuted = SlateBlueMuted,
  accent = AzureAccent,
  accentBright = IceBlueBright,
  accentContainer = DeepCobalt,
  onAccent = OnAzureAccent,
  gradientStart = RoyalNavyDeep,
  gradientMid = ElectricBlue,
  gradientEnd = IceBlueBright,
  statusCompleted = Color(0xFF4CC38A),
  statusCompletedContainer = Color(0xFF0C2B25),
  statusInProgress = AzureAccent,
  statusInProgressContainer = RoyalNavyDeep,
  statusPlanned = IceBlueBright,
  statusPlannedContainer = DeepCobalt,
  statusRest = MistBlueText,
  statusRestContainer = Color(0xFF0E1D46),
  statusMissed = Color(0xFFE06C75),
  statusMissedContainer = Color(0xFF2B1220),
  statusCancelled = SlateBlueMuted,
  statusCancelledContainer = Color(0xFF101936)
)

val AwakenLightPalette = AwakenPalette(
  isDark = false,
  background = AlabasterBackground,
  surface = StoneSurface,
  card = PaperCard,
  elevated = StoneElevated,
  border = StoneBorder,
  textPrimary = ObsidianInk,
  textSecondary = SlateSecondary,
  textMuted = SlateMuted,
  accent = BurnishedGold,
  accentBright = BurnishedGoldDeep,
  accentContainer = ChampagneContainer,
  onAccent = OnBurnishedGold,
  gradientStart = ChampagneContainer,
  gradientMid = BurnishedGold,
  gradientEnd = BurnishedGoldDeep,
  statusCompleted = Color(0xFF246F43),
  statusCompletedContainer = Color(0xFFE2F2E8),
  statusInProgress = Color(0xFF295B90),
  statusInProgressContainer = Color(0xFFE3EEF8),
  statusPlanned = BurnishedGoldDeep,
  statusPlannedContainer = Color(0xFFF4EAD5),
  statusRest = Color(0xFF4D647A),
  statusRestContainer = Color(0xFFE8EEF4),
  statusMissed = Color(0xFFB33B3B),
  statusMissedContainer = Color(0xFFFAE6E6),
  statusCancelled = Color(0xFF5A616B),
  statusCancelledContainer = Color(0xFFEBECF0)
)
