package com.quare.blitzsplit.feature.join.presentation.model

enum class JoinInviteStatus {
    LOADING,

    // Why: "Pierre te chamou para a Viagem Floripa", with "Entrar no grupo"; "Pierre te chamou como Carla" when personal.
    READY,

    /*
     * Why: "Este convite não vale mais": the link was changed or the group deleted. A personal link also once its
     * member was removed, or someone joined as them.
     */
    INVALID,

    /*
     * Why: "Sem internet para abrir o convite", only while the device says it is offline. The link stays on screen
     * and the invite opens by itself once the device is online.
     */
    OFFLINE,

    // Why: "Não deu para abrir o convite. Tente de novo.": online, the server failed, with "Tentar de novo".
    FAILED,
}
