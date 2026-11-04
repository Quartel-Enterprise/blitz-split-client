package com.quare.blitzsplit.android.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val blitzSplitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Why: the profile picks Light, Dark or the system's, so the colors outside the scheme follow the app's theme too.
val LocalIsDarkTheme = staticCompositionLocalOf { false }

// Why: a theme nested to force dark (the QR reader's camera) keeps the wallpaper's colors when they are on.
val LocalUsesDynamicColors = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BlitzSplitTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    usesDynamicColors: Boolean = LocalUsesDynamicColors.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalUsesDynamicColors provides usesDynamicColors,
    ) {
        MaterialExpressiveTheme(
            colorScheme = blitzSplitColorScheme(
                isDark = isDark,
                usesDynamicColors = usesDynamicColors,
            ),
            motionScheme = MotionScheme.expressive(),
            shapes = blitzSplitShapes,
            content = content,
        )
    }
}

/*
 * Why: Material You (Android 12 and later) takes the wallpaper's scheme; the group and category colors stay the brand's.
 * Aparência's previews draw the light and the dark one whatever the app shows now.
 */
@Composable
fun blitzSplitColorScheme(
    isDark: Boolean,
    usesDynamicColors: Boolean,
): ColorScheme {
    val context = LocalContext.current
    // Why: the wallpaper's scheme reads dozens of system colors; a new one each time would recompose every reader.
    return remember(context, isDark, usesDynamicColors) {
        when {
            usesDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            isDark -> darkBlitzSplitColors

            else -> lightBlitzSplitColors
        }
    }
}
