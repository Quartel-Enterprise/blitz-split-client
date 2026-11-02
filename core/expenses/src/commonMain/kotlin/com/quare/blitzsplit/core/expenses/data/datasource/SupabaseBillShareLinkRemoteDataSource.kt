package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillLinkResultDto
import com.quare.blitzsplit.core.expenses.data.dto.BillShareLinkDto
import com.quare.blitzsplit.core.expenses.data.dto.CommentOnSharedBillParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseIdParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.SetBillLinkParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillCodeParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillStatusDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc

class SupabaseBillShareLinkRemoteDataSource(
    private val postgrest: Postgrest,
) : BillShareLinkRemoteDataSource {
    override suspend fun fetchLink(expenseId: String): BillShareLinkDto? = postgrest
        .from(BILL_SHARE_LINKS)
        .select(Columns.list("code", "visibility")) {
            filter { eq("expense_id", expenseId) }
        }.decodeSingleOrNull<BillShareLinkDto>()

    override suspend fun updateLink(
        expenseId: String,
        visibility: String,
    ): BillLinkResultDto = postgrest
        .rpc(
            function = SET_BILL_LINK,
            parameters = SetBillLinkParamsDto(expenseId, visibility),
        ).decodeAs<BillLinkResultDto>()

    override suspend fun disableLink(expenseId: String): BillLinkResultDto = postgrest
        .rpc(
            function = DISABLE_BILL_LINK,
            parameters = ExpenseIdParamsDto(expenseId),
        ).decodeAs<BillLinkResultDto>()

    override suspend fun fetchSharedBill(code: String): SharedBillDto = postgrest
        .rpc(
            function = GET_SHARED_BILL,
            parameters = SharedBillCodeParamsDto(code),
        ).decodeAs<SharedBillDto>()

    override suspend fun createSharedBillComment(params: CommentOnSharedBillParamsDto): SharedBillStatusDto = postgrest
        .rpc(
            function = COMMENT_ON_SHARED_BILL,
            parameters = params,
        ).decodeAs<SharedBillStatusDto>()

    private companion object {
        const val BILL_SHARE_LINKS = "bill_share_links"
        const val SET_BILL_LINK = "set_bill_link"
        const val DISABLE_BILL_LINK = "disable_bill_link"
        const val GET_SHARED_BILL = "get_shared_bill"
        const val COMMENT_ON_SHARED_BILL = "comment_on_shared_bill"
    }
}
