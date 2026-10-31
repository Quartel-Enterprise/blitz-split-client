package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.model.GroupCurrency

data class MembersUiState(
    val groupName: String,
    val currency: GroupCurrency,
    // Why: "blitzsplit.com/g/7Hk2pQ", null until the group's first sync brings the code the server made.
    val displayLink: String?,
    // Why: in the order they were added, the creator first.
    val members: List<MembersMember>,
    val editor: MemberNameEditor?,
    // Why: "Remover Carla do grupo?", asked while not null; always someone who can be removed.
    val memberToRemove: MembersMember?,
    // Why: "Liberar o nome Carla?", asked while not null; always someone who can be released.
    val memberToRelease: MembersMember?,
    // Why: the release goes to the server, so "Liberar" waits for it with the confirmation open.
    val isReleasing: Boolean,
    // Why: the "Convite pessoal" sheet, open while not null; always someone nobody joined as, with a link.
    val personalInvite: PersonalInvite?,
    // Why: "Seu saldo neste grupo" over the tabs, null while there is no bill to owe anything for.
    val myBalance: MemberBalance?,
    // Why: "Ninguém deve nada a ninguém aqui." rather than "Outros ainda acertam entre si." on a settled card.
    val isEveryoneSettled: Boolean,
    // Why: "Pagamento de Ana registrado." with Desfazer, once Registrar pagamento closed over this screen.
    val recordedPayment: PaymentNotice?,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    // Why: "Você entrou como Bruno", right after joining the group through its link.
    val joinedAs: String?,
    // Why: "Proteja seu acesso" under it, while the account has no Google or Apple.
    val isAccountUnprotected: Boolean,
) {
    val showsProtectAccess: Boolean
        get() = joinedAs != null && isAccountUnprotected

    // Why: "4 membros · 2 já entraram".
    val memberCount: Int
        get() = members.size

    val joinedCount: Int
        get() = members.count { it.hasJoined }
}
