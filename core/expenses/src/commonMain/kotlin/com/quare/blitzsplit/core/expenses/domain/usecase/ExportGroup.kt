package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.export.GroupExportFile
import com.quare.blitzsplit.core.expenses.export.GroupExportFormat
import com.quare.blitzsplit.core.expenses.export.GroupExportWords

fun interface ExportGroup {
    /*
     * Why: "Export group data": the group's bills, payments, members and balances as they are on this device, offline
     * included, in the format picked and with the UI's words. Fails only when the group is no longer on the device.
     */
    suspend operator fun invoke(
        groupId: String,
        format: GroupExportFormat,
        words: GroupExportWords,
    ): Result<GroupExportFile>
}
