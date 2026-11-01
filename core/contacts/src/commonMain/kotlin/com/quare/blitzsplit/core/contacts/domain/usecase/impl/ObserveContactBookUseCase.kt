package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.model.ContactBook
import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.expenses.domain.model.GroupActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// Why: the groups, their money and the hidden list from one read of each, so the list and its nets always agree.
class ObserveContactBookUseCase(
    private val observeGroups: ObserveGroups,
    private val observeGroupsMoney: ObserveGroupsMoney,
    private val hiddenContacts: HiddenContactRepository,
) : ObserveContactBook {
    override fun invoke(): Flow<ContactBookSnapshot> = combine(
        observeGroups(),
        observeGroupsMoney(),
        hiddenContacts.observeHidden(),
    ) { groups, money, hidden ->
        val byActivity = GroupActivity.sortMostRecentFirst(groups, money)
        ContactBookSnapshot(
            contacts = ContactBook.of(byActivity, money, hidden),
            groups = byActivity,
            money = money,
        )
    }.distinctUntilChanged()
}
