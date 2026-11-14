package com.quare.blitzsplit.core.releasenotes.domain.usecase

fun interface RefreshReleaseNotes {
    suspend operator fun invoke(language: String): Result<Unit>
}
