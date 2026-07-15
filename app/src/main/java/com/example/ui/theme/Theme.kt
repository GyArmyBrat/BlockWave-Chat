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
    primary = NeonCyan,
    secondary = NavyBlue,
    tertiary = ElectricBlue,
    background = CyberBg,
    surface = CyberCard,
    onPrimary = CyberBg,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
  )

private val LightColorScheme = DarkColorScheme // Force dark theme for cyberpunk blockchain aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for premium dark aesthetic
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored cyberpunk brand
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
