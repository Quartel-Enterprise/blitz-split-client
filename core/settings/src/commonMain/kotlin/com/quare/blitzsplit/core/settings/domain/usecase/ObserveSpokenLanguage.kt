package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

/*
 * Why: the language the app is speaking, as its code alone ("pt", "tr"): Profile › Language's pick, or, while it
 * follows the system, the first of the device's languages the app has; English otherwise, as the app's own strings fall back. For
 * what leaves the app saying which language it was in, a shared link's preview.
 */
fun interface ObserveSpokenLanguage {
    operator fun invoke(): Flow<String>
}
