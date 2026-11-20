package com.quare.blitzsplit.shared.app

import com.quare.blitzsplit.core.inappupdate.domain.usecase.RequestUpdatePromptIfNeeded
import com.quare.blitzsplit.core.support.domain.usecase.ObserveReviewMoments
import com.quare.blitzsplit.core.support.domain.usecase.RequestReviewIfNeeded

/*
 * Why: the use cases for what the app opens on its own with the store's help, an update's offer and the review card,
 * as one dependency of AppViewModel, whose constructor is at the size Koin's factoryOf takes.
 */
data class StorePromptUseCases(
    val requestUpdatePromptIfNeeded: RequestUpdatePromptIfNeeded,
    val observeReviewMoments: ObserveReviewMoments,
    val requestReviewIfNeeded: RequestReviewIfNeeded,
)
