package com.quare.blitzsplit.android.feature.contacts

import android.content.Context
import com.quare.blitzsplit.android.platform.ContactsPermissionStatus
import com.quare.blitzsplit.android.platform.readDeviceContacts
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaAccess
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Why: the agenda as the shared code takes it, read off the main thread. Empty when the provider fails or the access
 * was taken away while reading, which the next read on coming back to the app corrects.
 */
internal suspend fun Context.readAgenda(): List<AgendaContact> = withContext(Dispatchers.IO) {
    runCatching { readDeviceContacts() }
        .getOrDefault(emptyList())
        .map { contact ->
            AgendaContact(
                id = contact.id,
                name = contact.name,
                phone = contact.phone,
            )
        }
}

internal fun agendaAccessOf(status: ContactsPermissionStatus): AgendaAccess = when (status) {
    ContactsPermissionStatus.GRANTED -> AgendaAccess.GRANTED
    ContactsPermissionStatus.NOT_ASKED -> AgendaAccess.NOT_ASKED
    ContactsPermissionStatus.DENIED -> AgendaAccess.DENIED
}
