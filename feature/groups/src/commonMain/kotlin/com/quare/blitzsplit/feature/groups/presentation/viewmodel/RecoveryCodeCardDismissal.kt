package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow

/*
 * Why: ✕ on Início's "Guarde seu código de recuperação" hides it until the next launch, also where Início's ViewModel
 * goes with the screen (the web's tabs), so it lives as long as the app (a Koin single), in memory.
 */
class RecoveryCodeCardDismissal {
    val isDismissed = MutableStateFlow(false)
}
