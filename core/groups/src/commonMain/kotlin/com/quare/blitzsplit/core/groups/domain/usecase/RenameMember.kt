package com.quare.blitzsplit.core.groups.domain.usecase

fun interface RenameMember {
    suspend operator fun invoke(
        memberId: String,
        name: String,
    ): Result<Unit>
}
