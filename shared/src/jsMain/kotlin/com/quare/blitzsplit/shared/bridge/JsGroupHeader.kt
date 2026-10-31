@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader

class JsGroupFace(
    val initials: String,
    val color: String,
)

// Why: "Seu saldo neste grupo", amounts in the currency's minor unit, as numbers JavaScript reads exactly.
class JsMemberBalance(
    val receivesMinor: Double,
    val owesMinor: Double,
    val netMinor: Double,
    val isSettled: Boolean,
    val canSettle: Boolean,
)

// Why: "Pagamento de Ana registrado." with Desfazer; a null name is someone no longer in the group.
class JsPaymentNotice(
    val settlementId: String,
    val fromName: String?,
    val fromIsMe: Boolean,
    val toName: String?,
)

// Why: the top of the group's page, the same over Despesas and Saldos.
class JsGroupHeader(
    val groupName: String,
    val groupInitials: String,
    val groupColor: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val faces: Array<JsGroupFace>,
    val extraMemberCount: Int,
    val memberCount: Int,
    val notJoinedCount: Int,
    val totalSpentMinor: Double,
    val myBalance: JsMemberBalance?,
    val isEveryoneSettled: Boolean,
)

internal fun toJsGroupHeader(header: GroupHeader): JsGroupHeader = JsGroupHeader(
    groupName = header.groupName,
    groupInitials = header.groupInitials,
    groupColor = header.groupColor.code,
    currency = header.currency.code,
    fractionDigits = header.currency.fractionDigits,
    faces = header.faces.map(::toJsGroupFace).toTypedArray(),
    extraMemberCount = header.extraMemberCount,
    memberCount = header.memberCount,
    notJoinedCount = header.notJoinedCount,
    totalSpentMinor = header.totalSpentMinor.toDouble(),
    myBalance = header.myBalance?.let(::toJsMemberBalance),
    isEveryoneSettled = header.isEveryoneSettled,
)

internal fun toJsGroupFace(face: GroupFace): JsGroupFace = JsGroupFace(face.initials, face.color.code)

internal fun toJsMemberBalance(balance: MemberBalance): JsMemberBalance = JsMemberBalance(
    receivesMinor = balance.receivesMinor.toDouble(),
    owesMinor = balance.owesMinor.toDouble(),
    netMinor = balance.netMinor.toDouble(),
    isSettled = balance.isSettled,
    canSettle = balance.canSettle,
)

internal fun toJsPaymentNotice(notice: PaymentNotice): JsPaymentNotice = JsPaymentNotice(
    settlementId = notice.settlementId,
    fromName = notice.fromName,
    fromIsMe = notice.fromIsMe,
    toName = notice.toName,
)
