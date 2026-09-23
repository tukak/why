package cz.kutner.why.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import cz.kutner.why.data.settings.ThemeMode

private val BloomLight = lightColorScheme(
    primary = Color(0xFF231A12),
    onPrimary = Color(0xFFFBF6EE),
    primaryContainer = Color(0xFFFFE1CF),
    onPrimaryContainer = Color(0xFF7A3A12),
    secondaryContainer = Color(0xFFFFD2B8),
    onSecondaryContainer = Color(0xFF231A12),
    tertiary = Color(0xFFC4480A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE1CF),
    onTertiaryContainer = Color(0xFF7A3A12),
    background = Color(0xFFFBF6EE),
    onBackground = Color(0xFF231A12),
    surface = Color(0xFFFBF6EE),
    onSurface = Color(0xFF231A12),
    onSurfaceVariant = Color(0xFF5E5245),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F0E4),
    surfaceContainer = Color(0xFFF3EBDD),
    surfaceContainerHigh = Color(0xFFF1E8DA),
    surfaceContainerHighest = Color(0xFFEBE1D1),
    outline = Color(0xFF857868),
    outlineVariant = Color(0xFFE6DCCD),
)

private val BloomDark = darkColorScheme(
    primary = Color(0xFFF4EDE3),
    onPrimary = Color(0xFF17130F),
    primaryContainer = Color(0xFF4A2410),
    onPrimaryContainer = Color(0xFFFFC7A3),
    secondaryContainer = Color(0xFF5A2C12),
    onSecondaryContainer = Color(0xFFF4EDE3),
    tertiary = Color(0xFFFF9A5C),
    onTertiary = Color(0xFF3A1A06),
    tertiaryContainer = Color(0xFF4A2410),
    onTertiaryContainer = Color(0xFFFFC7A3),
    background = Color(0xFF17130F),
    onBackground = Color(0xFFF4EDE3),
    surface = Color(0xFF17130F),
    onSurface = Color(0xFFF4EDE3),
    onSurfaceVariant = Color(0xFFC4B8A8),
    surfaceContainerLowest = Color(0xFF120F0C),
    surfaceContainerLow = Color(0xFF1C1713),
    surfaceContainer = Color(0xFF221C17),
    surfaceContainerHigh = Color(0xFF2C251F),
    surfaceContainerHighest = Color(0xFF362E27),
    outline = Color(0xFF8F8374),
    outlineVariant = Color(0xFF3A3129),
)

/** Reason colors have their own light and dark tones, independent of the color scheme. */
val LocalIsDark = staticCompositionLocalOf { false }

@Composable
fun WhyTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && dark -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        dark -> BloomDark
        else -> BloomLight
    }
    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            typography = BloomTypography,
            content = content,
        )
    }
}
