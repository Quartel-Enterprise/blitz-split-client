package com.quare.blitzsplit.feature.contacts.presentation.model

// Why: the line under the profile's net, the same on every platform.
enum class ContactBalanceNote {
    // Why: "Nada pendente em nenhum grupo": every group in common is settled.
    NOTHING_OPEN,

    // Why: "Já compensado entre 3 grupos": the net adds up what is open in several groups.
    OFFSET_ACROSS_GROUPS,

    // Why: the one group in common, by name.
    ONE_GROUP,
}
