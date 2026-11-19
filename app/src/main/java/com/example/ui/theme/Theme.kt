package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.Perspective
import com.example.data.model.VisualThemeMode

@Immutable
data class DialecticSurfacePalette(
    val surfaceLowest: Color,
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val surfaceBright: Color,
    val isDark: Boolean
)

val LocalDialecticSurfaces = staticCompositionLocalOf {
    DialecticSurfacePalette(
        surfaceLowest = SurfaceLowestDark,
        surfaceLow = SurfaceLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceHigh = SurfaceHighDark,
        surfaceHighest = SurfaceHighestDark,
        surfaceBright = SurfaceBrightDark,
        isDark = true
    )
}

private val DialecticDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = PrimaryDark,
    onTertiary = OnPrimaryDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceHighestDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val DialecticLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = PrimaryLight,
    onTertiary = OnPrimaryLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceHighestLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

@Composable
fun DialecticTheme(
    themeMode: VisualThemeMode = VisualThemeMode.DARK,
    activePerspective: Perspective = Perspective.SUBJECT,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        VisualThemeMode.DARK -> true
        VisualThemeMode.LIGHT -> false
        VisualThemeMode.DUAL -> activePerspective == Perspective.SUBJECT
    }

    val colorScheme = if (useDark) DialecticDarkColorScheme else DialecticLightColorScheme
    val surfacePalette = if (useDark) {
        DialecticSurfacePalette(
            surfaceLowest = SurfaceLowestDark,
            surfaceLow = SurfaceLowDark,
            surfaceContainer = SurfaceContainerDark,
            surfaceHigh = SurfaceHighDark,
            surfaceHighest = SurfaceHighestDark,
            surfaceBright = SurfaceBrightDark,
            isDark = true
        )
    } else {
        DialecticSurfacePalette(
            surfaceLowest = SurfaceLowestLight,
            surfaceLow = SurfaceLowLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceHigh = SurfaceHighLight,
            surfaceHighest = SurfaceHighestLight,
            surfaceBright = SurfaceBrightLight,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalDialecticSurfaces provides surfacePalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DialecticTypography,
            content = content
        )
    }
}
