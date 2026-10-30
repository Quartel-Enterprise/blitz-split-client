package com.quare.blitzsplit.core.expenses.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/*
 * Why: "Esta conta foi apagada" on Despesas do grupo, after someone else deleted a bill that was open here and its
 * screen closed. Kept until that group's screen shows it, which on the web is drawn again only once it is on top.
 */
class DeletedBillNotice {
    // Why: the group whose screen owes the notice, null when none does.
    val groupId: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    fun post(groupId: String) {
        this.groupId.value = groupId
    }

    // Why: only that group's own notice, so another group's screen shown meanwhile leaves it waiting.
    fun consume(groupId: String) {
        this.groupId.update { if (it == groupId) null else it }
    }
}
