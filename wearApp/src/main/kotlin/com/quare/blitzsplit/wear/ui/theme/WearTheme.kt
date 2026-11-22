package com.quare.blitzsplit.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalanceDirection

/*
 * Why: the watch is dark only (canvas page 15): the brand's dark tokens on black. Money keeps the app's green and
 * orange, and the pending dot its amber, which Wear's scheme has no role for.
 */
private val wearColorScheme = ColorScheme(
    primary = Color(WearColors.PRIMARY),
    onPrimary = Color(WearColors.ON_PRIMARY),
    primaryContainer = Color(WearColors.PRIMARY_CONTAINER),
    onPrimaryContainer = Color(WearColors.ON_PRIMARY_CONTAINER),
    surfaceContainerLow = Color(0xFF1C2024),
    surfaceContainer = Color(0xFF1C2024),
    surfaceContainerHigh = Color(0xFF262A2E),
    onSurface = Color(WearColors.ON_SURFACE),
    onSurfaceVariant = Color(WearColors.ON_SURFACE_VARIANT),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF313539),
    background = Color.Black,
    onBackground = Color(0xFFE0E3E8),
    error = Color(0xFFFFB599),
    onError = Color(0xFF5A1C00),
)

val LocalMoneyColors = staticCompositionLocalOf {
    MoneyColors(
        toReceive = Color(WearColors.TO_RECEIVE),
        toPay = Color(WearColors.TO_PAY),
        pending = Color(0xFFD97706),
    )
}

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    // Why: the design system's avatars and badges read it, and the watch is always dark.
    CompositionLocalProvider(LocalIsDarkTheme provides true) {
        MaterialTheme(
            colorScheme = wearColorScheme,
            content = content,
        )
    }
}

// Why: green for what you receive, orange for what you owe, as every screen of the app colors money; even reads plain.
@Composable
fun moneyColorOf(direction: WatchBalanceDirection): Color {
    val colors = LocalMoneyColors.current
    return when (direction) {
        WatchBalanceDirection.TO_RECEIVE -> colors.toReceive
        WatchBalanceDirection.TO_PAY -> colors.toPay
        WatchBalanceDirection.EVEN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun moneyColorOf(amountMinor: Long): Color = moneyColorOf(WatchBalanceDirection.of(amountMinor))
