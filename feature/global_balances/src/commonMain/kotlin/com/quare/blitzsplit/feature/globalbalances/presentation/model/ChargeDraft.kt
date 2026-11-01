package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Cobrar", a message for whoever owes you, which moves no money and sends nothing by itself. Each UI writes the
 * message in its own words from these fields until it is edited, and then sends what was typed.
 */
data class ChargeDraft(
    val personId: String,
    val name: String?,
    val initials: String,
    val color: AvatarColor?,
    val currency: GroupCurrency,
    val amountMinor: Long,
    // Why: "· Viagem Floripa", and the groups the message names, the most recently active first.
    val groupNames: List<String>,
    val isGuest: Boolean,
    // Why: a guest's message says what went into the bill: "sua parte da carne e das bebidas".
    val guestBills: List<String>,
    // Why: "menos os R$ 182,50 que você já pagou", when those numbers add up to what they owe.
    val guestPaidMinor: Long?,
    // Why: the profile has a Pix key now, which may load or change while Cobrar is open.
    val hasPixKey: Boolean,
    // Why: "Incluir minha chave Pix" as the person switched it, or as it was when they edited the message; null before.
    val pixKeyChoice: Boolean?,
    // Why: null until the message is edited; from then on the UIs show and send this text.
    val editedMessage: String?,
) {
    // Why: "Incluir minha chave Pix", on by default while the profile has a Pix key.
    val includesPixKey: Boolean
        get() = pixKeyChoice ?: hasPixKey

    // Why: once the message is edited the key is the person's to type, so the switch can't rewrite their text.
    val canTogglePixKey: Boolean
        get() = editedMessage == null
}
