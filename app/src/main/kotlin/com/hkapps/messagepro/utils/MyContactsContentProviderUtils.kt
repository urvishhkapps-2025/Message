package com.hkapps.messagepro.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.hkapps.messagepro.extensions.getIntValue
import com.hkapps.messagepro.extensions.getStringValue
import com.hkapps.messagepro.model.ContactsModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// used for sharing privately stored contacts in Simple Contacts with Simple Dialer, Simple SMS Messenger and Simple Calendar Pro
class MyContactsContentProviderUtils {
    companion object {
        private const val AUTHORITY = "com.hkapps.messagepro.contacts.provider"
        val CONTACTS_CONTENT_URI = Uri.parse("content://$AUTHORITY/contacts")
        const val COL_RAW_ID = "raw_id"
        const val COL_CONTACT_ID = "contact_id"
        const val COL_NAME = "name"
        const val COL_PHOTO_URI = "photo_uri"
        const val COL_PHONE_NUMBERS = "phone_numbers"
        const val COL_BIRTHDAYS = "birthdays"
        const val COL_ANNIVERSARIES = "anniversaries"

        fun getSimpleContacts(context: Context, cursor: Cursor?): ArrayList<ContactsModel> {
            val contacts = ArrayList<ContactsModel>()
            val packageName = context.packageName.removeSuffix(".debug")
            if (packageName != "com.myapp.dialer" && packageName != "com.hkapps.messagepro" && packageName != "com.myapp.calendar.pro") {
                return contacts
            }

            try {
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        do {
                            val rawId = cursor.getIntValue(COL_RAW_ID)
                            val contactId = cursor.getIntValue(COL_CONTACT_ID)
                            val name = cursor.getStringValue(COL_NAME)
                            val photoUri = cursor.getStringValue(COL_PHOTO_URI)
                            val phoneNumbersJson = cursor.getStringValue(COL_PHONE_NUMBERS)
                            val birthdaysJson = cursor.getStringValue(COL_BIRTHDAYS)
                            val anniversariesJson = cursor.getStringValue(COL_ANNIVERSARIES)

                            val token = object : TypeToken<ArrayList<String>>() {}.type
                            val phoneNumbers = Gson().fromJson<ArrayList<String>>(phoneNumbersJson, token) ?: ArrayList()
                            val birthdays = Gson().fromJson<ArrayList<String>>(birthdaysJson, token) ?: ArrayList()
                            val anniversaries = Gson().fromJson<ArrayList<String>>(anniversariesJson, token) ?: ArrayList()

                            val contact = ContactsModel(rawId, contactId, name, photoUri, phoneNumbers, birthdays, anniversaries)
                            contacts.add(contact)
                        } while (cursor.moveToNext())
                    }
                }
            } catch (ignored: Exception) {
            }
            return contacts
        }
    }
}
