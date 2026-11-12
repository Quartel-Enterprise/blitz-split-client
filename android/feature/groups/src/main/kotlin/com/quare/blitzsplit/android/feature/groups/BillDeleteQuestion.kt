package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent

// Why: "Apagar esta conta?" from a bill's ⋮ on Início or "Todas as despesas", as the group's Despesas asks it.
@Composable
internal fun BillDeleteQuestion(
    deletion: BillDeletion?,
    isDeleting: Boolean,
    onEvent: (BillMenuEvent) -> Unit,
) {
    deletion ?: return
    DeleteBillDialog(
        description = deletion.description,
        myBalanceMinor = deletion.myBalanceMinor,
        settledPair = deletion.settledPair,
        isDeleting = isDeleting,
        currency = deletion.currency,
        onConfirm = { onEvent(BillMenuEvent.OnDeleteConfirmed) },
        onDismiss = { onEvent(BillMenuEvent.OnDeleteDismissed) },
    )
}
