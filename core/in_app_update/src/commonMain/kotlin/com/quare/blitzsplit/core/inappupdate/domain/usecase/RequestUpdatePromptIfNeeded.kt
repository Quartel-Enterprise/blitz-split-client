package com.quare.blitzsplit.core.inappupdate.domain.usecase

fun interface RequestUpdatePromptIfNeeded {
    suspend operator fun invoke()
}
