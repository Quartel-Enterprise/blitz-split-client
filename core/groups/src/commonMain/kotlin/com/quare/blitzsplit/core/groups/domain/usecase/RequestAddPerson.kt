package com.quare.blitzsplit.core.groups.domain.usecase

fun interface RequestAddPerson {
    // Why: the group's Members opens "Add person" as soon as it is on top.
    operator fun invoke(groupId: String)
}
