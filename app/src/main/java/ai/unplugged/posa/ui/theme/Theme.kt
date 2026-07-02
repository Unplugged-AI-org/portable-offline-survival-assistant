package ai.unplugged.posa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = FieldCanvas,
    secondary = SlateBlue,
    onSecondary = FieldCanvas,
    secondaryContainer = ColorTokens.LightSecondaryContainer,
    onSecondaryContainer = ColorTokens.LightOnSecondaryContainer,
    tertiary = SignalAmber,
    onTertiary = FieldCanvasDark,
    background = FieldCanvas,
    onBackground = FieldCanvasDark,
    surface = FieldCanvas,
    onSurface = FieldCanvasDark,
    error = ClayRed,
)

private val DarkColors = darkColorScheme(
    primary = PineDark,
    onPrimary = FieldCanvasDark,
    secondary = SlateBlueDark,
    onSecondary = FieldCanvasDark,
    secondaryContainer = ColorTokens.DarkSecondaryContainer,
    onSecondaryContainer = ColorTokens.DarkOnSecondaryContainer,
    tertiary = SignalAmberDark,
    onTertiary = FieldCanvasDark,
    background = FieldCanvasDark,
    onBackground = FieldCanvas,
    surface = FieldCanvasDark,
    onSurface = FieldCanvas,
    error = ClayRed,
)

@Composable
fun PosaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
