package ba.noniboy.struja.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkPrimary,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF3A2E5A),
    onSecondaryContainer = DarkSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    tertiary = DarkSecondary,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF3A2E5A),
    onTertiaryContainer = DarkSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF00363A),
    secondary = LightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DDF8),
    onSecondaryContainer = Color(0xFF250057),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE0F2F1),
    onSurfaceVariant = Color(0xFF000000),
    tertiary = LightSecondary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DDF8),
    onTertiaryContainer = Color(0xFF250057)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrujaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
