package com.quare.blitzsplit.feature.contacts.presentation.model

sealed interface ContactProfileUiAction {
    // Why: what isn't built yet: Atividade (page 12) and "Pedir para quem já usa o app".
    data object ShowComingSoon : ContactProfileUiAction

    // Why: "Copiado".
    data class CopyLink(
        val link: String,
    ) : ContactProfileUiAction

    // Why: the system share sheet with the personal link; the browser copies it when it has none.
    data class ShareLink(
        val link: String,
        val name: String,
    ) : ContactProfileUiAction

    // Why: "Carla entrou no Futebol de quinta."
    data class ShowAddedToGroup(
        val name: String,
        val groupName: String,
    ) : ContactProfileUiAction

    // Why: the device couldn't save a change, which is rare enough to just say so.
    data object ShowChangeError : ContactProfileUiAction

    // Why: Cancelar didn't reach the server.
    data object ShowCancelMergeError : ContactProfileUiAction
}
