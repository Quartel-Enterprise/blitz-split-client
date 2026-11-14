package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.export.GroupExportFile

sealed interface GroupDetailUiAction {
    // Why: "Export group data" made its file, which the UI hands to the system to save or share.
    class SaveExport(
        val file: GroupExportFile,
    ) : GroupDetailUiAction

    // Why: the group left the device while the file was being made, which is rare enough to just say so.
    data object ShowExportError : GroupDetailUiAction

    // Why: the device couldn't sync or reach the server, so nobody left; trying again may work.
    data object ShowLeaveError : GroupDetailUiAction

    // Why: the device couldn't delete the payment, which is rare enough to just say so; it still counts.
    data object ShowChangeError : GroupDetailUiAction

    // Why: a bill deleted from its ⋮ on the list couldn't be, which is rare enough to just say so; it is still there.
    data object ShowBillDeleteError : GroupDetailUiAction

    // Why: the word couldn't go into the map; the bill is saved anyway.
    data object ShowTeachError : GroupDetailUiAction
}
