package com.quare.blitzsplit.core.activity.domain.usecase

fun interface LoadOlderActivity {
    suspend operator fun invoke(): Result<Unit>
}
