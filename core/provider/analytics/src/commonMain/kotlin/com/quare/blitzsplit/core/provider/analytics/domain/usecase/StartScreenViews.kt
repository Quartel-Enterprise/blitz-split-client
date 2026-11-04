package com.quare.blitzsplit.core.provider.analytics.domain.usecase

/*
 * Why: a screen_view each time the back stack shows another screen on top, from the shared back stack, so every UI
 * counts the same screens. Started once the first screen is drawn, so the placeholder before it never counts.
 */
fun interface StartScreenViews {
    operator fun invoke()
}
