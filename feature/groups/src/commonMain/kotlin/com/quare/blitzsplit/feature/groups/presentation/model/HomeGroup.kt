package com.quare.blitzsplit.feature.groups.presentation.model

// Why: a card of Início's "Seus grupos": the group's row as Grupos has it, and whether "Fixar na Início" pinned it.
data class HomeGroup(
    val row: GroupRow,
    val isPinned: Boolean,
)
