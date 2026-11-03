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
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull

data class RegisterPaymentUiState(
    val currency: GroupCurrency,
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
     * and the account's copy on this device is not whole yet, since only its full sync brings the members' keys.
     */
    val receiverKeys: Loadable<List<ReceiverKey>>,
    // Why: "Sem internet. Seus dados aparecem quando reconectar." under the keys' shimmer while that sync waits offline.
    val isWaitingForInternet: Boolean,
    // Why: "Apê 302 · você deve" under "Pagar Ana"; null across several groups ("3 grupos · você deve").
    val groupName: String?,
    // Why: Pix or Bitcoin, the method picked in "Forma de pagamento"; one the receiver has no key of reads as the other.
    val keyMethod: PaymentMethod,
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
) {
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
        get() = AmountInput.parse(amountText, currency)

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

    val canConfirm: Boolean
        get() = isLoaded && isAmountValid && !isSaving && !isLoadingReceiverKeys
}
