package com.quare.blitzsplit.android.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Why: the tokens of the design canvas (Android, Material 3 Expressive), seeded from #266489. Every role is set, the
 * fixed ones included: one left out falls back to Material's baseline purple (a lilac field, a pink dot) among
 * the blues.
 */
internal val lightBlitzSplitColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF266489),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E6FF),
    onPrimaryContainer = Color(0xFF001E2F),
    inversePrimary = Color(0xFF95CDF7),
    secondary = Color(0xFF496177),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE5FF),
    onSecondaryContainer = Color(0xFF001D32),
    tertiary = Color(0xFF416276),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC3E7FF),
    onTertiaryContainer = Color(0xFF001E2C),
    error = Color(0xFF8E4D31),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDBCE),
    onErrorContainer = Color(0xFF370E00),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    surfaceTint = Color(0xFF266489),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
    inverseSurface = Color(0xFF2D3135),
    inverseOnSurface = Color(0xFFEEF1F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F9),
    surfaceContainer = Color(0xFFEBEEF3),
    surfaceContainerHigh = Color(0xFFE5E8ED),
    surfaceContainerHighest = Color(0xFFDFE3E8),
    surfaceDim = Color(0xFFD7DADF),
    surfaceBright = Color(0xFFF7F9FF),
).withFixedRoles()

internal val darkBlitzSplitColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF95CDF7),
    onPrimary = Color(0xFF00344E),
    primaryContainer = Color(0xFF004B6F),
    onPrimaryContainer = Color(0xFFC9E6FF),
    inversePrimary = Color(0xFF266489),
    secondary = Color(0xFFB1C9E3),
    onSecondary = Color(0xFF1A3247),
    secondaryContainer = Color(0xFF094B72),
    onSecondaryContainer = Color(0xFFCDE5FF),
    tertiary = Color(0xFFA8CBE2),
    onTertiary = Color(0xFF0E3446),
    tertiaryContainer = Color(0xFF004C69),
    onTertiaryContainer = Color(0xFFC3E7FF),
    error = Color(0xFFFFB599),
    onError = Color(0xFF552108),
    errorContainer = Color(0xFF71361C),
    onErrorContainer = Color(0xFFFFDBCE),
    background = Color(0xFF101417),
    onBackground = Color(0xFFE0E3E8),
    surface = Color(0xFF101417),
    onSurface = Color(0xFFE0E3E8),
    surfaceVariant = Color(0xFF41474D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    surfaceTint = Color(0xFF95CDF7),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41474D),
    inverseSurface = Color(0xFFE0E3E8),
    inverseOnSurface = Color(0xFF2D3135),
    surfaceContainerLowest = Color(0xFF1C2024),
    surfaceContainerLow = Color(0xFF181C20),
    surfaceContainer = Color(0xFF1C2024),
    surfaceContainerHigh = Color(0xFF262A2E),
    surfaceContainerHighest = Color(0xFF313539),
    surfaceDim = Color(0xFF101417),
    surfaceBright = Color(0xFF353A3E),
).withFixedRoles()

private val lightPositive = Color(0xFF006C4C)
private val darkPositive = Color(0xFF6EDBAA)
private val lightPending = Color(0xFF7C5800)
private val darkPending = Color(0xFFF9BC3E)

// Why: the attachment viewer stays black in both themes, so it takes the dark theme's amber, as the camera does.
val pendingOnBlackColor: Color = darkPending

/*
 * Why: what comes back to the user (a positive net, "a receber", "te deve") is green on every platform, apart from
 * the primary of links and buttons; outside the scheme, so the wallpaper's colors never replace it.
 */
@Composable
fun positiveAmountColor(): Color = if (LocalIsDarkTheme.current) darkPositive else lightPositive

/*
 * Why: what is saved here and not on the server yet (the queue's dot, "Aguardando envio") is amber on every platform:
 * it reads as text (5:1 or more) and stays apart from the error's orange of what is owed. Outside the scheme too, so
 * the wallpaper's tertiary never replaces it.
 */
@Composable
fun pendingColor(): Color = if (LocalIsDarkTheme.current) darkPending else lightPending

// Why: on the inverse surface (a badge over a photo) the other theme's amber is the one that reads.
@Composable
fun pendingOnInverseSurfaceColor(): Color = if (LocalIsDarkTheme.current) lightPending else darkPending

// Why: the fixed roles keep the same tones in light and dark, so both schemes take them from here.
private fun ColorScheme.withFixedRoles(): ColorScheme = copy(
    primaryFixed = Color(0xFFC9E6FF),
    primaryFixedDim = Color(0xFF95CDF7),
    onPrimaryFixed = Color(0xFF001E2F),
    onPrimaryFixedVariant = Color(0xFF004B6F),
    secondaryFixed = Color(0xFFCDE5FF),
    secondaryFixedDim = Color(0xFFB1C9E3),
    onSecondaryFixed = Color(0xFF001D32),
    onSecondaryFixedVariant = Color(0xFF31495E),
    tertiaryFixed = Color(0xFFC3E7FF),
    tertiaryFixedDim = Color(0xFFA8CBE2),
    onTertiaryFixed = Color(0xFF001E2C),
    onTertiaryFixedVariant = Color(0xFF284B5E),
)
