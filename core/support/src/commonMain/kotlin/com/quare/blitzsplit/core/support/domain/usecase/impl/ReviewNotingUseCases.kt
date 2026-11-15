package com.quare.blitzsplit.core.support.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.usecase.NoteReviewMoment

/*
 * Why: the forms that save a bill or a payment note what went well without knowing a review exists: each of these is
 * the use case the form already calls, which also notes the moment once it succeeds. Only the forms take them, so a
 * bill or a payment that comes through a sync is no moment.
 */
fun CreateExpense.toReviewNoting(note: NoteReviewMoment): CreateExpense = CreateExpense {
    groupId,
    description,
    amountMinor,
    currency,
    payers,
    splitMode,
    shares,
    items,
    categoryId,
    spentAt,
    ->
    this(groupId, description, amountMinor, currency, payers, splitMode, shares, items, categoryId, spentAt)
        .onSuccess { note(ReviewMoment.BillCreated) }
}

fun RecordSettlement.toReviewNoting(note: NoteReviewMoment): RecordSettlement = RecordSettlement {
    groupId,
    fromMemberId,
    toMemberId,
    amountMinor,
    currency,
    method,
    paidAt,
    ->
    this(groupId, fromMemberId, toMemberId, amountMinor, currency, method, paidAt)
        .onSuccess { note(ReviewMoment.PaymentRecorded(listOf(groupId))) }
}

fun SettleUp.toReviewNoting(note: NoteReviewMoment): SettleUp = SettleUp {
    legs,
    method,
    paidAt,
    personName,
    isPaidByMe,
    ->
    this(legs, method, paidAt, personName, isPaidByMe)
        .onSuccess { note(ReviewMoment.PaymentRecorded(legs.map { it.groupId }.distinct())) }
}
