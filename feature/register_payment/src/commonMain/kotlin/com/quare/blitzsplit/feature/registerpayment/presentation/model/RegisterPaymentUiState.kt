package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.profile.domain.model.PixBrCode
import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull

data class RegisterPaymentUiState(
    val currency: GroupCurrency,
    // Why: bitcoin typed in whole satoshis while Preferências' "Bitcoin em satoshis" is on.
    val showsSats: Boolean,
    // Why: null until Room is read, and for someone no longer in the group ("Ex-membro").
    val from: PaymentPerson?,
    val to: PaymentPerson?,
    // Why: "Ana te deve R$ 162,30", what the pair owes now, which a payment never goes past.
    val debtMinor: Long,
    /*
     * Why: settling up with someone across groups from the global Saldos (Acertar, Recebi): what each group adds to
     * the net, one payment each; empty for a pair of one group.
     */
    val groupParts: List<PersonGroupBalance>,
    // Why: the "Valor pago" field as typed, which opens with the whole debt.
    val amountText: String,
    // Why: once typed in, the field keeps what was typed when a sync changes the debt.
    val hasEditedAmount: Boolean,
    val method: SettlementMethod,
    // Why: "Quando", date and time; each UI writes the date, after "Hoje" or "Ontem" when it is one of them.
    val paidAt: Long,
    // Why: the time the form opened with, what a "Quando" picked is compared against.
    val openedPaidAt: Long,
    // Why: the method the form opened with, Pix or the one the Pix rule starts it on; a pick is compared with it.
    val openedMethod: SettlementMethod,
    val paidDay: RelativeDay,
    // Why: a payment was made by now, so "Quando" picks nothing later; the time of the last change, which pickers cap at.
    val latestPaidAt: Long,
    val isOnline: Boolean,
    val isSaving: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    /*
     * Why: "Pagar Ana" (4.5), the receiver's keys as the last sync cached them, each method's default first, Pix
     * first; empty for a guest, someone no longer in the group, or someone with no key. Loading while none is cached
     * and the account's copy on this device is not whole yet, since only its full sync brings the members' keys; never
     * offline, when the form opens at once, since a payment is recorded offline too.
     */
    val receiverKeys: Loadable<List<ReceiverKey>>,
    /*
     * Why: none cached before the account's copy is whole (offline, or back online with it still coming down), so the
     * form can't say the receiver has no Pix key: they may well have one this device hasn't downloaded yet.
     */
    val areReceiverKeysUnknown: Boolean,
    // Why: "Apê 302 · você deve" under "Pagar Ana"; null across several groups ("3 grupos · você deve").
    val groupName: String?,
    // Why: Pix or Bitcoin, the method picked in "Forma de pagamento"; one the receiver has no key of reads as the other.
    val keyMethod: PaymentMethod,
    // Why: the Pix rule (docs/product/general.md); false hides Pix from "Como foi pago" unless it is the one picked.
    val showsPix: Boolean,
    // Why: Início's "Quem pagou a quem?" only records a payment; every Acertar offers the receiver's keys.
    val offersReceiverKeys: Boolean,
    /*
     * Why: the form stays once shown: "Paguei outro valor ou de outro jeito" picked it (a part of the debt, cash, a
     * past day), or the keys were first known with none, so a sync bringing the first one never swaps it mid-typing.
     */
    val prefersForm: Boolean,
    /*
     * Why: "Pix copia e cola" and its QR code, the default Pix key with the whole debt already in it; null off Pix,
     * and in any currency but reais, the one Pix moves.
     */
    val pixCode: String?,
    val pixQrCode: QrCode?,
    // Why: the pair is one "Simplificar dívidas" made or grew (13.5); null otherwise and across groups.
    val simplifiedNote: SimplifiedPairNote?,
    // Why: 13.5c, which takes the place of the form or "Pagar Ana" until closed or paid anyway.
    val simplifyChange: SimplifyChangedNotice?,
) {
    // Why: "Como foi pago", Pix only where the rule offers it or while it is the method already picked.
    val methods: List<SettlementMethod>
        get() = SettlementMethod.entries.filter { it != SettlementMethod.PIX || showsPix || method == it }

    val canPayAnyway: Boolean
        get() = simplifyChange != null && !isSaving

    /*
     * Why: "Pagar Ana" instead of the form while this device's user pays someone with a key: copy it, pay in the bank
     * or the wallet, and "Marcar como pago" records the whole debt.
     */
    val showsReceiverKeys: Boolean
        get() = offersReceiverKeys && !prefersForm && kind == PaymentKind.PAID && loadedKeys.isNotEmpty()

    // Why: neither "Pagar Ana" nor the form yet: the keys shimmer until the account's full sync says if there are any.
    val isLoadingReceiverKeys: Boolean
        get() = offersReceiverKeys && !prefersForm && kind == PaymentKind.PAID && receiverKeys.isLoading()

    private val loadedKeys: List<ReceiverKey>
        get() = receiverKeys.valueOrNull().orEmpty()

    // Why: the methods the receiver has a key of, Pix first; "Forma de pagamento" only shows with both.
    val keyMethods: List<PaymentMethod>
        get() = PaymentMethod.entries.filter { method -> loadedKeys.any { it.type.method == method } }

    val showsKeyMethodPicker: Boolean
        get() = keyMethods.size > 1

    val shownKeyMethod: PaymentMethod
        get() = keyMethod.takeIf { it in keyMethods } ?: keyMethods.firstOrNull() ?: keyMethod

    // Why: "Chave padrão de Ana", the one the payer sees first.
    val defaultKey: ReceiverKey?
        get() = loadedKeys.firstOrNull { it.type.method == shownKeyMethod && it.isDefault }
            ?: loadedKeys.firstOrNull { it.type.method == shownKeyMethod }

    // Why: "Outras chaves de Ana", in the order she added them.
    val otherKeys: List<ReceiverKey>
        get() = loadedKeys.filter { it.type.method == shownKeyMethod && it.id != defaultKey?.id }

    /*
     * Why: the form's Pix keys (beta feedback 3 item 26): whoever pays and picked Pix sees where to send it, the
     * default first and then in the order the receiver added them, each one copied as is. Empty off Pix, when someone
     * else pays (the receiver's own keys or a payment between others are of no use there), for someone no longer in
     * the group (whose keys this device can't know), while the keys load, and while "Pagar Ana" shows them instead.
     */
    val formPixKeys: List<ReceiverKey>
        get() = if (showsFormPixKeys) pixKeys else emptyList()

    /*
     * Why: "Ana não tem chave Pix no app." under Pix, so the payer knows to ask, also for a guest with no account. Only
     * in reais: the form opens on Pix in every currency, and a euro or dollar group would read it on every payment.
     */
    val showsNoPixKeyNotice: Boolean
        get() = showsFormPixKeys && pixKeys.isEmpty() && !areReceiverKeysUnknown && currency == GroupCurrency.BRL

    private val showsFormPixKeys: Boolean
        get() = isLoaded &&
            to != null &&
            kind == PaymentKind.PAID &&
            method == SettlementMethod.PIX &&
            receiverKeys.isLoaded() &&
            !showsReceiverKeys

    private val pixKeys: List<ReceiverKey>
        get() = loadedKeys.filter { it.type.method == PaymentMethod.PIX }.sortedByDescending { it.isDefault }

    /*
     * Why: the code pixCode should hold now, which the ViewModel encodes as a QR code once it changes; none while the
     * form is shown, so "Já pagou", "Quem pagou a quem?" and the form never encode a QR code nobody sees.
     */
    internal val expectedPixCode: String?
        get() {
            val key = defaultKey?.takeIf {
                showsReceiverKeys && shownKeyMethod == PaymentMethod.PIX && currency == GroupCurrency.BRL
            }
            return key?.let {
                PixBrCode.of(
                    key = it.value,
                    amountMinor = debtMinor,
                    receiverName = to?.name.orEmpty(),
                )
            }
        }

    val canMarkAsPaid: Boolean
        get() = isLoaded && showsReceiverKeys && !isSaving

    val kind: PaymentKind
        get() = when {
            to?.isMe == true -> PaymentKind.RECEIVED
            from?.isMe == true -> PaymentKind.PAID
            else -> PaymentKind.BETWEEN_OTHERS
        }

    val amountMinor: Long?
        get() = AmountInput.parse(amountText, currency, showsSats)

    /*
     * Why: across several groups only the whole net settles, since a part of it can't say which group it closes;
     * "Toda a dívida" and the field stay as they are.
     */
    val isAmountLocked: Boolean
        get() = groupParts.size > 1

    val isAmountValid: Boolean
        get() = amountMinor?.let { if (isAmountLocked) it == debtMinor else it in 1..debtMinor } == true

    // Why: "Ana te deve R$ 162,30. Use até esse valor."
    val showsOverDebtError: Boolean
        get() = amountMinor?.let { it > debtMinor } == true

    // Why: "Ana ainda vai te dever R$ 62,30" under a partial payment.
    val remainingMinor: Long?
        get() = amountMinor?.takeIf { isAmountValid && it < debtMinor }?.let { debtMinor - it }

    // Why: "Toda a dívida" is marked while the amount is the whole debt.
    val isFullDebt: Boolean
        get() = amountMinor == debtMinor

    // Why: "Sem internet: fica salvo no aparelho e sincroniza ao reconectar."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    /*
     * Why: closing asks "Discard changes?" only when the form holds something the person changed: an amount other
     * than the whole debt, a method other than the opened one, or another "Quando". Untouched, or put back, it closes silently.
     */
    val hasUnsavedInput: Boolean
        get() = isLoaded &&
            ((hasEditedAmount && amountMinor != debtMinor) || method != openedMethod || paidAt != openedPaidAt)

    val canConfirm: Boolean
        get() = isLoaded && isAmountValid && !isSaving && !isLoadingReceiverKeys
}
