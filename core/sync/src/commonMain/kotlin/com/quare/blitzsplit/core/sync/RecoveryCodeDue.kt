package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.utils.RecoveryWords

/*
 * Why: an anonymous account gets its code after its first sync. A code kept from before the words (12 letters and
 * digits) no longer restores and isn't shown, so it is replaced by a new one, which shows the sheet again.
 */
internal fun AccountSnapshot.isRecoveryCodeDue(): Boolean {
    if (status !is AccountStatus.Anonymous) return false
    val code = recoveryCode
    val hasCodeFromBefore = code != null && RecoveryWords.wordsOf(code) == null
    return recoveryCodePrompt == RecoveryCodePrompt.NOT_DUE || hasCodeFromBefore
}
