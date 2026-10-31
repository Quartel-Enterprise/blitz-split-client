package com.quare.blitzsplit.core.groups.domain.usecase

fun interface ConsumeGroupDeletion {
    // Why: "Grupo apagado" was shown, so no other screen shows it again.
    operator fun invoke(groupId: String)
}
