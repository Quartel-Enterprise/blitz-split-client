package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillLinkResultDto
import com.quare.blitzsplit.core.expenses.data.dto.BillShareLinkDto
import com.quare.blitzsplit.core.expenses.data.dto.CommentOnSharedBillParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillStatusDto

// Why: a bill's link lives only on the server, written through its RPCs and never synced, so it needs the internet.
interface BillShareLinkRemoteDataSource {
    // Why: null while the bill has no link.
    suspend fun fetchLink(expenseId: String): BillShareLinkDto?

    suspend fun updateLink(
        expenseId: String,
        visibility: String,
    ): BillLinkResultDto

    suspend fun disableLink(expenseId: String): BillLinkResultDto

    suspend fun fetchSharedBill(code: String): SharedBillDto

    suspend fun createSharedBillComment(params: CommentOnSharedBillParamsDto): SharedBillStatusDto
}
