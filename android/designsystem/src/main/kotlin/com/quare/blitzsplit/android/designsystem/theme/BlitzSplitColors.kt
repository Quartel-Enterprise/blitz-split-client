package com.quare.blitzsplit.android.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Why: the tokens of the design canvas (Android, Material 3 Expressive), seeded from #266489.
internal val lightBlitzSplitColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF266489),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E6FF),
    onPrimaryContainer = Color(0xFF001E2F),
    secondaryContainer = Color(0xFFCDE5FF),
    onSecondaryContainer = Color(0xFF001D32),
    tertiaryContainer = Color(0xFFC3E7FF),
    onTertiaryContainer = Color(0xFF001E2C),
    error = Color(0xFF8E4D31),
    errorContainer = Color(0xFFFFDBCE),
    onErrorContainer = Color(0xFF370E00),
    background = Color(0xFFF7F9FF),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF181C20),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
    inverseSurface = Color(0xFF2D3135),
    inverseOnSurface = Color(0xFFEEF1F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F9),
    surfaceContainer = Color(0xFFEBEEF3),
    surfaceContainerHigh = Color(0xFFE5E8ED),
)

internal val darkBlitzSplitColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF95CDF7),
    onPrimary = Color(0xFF00344E),
    primaryContainer = Color(0xFF004B6F),
    onPrimaryContainer = Color(0xFFC9E6FF),
    secondaryContainer = Color(0xFF094B72),
    onSecondaryContainer = Color(0xFFCDE5FF),
    tertiaryContainer = Color(0xFF004C69),
    onTertiaryContainer = Color(0xFFC3E7FF),
    error = Color(0xFFFFB599),
    errorContainer = Color(0xFF71361C),
    onErrorContainer = Color(0xFFFFDBCE),
    background = Color(0xFF101417),
    surface = Color(0xFF101417),
    onSurface = Color(0xFFE0E3E8),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41474D),
    inverseSurface = Color(0xFFE0E3E8),
    inverseOnSurface = Color(0xFF2D3135),
    surfaceContainerLowest = Color(0xFF1C2024),
    surfaceContainerLow = Color(0xFF181C20),
    surfaceContainer = Color(0xFF1C2024),
    surfaceContainerHigh = Color(0xFF262A2E),
)
