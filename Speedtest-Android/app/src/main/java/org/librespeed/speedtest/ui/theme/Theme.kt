package org.librespeed.speedtest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF2DD4BF)
val TealDeep = Color(0xFF14B8A6)
val Purple = Color(0xFFA78BFA)

/** The darker teal light mode needs; also its [primary]. */
val TealDark = Color(0xFF0F766E)

/**
 * Hue-carrying accents resolved per theme: the two transfer directions and the
 * latency scale. The bright dark-mode tones sit below the 3:1 contrast minimum
 * on the light surfaces (TealDeep 2.49:1, Amber 2.28:1), so light mode gets
 * darker shades of the same hues rather than a different palette.
 */
data class SpeedAccents(
    val download: Color,
    val upload: Color,
    val warn: Color,
    val bad: Color
)

private val DarkAccents = SpeedAccents(
    download = Teal,
    upload = Purple,
    warn = Color(0xFFF7941D),
    //the same value as DangerRed, spelled out because that one is declared
    //further down the file and top-level initialisation runs in order
    bad = Color(0xFFEF6461)
)
private val LightAccents = SpeedAccents(
    download = TealDark,
    upload = Color(0xFF7C5CD6),
    warn = Color(0xFFB45309),
    bad = Color(0xFFC0342F)
)

val LocalSpeedAccents = staticCompositionLocalOf { DarkAccents }
val NightBackground = Color(0xFF0C111C)
val NightSurface = Color(0xFF151C2C)
val NightSurfaceHigh = Color(0xFF1D2537)
val DayBackground = Color(0xFFF6F8FB)
val DaySurface = Color(0xFFFFFFFF)
val DangerRed = Color(0xFFEF6461)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0F3B36),
    onPrimaryContainer = Teal,
    secondary = Purple,
    onSecondary = Color(0xFF1F1147),
    secondaryContainer = Color(0xFF2E2258),
    onSecondaryContainer = Purple,
    background = NightBackground,
    onBackground = Color(0xFFE4E9F2),
    surface = NightSurface,
    onSurface = Color(0xFFE4E9F2),
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = Color(0xFF9AA4B8),
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    error = DangerRed,
    outline = Color(0xFF3A445A)
)

private val LightColors = lightColorScheme(
    //TealDeep reads at 2.49:1 on these surfaces, both as text and behind
    //onPrimary; the darker teal clears 4.5:1 in either direction
    primary = TealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F5EE),
    onPrimaryContainer = Color(0xFF00332E),
    secondary = Color(0xFF7C5CD6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E0FB),
    onSecondaryContainer = Color(0xFF2A1A5E),
    background = DayBackground,
    onBackground = Color(0xFF1A1F2B),
    surface = DaySurface,
    onSurface = Color(0xFF1A1F2B),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF5A6478),
    surfaceContainer = DaySurface,
    surfaceContainerHigh = Color(0xFFF0F3F8),
    error = Color(0xFFC0342F),
    outline = Color(0xFFC3CAD8)
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun LibreSpeedTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalSpeedAccents provides if (dark) DarkAccents else LightAccents) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            content = content
        )
    }
}
