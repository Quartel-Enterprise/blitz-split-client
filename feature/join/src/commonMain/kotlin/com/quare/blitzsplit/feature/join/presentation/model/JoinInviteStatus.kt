package com.quare.blitzsplit.feature.join.presentation.model

enum class JoinInviteStatus {
    LOADING,

    // Why: "Pierre te chamou para a Viagem Floripa", with "Entrar no grupo".
    READY,

    // Why: "Este convite não vale mais": the link was changed or the group deleted.
    INVALID,

    /*
     * Why: "Sem internet para abrir o convite". The link stays on screen and the invite opens by itself once the
     * device is online; a server that can't be reached reads the same, with "Tentar de novo".
     */
    OFFLINE,
}
