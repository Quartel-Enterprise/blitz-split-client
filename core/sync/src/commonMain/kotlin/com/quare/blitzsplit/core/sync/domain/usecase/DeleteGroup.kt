package com.quare.blitzsplit.core.sync.domain.usecase

fun interface DeleteGroup {
    /*
     * Why: "Apagar grupo", for everyone and online only. The group leaves every screen at once and the server is asked
     * in the background, after the device pushed what it has, so it judges the balances with every bill and payment
     * made here. A refusal or a failure brings the group back and posts why (GroupDeletion.failure), which Grupos or
     * Início, whichever is on top first, says.
     */
    operator fun invoke(
        groupId: String,
        groupName: String,
    )
}
