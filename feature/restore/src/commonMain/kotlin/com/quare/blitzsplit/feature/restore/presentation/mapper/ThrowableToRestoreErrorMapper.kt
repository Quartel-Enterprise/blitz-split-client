package com.quare.blitzsplit.feature.restore.presentation.mapper

import com.quare.blitzsplit.core.account.domain.model.InvalidRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.model.TooManyRecoveryAttemptsException
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreError

fun Throwable.toRestoreError(): RestoreError = when (this) {
    is InvalidRecoveryCodeException -> RestoreError.INVALID_CODE
    is TooManyRecoveryAttemptsException -> RestoreError.TOO_MANY_ATTEMPTS
    else -> RestoreError.UNKNOWN
}
