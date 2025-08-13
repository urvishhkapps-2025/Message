package com.hkapps.messagepro.extensions

import android.text.TextUtils
import com.hkapps.messagepro.model.ContactsModel
import java.util.*

fun <T> ArrayList<T>.moveLastItemToFront() {
    val last = removeAt(size - 1)
    add(0, last)
}
fun ArrayList<ContactsModel>.getThreadTitle() = TextUtils.join(", ", map { it.name }.toTypedArray())
fun ArrayList<ContactsModel>.getSubTitle() = TextUtils.join(", ", map { it.phoneNumbers }.toTypedArray()).replace("[", "")
    .replace("]", "")
    .replace(" ", "")
