package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentTarget
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey

// Why: a target with the keys Room gave for userId's account, read with whether the account's copy is whole yet.
internal data class KeyedTarget(
    val target: PaymentTarget?,
    val userId: String?,
    val keys: List<ReceiverKey>,
    val download: FirstDownloadProgress?,
)
