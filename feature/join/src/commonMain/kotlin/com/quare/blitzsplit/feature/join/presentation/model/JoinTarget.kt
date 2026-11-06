package com.quare.blitzsplit.feature.join.presentation.model

// Why: who a join on its way joins as, whose button shows the progress.
enum class JoinTarget {
    // Why: "Entrar como Bruno", a member picked from the list.
    MEMBER,

    // Why: a new member, from "Entrar com outro nome" or, for a Google or Apple account, from "Não estou na lista".
    NEW_MEMBER,
}
