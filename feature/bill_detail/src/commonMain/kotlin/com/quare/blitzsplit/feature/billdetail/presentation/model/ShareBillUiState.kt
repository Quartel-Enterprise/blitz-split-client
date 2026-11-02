package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Compartilhar conta" (7.3): the bill's link, made with "Só quem está na conta" the first time it opens, who may
 * open it, "Copiar", "Enviar link" and "Desativar link". The server keeps the link, so it needs the internet.
 */
data class ShareBillUiState(
    // Why: "Jantar no Ostradamus · R$ 642,00" under the title.
    val description: String,
    val totalMinor: Long,
    val currency: GroupCurrency,
    // Why: "Ana, Bruno, Carla, Elisa e Diego entram com a própria conta…": who joined, this device's user aside.
    val memberNames: List<String>,
    // Why: null until it is read or made.
    val link: String?,
    // Why: null, no option picked, for a way a newer app added.
    val visibility: BillLinkVisibility?,
    val isOnline: Boolean,
    // Why: reading the link, or making it the first time.
    val isLoading: Boolean,
    // Why: changing who may open it, or turning it off.
    val isUpdating: Boolean,
    // Why: the link couldn't be read or made: "Tentar de novo".
    val hasFailed: Boolean,
    val isLinkCopied: Boolean,
) {
    // Why: "blitzsplit.com/c/7Hk2pQ8x".
    val displayLink: String?
        get() = link?.let(SharedBillLink::displayOf)

    // Why: "Compartilhar precisa de internet."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: "Copiar" and "Enviar link" work offline too, once the link is known.
    val canSendLink: Boolean
        get() = link != null

    // Why: who may open it and "Desativar link" change the server's link.
    val canChangeLink: Boolean
        get() = isOnline && link != null && !isUpdating
}
