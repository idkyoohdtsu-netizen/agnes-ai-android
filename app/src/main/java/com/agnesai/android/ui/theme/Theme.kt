package com.agnesai.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    primaryContainer = Purple30,
    onPrimaryContainer = Purple90,

    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = Indigo30,
    onSecondaryContainer = Indigo90,

    tertiary = AccentTeal,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF003731),
    onTertiaryContainer = Color(0xFF70F6C8),

    error = ErrorColor,
    onError = Color.White,
    errorContainer = Color(0xFF8C0009),
    onErrorContainer = Color(0xFFFFDAD6),

    background = DarkBackground,
    onBackground = OnDarkBackground,

    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SubtleText,

    outline = DividerColor,
    outlineVariant = Color(0xFF1E1E40),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E0F0),
    inverseOnSurface = Color(0xFF1A1A2E),
    inversePrimary = DeepPurple,

    surfaceTint = AccentPurple,
)

@Composable
fun AgnesAITheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
