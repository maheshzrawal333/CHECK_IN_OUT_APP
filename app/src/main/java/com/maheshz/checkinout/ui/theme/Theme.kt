package com.maheshz.checkinout.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
  lightColorScheme(
    primary = BrandPurple,
    onPrimary = WhiteSurface,
    primaryContainer = LightPurple,
    onPrimaryContainer = DarkPurple,
    secondary = TealAccent,
    background = BgColor,
    onBackground = PrimaryText,
    surface = WhiteSurface,
    onSurface = PrimaryText,
    surfaceVariant = LightGrayBorder,
    onSurfaceVariant = SecondaryText
  )

@Composable
fun MyApplicationTheme(
  // We force light theme and disable dynamic color to strictly apply the Bold Typography visual identity
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
