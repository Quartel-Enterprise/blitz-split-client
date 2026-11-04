package com.quare.blitzsplit.core.inappupdate.domain.usecase

fun interface InstallDownloadedUpdate {
    suspend operator fun invoke()
}
