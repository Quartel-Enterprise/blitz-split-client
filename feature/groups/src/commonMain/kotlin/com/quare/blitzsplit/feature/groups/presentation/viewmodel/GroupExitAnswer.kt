package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.exit.GroupExitController

/*
 * Why: Sair's and Apagar's dialogs answered from a row of Grupos, counted only while the dialog can still be answered,
 * since Apple's alert also closes itself after its button.
 */
internal fun GroupExitController.answer(
    trackEvent: TrackEvent,
    isConfirmed: Boolean,
) {
    findAnswerableDialog()?.let { dialog ->
        val params = mapOf("dialog" to dialog, "source" to "groups")
        if (isConfirmed) trackEvent("group_exit_confirmed", params) else trackEvent("group_exit_cancelled", params)
    }
    if (isConfirmed) confirm() else dismiss()
}
