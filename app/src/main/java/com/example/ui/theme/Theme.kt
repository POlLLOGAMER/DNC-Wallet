package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TechCyan,
    onPrimary = SpaceBlack,
    secondary = DncGreen,
    onSecondary = SpaceBlack,
    tertiary = AlertAmber,
    onTertiary = SpaceBlack,
    background = SpaceBlack,
    onBackground = TextLight,
    surface = CardSlate,
    onSurface = TextLight,
    surfaceVariant = BorderSlate,
    onSurfaceVariant = SubtleText
  )

private val LightColorScheme = DarkColorScheme // Default to premium dark even in light mode

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force visual premium consistency
  dynamicColor: Boolean = false, // Preserve brand-specific tech colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
