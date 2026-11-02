package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillLinkResultDto
import com.quare.blitzsplit.core.expenses.data.dto.BillShareLinkDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillStatusDto
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment

interface SharedBillMapper {
    // Why: the link's visibility is null for one this version doesn't know.
    fun toDomain(dto: BillShareLinkDto): BillShareLink

    // Why: null for "not_found", the bill gone or not in the caller's groups.
    fun toDomain(dto: BillLinkResultDto): BillShareLink?

    // Why: imageUrls by the attachments' paths, which the server's answer carries.
    fun toDomain(
        dto: SharedBillDto,
        imageUrls: Map<String, String>,
    ): SharedBill

    fun toDomain(dto: SharedBillStatusDto): SharedBillComment
}
