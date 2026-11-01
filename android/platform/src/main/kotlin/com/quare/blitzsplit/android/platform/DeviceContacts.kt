package com.quare.blitzsplit.android.platform

import android.content.Context
import android.provider.ContactsContract

// Why: someone of the device's agenda, as ContactsContract has them; nothing of it leaves the device.
data class DeviceContact(
    val id: String,
    val name: String,
    // Why: the first number, as the agenda writes it; null for a contact without one.
    val phone: String?,
)

/*
 * Why: "Convidar da agenda" lists everyone with a name, A to Z as the agenda sorts them, each with their first number.
 * It reads the provider, so it runs off the main thread, and only once the permission is granted.
 */
fun Context.readDeviceContacts(): List<DeviceContact> {
    val phones = readFirstPhones()
    val contacts = mutableListOf<DeviceContact>()
    contentResolver
        .query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)?.trim()
                if (name.isNullOrEmpty()) continue
                val id = cursor.getLong(idColumn)
                contacts += DeviceContact(
                    id = id.toString(),
                    name = name,
                    phone = phones[id],
                )
            }
        }
    return contacts
}

private fun Context.readFirstPhones(): Map<Long, String> {
    val phones = mutableMapOf<Long, String>()
    contentResolver
        .query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC",
        )?.use { cursor ->
            val contactColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberColumn)?.trim()
                if (number.isNullOrEmpty()) continue
                phones.putIfAbsent(cursor.getLong(contactColumn), number)
            }
        }
    return phones
}
