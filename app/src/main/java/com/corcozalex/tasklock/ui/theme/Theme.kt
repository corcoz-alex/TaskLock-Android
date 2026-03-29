package com.corcozalex.tasklock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = BlueContainer,
    primaryContainer = BluePrimaryDark,
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF2B3748),
    tertiary = BluePrimaryDark,
    background = Color(0xFF12161D),
    surface = Color(0xFF1A202B),
    surfaceVariant = Color(0xFF232B37),
    surfaceTint = BluePrimary,
    surfaceContainerLowest = Color(0xFF12161D),
    surfaceContainerLow = Color(0xFF171D27),
    surfaceContainer = Color(0xFF1A202B),
    surfaceContainerHigh = Color(0xFF1F2733),
    surfaceContainerHighest = Color(0xFF232B37),
    outline = Color(0xFF657185),
    outlineVariant = Color(0xFF3A4557),
    onPrimary = Color.White,
    onSecondaryContainer = Color(0xFFE8EDF6),
    onBackground = Color(0xFFE8EDF6),
    onSurface = Color(0xFFE8EDF6),
    onSurfaceVariant = Color(0xFFB2BECE)
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueContainer,
    primaryContainer = BlueContainer,
    onPrimaryContainer = TextPrimary,
    secondaryContainer = Color(0xFFDCEBFF),
    tertiary = BluePrimaryDark,
    background = GrayBackground,
    surface = GraySurface,
    surfaceVariant = GraySurfaceVariant,
    surfaceTint = BluePrimary,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFF2F3F5),
    surfaceContainerHigh = Color(0xFFEDEFF2),
    surfaceContainerHighest = Color(0xFFE7E9ED),
    outline = Color(0xFF8C97A8),
    outlineVariant = Color(0xFFD1D5DC),
    onPrimary = Color.White,
    onSecondaryContainer = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun TaskLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}