package com.quare.blitzsplit.core.groups.domain.usecase

// Why: "Fixar no Início" and "Desafixar do Início", saved on this device and synced later.
fun interface SetGroupPinned {
    suspend operator fun invoke(
        groupId: String,
        isPinned: Boolean,
    ): Result<Unit>
}
