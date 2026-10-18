package com.quare.blitzsplit.core.account.domain.usecase

fun interface IssueRecoveryCode {
    suspend operator fun invoke(): Result<String>
}
