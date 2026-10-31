package com.quare.blitzsplit.core.groups.domain.model

// Why: what "Sair do grupo" and "Apagar grupo" end in, once the server answered.
enum class GroupExitOutcome {
    // Why: the group left this device; also when the server no longer had this person in it.
    DONE,

    // Why: the server knows of a balance still open, so nothing changed.
    HAS_BALANCE,
}
