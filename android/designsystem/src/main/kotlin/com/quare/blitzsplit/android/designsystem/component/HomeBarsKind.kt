package com.quare.blitzsplit.android.designsystem.component

// Why: the bottom bar on a phone upright, the rail anywhere wider.
enum class HomeBarsKind {
    BOTTOM_BAR,
    RAIL,
}

// Why: the one rule every screen with the bars follows, from whether its layout has the rail.
fun homeBarsKindOf(usesRail: Boolean): HomeBarsKind = if (usesRail) HomeBarsKind.RAIL else HomeBarsKind.BOTTOM_BAR
