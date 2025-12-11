package com.example.project2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueContainer,
    onPrimary = Color.White,
    primaryContainer = RoyalBluePrimary,
    onPrimaryContainer = Color(0xFFD3DBFF),
    secondary = DeepNavySecondary,
    onSecondary = Color.White,
    secondaryContainer = DeepNavySecondaryContainer,
    onSecondaryContainer = CharcoalBase,
    tertiary = SteelBlueTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SteelBlueTertiaryContainer,
    onTertiaryContainer = CharcoalBase,
    background = CharcoalBase,
    onBackground = Color.White,
    surface = CharcoalSurface,
    onSurface = Color.White,
    surfaceVariant = RoyalBluePrimary,
    onSurfaceVariant = LightMist,
    outline = CoolGray
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBluePrimary,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = Color(0xFFD3DBFF),
    secondary = DeepNavySecondary,
    onSecondary = Color.White,
    secondaryContainer = DeepNavySecondaryContainer,
    onSecondaryContainer = CharcoalBase,
    tertiary = SteelBlueTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SteelBlueTertiaryContainer,
    onTertiaryContainer = CharcoalBase,
    background = LightMist,
    onBackground = CharcoalBase,
    surface = Color.White,
    onSurface = CharcoalBase,
    surfaceVariant = LightMist,
    onSurfaceVariant = RoyalBluePrimary,
    outline = CoolGray
)

/**
 * App-wide Material3 theme using custom brand colors with optional dynamic colors.
 *
 * @param darkTheme whether to force dark mode (defaults to system)
 * @param dynamicColor enable Android 12 dynamic color harmonization
 * @param content themed composable content
 */
@Composable
fun Project2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
