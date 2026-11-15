package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = ChampagneGoldDark,
    onSecondary = Color.White,
    secondaryContainer = ChampagneSoft,
    onSecondaryContainer = ChampagneOnContainer,
    tertiary = EmeraldDeep,
    onTertiary = ChampagneGold,
    background = IvoryWhite,
    onBackground = InkCharcoal,
    surface = IvoryCard,
    onSurface = InkCharcoal,
    surfaceVariant = SoftBeige,
    onSurfaceVariant = InkMuted,
    outline = WarmBeigeBorder,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedBg,
    onErrorContainer = AlertRed
)

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = EmeraldDeep,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = IvoryWhite,
    secondary = EmeraldContainer,
    onSecondary = EmeraldDeep,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = ChampagneSoft,
    tertiary = ChampagneGold,
    onTertiary = EmeraldDeep,
    background = DarkSurface,
    onBackground = IvoryWhite,
    surface = DarkCard,
    onSurface = IvoryWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB8C9C2),
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val NaqshShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NaqshCollectionTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = NaqshTypography,
        shapes = NaqshShapes,
        content = content
    )
}
