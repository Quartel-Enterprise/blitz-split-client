package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: the notice of 13.1e as a group has it, naming whoever changed it after this device by their member's name.
object SimplifyDebtsConflicts {
    fun noticeOrNull(group: Group): SimplifyDebtsConflictNotice? = group.simplifyDebtsConflictBy?.let { userId ->
        SimplifyDebtsConflictNotice(
            changedByName = group.members.firstOrNull { it.userId == userId }?.name,
            simplifiesDebts = group.simplifiesDebts,
        )
    }
}
