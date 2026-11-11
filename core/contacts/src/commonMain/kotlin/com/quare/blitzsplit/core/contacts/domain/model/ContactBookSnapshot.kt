package com.quare.blitzsplit.core.contacts.domain.model

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: Contatos and the profile from one read of the groups, their money and the hidden list.
data class ContactBookSnapshot(
    // Why: A to Z, hidden ones included (Contact.isHidden).
    val contacts: List<Contact>,
    // Why: the device user's groups, the most recently active first, as the home lists them.
    val groups: List<Group>,
    val money: Map<String, GroupMoney>,
) {
    fun findOrNull(key: String): Contact? = contacts.firstOrNull { it.key == key }

    // Why: the categories only the profile's history needs (its bills' badges), read by the profile itself.
    fun historyOf(
        contact: Contact,
        categories: CategoryTree,
    ): List<ContactHistoryItem> = ContactHistory.of(contact, groups, money, categories)
}
