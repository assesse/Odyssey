package com.halmeoni.transit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SeniorColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryWhite,
    secondary = SecondaryDarkBlue,
    background = BackgroundWhite,
    surface = SurfaceLight,
    onBackground = TextBlack,
    onSurface = TextBlack,
    error = ActionRed
)

@Composable
fun HalmeoniTransitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeniorColorScheme,
        typography = Typography,
        content = content
    )
}
