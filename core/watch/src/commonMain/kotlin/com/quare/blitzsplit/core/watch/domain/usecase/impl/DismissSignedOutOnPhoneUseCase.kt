package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.watch.domain.model.WatchSignOutNotice
import com.quare.blitzsplit.core.watch.domain.usecase.DismissSignedOutOnPhone

class DismissSignedOutOnPhoneUseCase(
    private val notice: WatchSignOutNotice,
) : DismissSignedOutOnPhone {
    override fun invoke() {
        notice.dismiss()
    }
}
