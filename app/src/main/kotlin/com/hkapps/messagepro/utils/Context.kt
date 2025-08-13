package com.hkapps.messagepro.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.PhoneLookup
import android.provider.OpenableColumns
import android.provider.Telephony.*
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.MessagesActivity
import com.hkapps.messagepro.services.MarkingAsReadService
import com.hkapps.messagepro.services.ReplySMSService
import com.hkapps.messagepro.listners.ConversationsRoomDaoListner
import com.hkapps.messagepro.listners.DaoListner
import com.hkapps.messagepro.model.*
import me.leolin.shortcutbadger.ShortcutBadger
import java.io.FileNotFoundException
import java.util.regex.Pattern


val Context.config: AppConfigNew get() = AppConfigNew.newInstance(applicationContext)

fun Context.getMessagessDB() = DatabaseMessages.getInstance(this)

val Context.conversationsDB: ConversationsRoomDaoListner get() = getMessagessDB().ConversationsDao()

val Context.messagesDB: DaoListner get() = getMessagessDB().MessagesDao()

fun Context.getMessages(threadId: Long): ArrayList<MessagesModel> {
    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms._ID,
        Sms.BODY,
        Sms.TYPE,
        Sms.ADDRESS,
        Sms.DATE,
        Sms.READ,
        Sms.THREAD_ID,
        Sms.SUBSCRIPTION_ID,
        Sms.STATUS
    )

    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    val sortOrder = "${Sms._ID} DESC LIMIT 100"

    val blockStatus = HashMap<String, Boolean>()
    val blockedNumbers = getBlockedNumbers()
    var messages = ArrayList<MessagesModel>()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, showErrors = true) { cursor ->
        val senderNumber = cursor.getStringValue(Sms.ADDRESS) ?: return@queryCursor

        val isNumberBlocked = if (blockStatus.containsKey(senderNumber)) {
            blockStatus[senderNumber]!!
        } else {
            val isBlocked = isNumberBlocked(senderNumber, blockedNumbers)
            blockStatus[senderNumber] = isBlocked
            isBlocked
        }

        if (isNumberBlocked) {
            return@queryCursor
        }

        val id = cursor.getLongValue(Sms._ID)
        val body = cursor.getStringValue(Sms.BODY)
        val type = cursor.getIntValue(Sms.TYPE)
        val namePhoto = getNameAndPhotoFromPhoneNumber(senderNumber)
        val senderName = namePhoto.name
        val photoUri = namePhoto.photoUri ?: ""
        val date = (cursor.getLongValue(Sms.DATE) / 1000).toInt()
        val read = cursor.getIntValue(Sms.READ) == 1
        val thread = cursor.getLongValue(Sms.THREAD_ID)
        val subscriptionId = cursor.getIntValue(Sms.SUBSCRIPTION_ID)
        val status = cursor.getIntValue(Sms.STATUS)
        val participant = ContactsModel(0, 0, senderName, photoUri, arrayListOf(senderNumber), ArrayList(), ArrayList())
        val isMMS = false
        val message = MessagesModel(id, body, type, status, arrayListOf(participant), date, read, thread, isMMS, null, senderName, photoUri, subscriptionId)
        messages.add(message)
    }

    messages.addAll(getMMS(threadId, sortOrder))
    messages = messages.filter { it.participants.isNotEmpty() }
        .sortedWith(compareBy<MessagesModel> { it.date }.thenBy { it.id }).toMutableList() as ArrayList<MessagesModel>

    return messages
}

// as soon as a message contains multiple recipients it counts as an MMS instead of SMS
fun Context.getMMS(threadId: Long? = null, sortOrder: String? = null): ArrayList<MessagesModel> {
    val uri = Mms.CONTENT_URI
    val projection = arrayOf(
        Mms._ID,
        Mms.DATE,
        Mms.READ,
        Mms.MESSAGE_BOX,
        Mms.THREAD_ID,
        Mms.SUBSCRIPTION_ID,
        Mms.STATUS
    )

    val selection = if (threadId == null) {
        "1 == 1) GROUP BY (${Mms.THREAD_ID}"
    } else {
        "${Mms.THREAD_ID} = ?"
    }

    val selectionArgs = if (threadId == null) {
        null
    } else {
        arrayOf(threadId.toString())
    }

    val messages = ArrayList<MessagesModel>()
    val contactsMap = HashMap<Int, ContactsModel>()
    val threadParticipants = HashMap<Long, ArrayList<ContactsModel>>()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, showErrors = true) { cursor ->
        val mmsId = cursor.getLongValue(Mms._ID)
        val type = cursor.getIntValue(Mms.MESSAGE_BOX)
        val date = cursor.getLongValue(Mms.DATE).toInt()
        val read = cursor.getIntValue(Mms.READ) == 1
        val threadId = cursor.getLongValue(Mms.THREAD_ID)
        val subscriptionId = cursor.getIntValue(Mms.SUBSCRIPTION_ID)
        val status = cursor.getIntValue(Mms.STATUS)
        val participants = if (threadParticipants.containsKey(threadId)) {
            threadParticipants[threadId]!!
        } else {
            val parts = getThreadParticipants(threadId, contactsMap)
            threadParticipants[threadId] = parts
            parts
        }

        val isMMS = true
        val attachment = getMmsAttachment(mmsId)
        val body = attachment.text
        var senderName = ""
        var senderPhotoUri = ""

        if (type != Mms.MESSAGE_BOX_SENT && type != Mms.MESSAGE_BOX_FAILED) {
            val number = getMMSSender(mmsId)
            val namePhoto = getNameAndPhotoFromPhoneNumber(number)
            senderName = namePhoto.name
            senderPhotoUri = namePhoto.photoUri ?: ""
        }

        val message = MessagesModel(mmsId, body, type, status, participants, date, read, threadId, isMMS, attachment, senderName, senderPhotoUri, subscriptionId)
        messages.add(message)

        participants.forEach {
            contactsMap[it.rawId] = it
        }
    }

    return messages
}

fun Context.getMMSSender(msgId: Long): String {
    val uri = Uri.parse("${Mms.CONTENT_URI}/$msgId/addr")
    val projection = arrayOf(
        Mms.Addr.ADDRESS
    )

    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Mms.Addr.ADDRESS)
            }
        }
    } catch (ignored: Exception) {
    }
    return ""
}

fun Context.getConversations(threadId: Long? = null, privateContacts: ArrayList<ContactsModel> = ArrayList()): ArrayList<ConversationSmsModel> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(
        Threads._ID,
        Threads.SNIPPET,
        Threads.DATE,
        Threads.READ,
        Threads.RECIPIENT_IDS
    )

    var selection = "${Threads.MESSAGE_COUNT} > ?"
    var selectionArgs = arrayOf("0")
    if (threadId != null) {
        selection += " AND ${Threads._ID} = ?"
        selectionArgs = arrayOf("0", threadId.toString())
    }

    val sortOrder = "${Threads.DATE} DESC"

    val conversations = ArrayList<ConversationSmsModel>()
    val simpleContactHelper = SimpleContactsHelperUtils(this)
    val blockedNumbers = getBlockedNumbers()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->
        val id = cursor.getLongValue(Threads._ID)
        var snippet = cursor.getStringValue(Threads.SNIPPET) ?: ""
        if (snippet.isEmpty()) {
            snippet = getThreadSnippet(id)
        }

        var date = cursor.getLongValue(Threads.DATE)
        if (date.toString().length > 10) {
            date /= 1000
        }

        val rawIds = cursor.getStringValue(Threads.RECIPIENT_IDS)
        val recipientIds = rawIds.split(" ").filter { it.areDigitsOnly() }.map { it.toInt() }.toMutableList()
        val phoneNumbers = getThreadPhoneNumbers(recipientIds)
        if (phoneNumbers.any { isNumberBlocked(it, blockedNumbers) }) {
            return@queryCursor
        }

        val names = getThreadContactNames(phoneNumbers, privateContacts)
        val title = TextUtils.join(", ", names.toTypedArray())
        val photoUri = if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else ""
        val isGroupConversation = phoneNumbers.size > 1
        val read = cursor.getIntValue(Threads.READ) == 1
        val conversation = ConversationSmsModel(id, snippet, date.toInt(), read, title, photoUri, isGroupConversation, phoneNumbers.first())
        conversations.add(conversation)
    }

    conversations.sortByDescending { it.date }
    return conversations
}

fun Context.getConversationIds(): List<Long> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(Threads._ID)
    val selection = "${Threads.MESSAGE_COUNT} > ?"
    val selectionArgs = arrayOf("0")
    val sortOrder = "${Threads.DATE} ASC"
    val conversationIds = mutableListOf<Long>()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->
        val id = cursor.getLongValue(Threads._ID)
        conversationIds.add(id)
    }
    return conversationIds
}


@SuppressLint("NewApi")
fun Context.getMmsAttachment(id: Long): AttachmentSMSModel {
    val uri = if (isQPlus()) {
        Mms.Part.CONTENT_URI
    } else {
        Uri.parse("content://mms/part")
    }

    val projection = arrayOf(
        Mms._ID,
        Mms.Part.CONTENT_TYPE,
        Mms.Part.TEXT
    )
    val selection = "${Mms.Part.MSG_ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    val messageAttachment = AttachmentSMSModel(id, "", arrayListOf())

    var attachmentName = ""
    queryCursor(uri, projection, selection, selectionArgs, showErrors = true) { cursor ->
        val partId = cursor.getLongValue(Mms._ID)
        val mimetype = cursor.getStringValue(Mms.Part.CONTENT_TYPE)
        if (mimetype == "text/plain") {
            messageAttachment.text = cursor.getStringValue(Mms.Part.TEXT) ?: ""
        } else if (mimetype.startsWith("image/") || mimetype.startsWith("video/")) {
            val attachment = AttachmentModel(partId, id, Uri.withAppendedPath(uri, partId.toString()).toString(), mimetype, 0, 0, "")
            messageAttachment.attachments.add(attachment)
        } else if (mimetype != "application/smil") {
            val attachment = AttachmentModel(partId, id, Uri.withAppendedPath(uri, partId.toString()).toString(), mimetype, 0, 0, attachmentName)
            messageAttachment.attachments.add(attachment)
        } else {
            val text = cursor.getStringValue(Mms.Part.TEXT)
            val cutName = text.substringAfter("ref src=\"").substringBefore("\"")
            if (cutName.isNotEmpty()) {
                attachmentName = cutName
            }
        }
    }

    return messageAttachment
}

fun Context.getLatestMMS(): MessagesModel? {
    val sortOrder = "${Mms.DATE} DESC LIMIT 1"
    return getMMS(sortOrder = sortOrder).firstOrNull()
}

fun Context.getThreadSnippet(threadId: Long): String {
    val sortOrder = "${Mms.DATE} DESC LIMIT 1"
    val latestMms = getMMS(threadId, sortOrder).firstOrNull()
    var snippet = latestMms?.body ?: ""

    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms.BODY
    )

    val selection = "${Sms.THREAD_ID} = ? AND ${Sms.DATE} > ?"
    val selectionArgs = arrayOf(
        threadId.toString(),
        latestMms?.date?.toString() ?: "0"
    )
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                snippet = cursor.getStringValue(Sms.BODY)
            }
        }
    } catch (ignored: Exception) {
    }
    return snippet
}

fun Context.getMessageRecipientAddress(messageId: Long): String {
    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms.ADDRESS
    )

    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(messageId.toString())

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Sms.ADDRESS)
            }
        }
    } catch (e: Exception) {
    }

    return ""
}

fun Context.getThreadParticipants(threadId: Long, contactsMap: HashMap<Int, ContactsModel>?): ArrayList<ContactsModel> {
    val uri = Uri.parse("${MmsSms.CONTENT_CONVERSATIONS_URI}?simple=true")
    val projection = arrayOf(
        ThreadsColumns.RECIPIENT_IDS
    )
    val selection = "${Mms._ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    val participants = ArrayList<ContactsModel>()
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val address = cursor.getStringValue(ThreadsColumns.RECIPIENT_IDS)
                address.split(" ").filter { it.areDigitsOnly() }.forEach {
                    val addressId = it.toInt()
                    if (contactsMap?.containsKey(addressId) == true) {
                        participants.add(contactsMap[addressId]!!)
                        return@forEach
                    }

                    val phoneNumber = getPhoneNumberFromAddressId(addressId)
                    val namePhoto = getNameAndPhotoFromPhoneNumber(phoneNumber)
                    val name = namePhoto.name
                    val photoUri = namePhoto.photoUri ?: ""
                    val contact = ContactsModel(addressId, addressId, name, photoUri, arrayListOf(phoneNumber), ArrayList(), ArrayList())
                    participants.add(contact)
                }
            }
        }
    } catch (e: Exception) {
        showErrorToast(e)
    }
    return participants
}

fun Context.getThreadPhoneNumbers(recipientIds: List<Int>): ArrayList<String> {
    val numbers = ArrayList<String>()
    recipientIds.forEach {
        numbers.add(getPhoneNumberFromAddressId(it))
    }
    return numbers
}

fun Context.getThreadContactNames(phoneNumbers: List<String>, privateContacts: ArrayList<ContactsModel>): ArrayList<String> {
    val names = ArrayList<String>()
    phoneNumbers.forEach { number ->
        val name = SimpleContactsHelperUtils(this).getNameFromPhoneNumber(number)
        if (name != number) {
            names.add(name)
        } else {
            val privateContact = privateContacts.firstOrNull { it.doesHavePhoneNumber(number) }
            if (privateContact == null) {
                names.add(name)
            } else {
                names.add(privateContact.name)
            }
        }
    }
    return names
}

fun Context.getPhoneNumberFromAddressId(canonicalAddressId: Int): String {
    val uri = Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses")
    val projection = arrayOf(
        Mms.Addr.ADDRESS
    )

    val selection = "${Mms._ID} = ?"
    val selectionArgs = arrayOf(canonicalAddressId.toString())
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Mms.Addr.ADDRESS)
            }
        }
    } catch (e: Exception) {
        showErrorToast(e)
    }
    return ""
}

fun Context.getSuggestedContacts(privateContacts: ArrayList<ContactsModel>): ArrayList<ContactsModel> {
    val contacts = ArrayList<ContactsModel>()
    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms.ADDRESS
    )

    val selection = "1 == 1) GROUP BY (${Sms.ADDRESS}"
    val selectionArgs = null
    val sortOrder = "${Sms.DATE} DESC LIMIT 20"
    val blockedNumbers = getBlockedNumbers()

    queryCursor(uri, projection, selection, selectionArgs, sortOrder, showErrors = true) { cursor ->
        val senderNumber = cursor.getStringValue(Sms.ADDRESS) ?: return@queryCursor
        val namePhoto = getNameAndPhotoFromPhoneNumber(senderNumber)
        var senderName = namePhoto.name
        var photoUri = namePhoto.photoUri ?: ""
        if (isNumberBlocked(senderNumber, blockedNumbers)) {
            return@queryCursor
        } else if (namePhoto.name == senderNumber) {
            if (privateContacts.isNotEmpty()) {
                val privateContact = privateContacts.firstOrNull { it.phoneNumbers.first() == senderNumber }
                if (privateContact != null) {
                    senderName = privateContact.name
                    photoUri = privateContact.photoUri
                } else {
                    return@queryCursor
                }
            } else {
                return@queryCursor
            }
        }

        val contact = ContactsModel(0, 0, senderName, photoUri, arrayListOf(senderNumber), ArrayList(), ArrayList())
        if (!contacts.map { it.phoneNumbers.first().trimToComparableNumber() }.contains(senderNumber.trimToComparableNumber())) {
            contacts.add(contact)
        }
    }

    return contacts
}

fun Context.getNameAndPhotoFromPhoneNumber(number: String): UserPhotoModel {
    if (!hasPermission(PERMISSION_READ_CONTACTS)) {
        return UserPhotoModel(number, null)
    }

    val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    val projection = arrayOf(
        PhoneLookup.DISPLAY_NAME,
        PhoneLookup.PHOTO_URI
    )

    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor.use {
            if (cursor?.moveToFirst() == true) {
                val name = cursor.getStringValue(PhoneLookup.DISPLAY_NAME)
                val photoUri = cursor.getStringValue(PhoneLookup.PHOTO_URI)
                return UserPhotoModel(name, photoUri)
            }
        }
    } catch (e: Exception) {
    }

    return UserPhotoModel(number, null)
}

fun Context.insertNewSMS(address: String, subject: String, body: String, date: Long, read: Int, threadId: Long, type: Int, subscriptionId: Int): Long {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.ADDRESS, address)
        put(Sms.SUBJECT, subject)
        put(Sms.BODY, body)
        put(Sms.DATE, date)
        put(Sms.READ, read)
        put(Sms.THREAD_ID, threadId)
        put(Sms.TYPE, type)
        put(Sms.SUBSCRIPTION_ID, subscriptionId)
    }

    return try {
        val newUri = contentResolver.insert(uri, contentValues)
        newUri?.lastPathSegment?.toLong() ?: 0L
    } catch (e: Exception) {
        0L
    }
}

fun Context.deleteConversation(threadId: Long) {
    var uri = Sms.CONTENT_URI
    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    try {
        contentResolver.delete(uri, selection, selectionArgs)
    } catch (e: Exception) {
        showErrorToast(e)
    }

    uri = Mms.CONTENT_URI
    contentResolver.delete(uri, selection, selectionArgs)

    conversationsDB.deleteThreadIdMessage(threadId)
    messagesDB.deleteThreadMessagesApp(threadId)
}

fun Context.deleteMessage(id: Long, isMMS: Boolean) {
    val uri = if (isMMS) Mms.CONTENT_URI else Sms.CONTENT_URI
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    try {
        contentResolver.delete(uri, selection, selectionArgs)
        messagesDB.delete(id)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.markMessageRead(id: Long, isMMS: Boolean) {
    val uri = if (isMMS) Mms.CONTENT_URI else Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.READ, 1)
        put(Sms.SEEN, 1)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
    messagesDB.markReadAsMessage(id)
}

fun Context.markThreadMessagesReadNew(threadId: Long) {
    arrayOf(Sms.CONTENT_URI, Mms.CONTENT_URI).forEach { uri ->
        val contentValues = ContentValues().apply {
            put(Sms.READ, 1)
            put(Sms.SEEN, 1)
        }
        val selection = "${Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        contentResolver.update(uri, contentValues, selection, selectionArgs)
    }
    messagesDB.markThreadRead(threadId)
}

fun Context.markThreadMessagesUnreadNew(threadId: Long) {
    arrayOf(Sms.CONTENT_URI, Mms.CONTENT_URI).forEach { uri ->
        val contentValues = ContentValues().apply {
            put(Sms.READ, 0)
            put(Sms.SEEN, 0)
        }
        val selection = "${Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        contentResolver.update(uri, contentValues, selection, selectionArgs)
    }
}

fun Context.updateMessageTypeApp(id: Long, type: Int) {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.TYPE, type)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
}

fun Context.updateMessageStatusNew(id: Long, status: Int) {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.STATUS, status)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
}

fun Context.updateMessageSubscriptionId(messageId: Long, subscriptionId: Int) {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.SUBSCRIPTION_ID, subscriptionId)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(messageId.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
}

fun Context.updateUnreadCountBadge(conversations: List<ConversationSmsModel>) {
    val unreadCount = conversations.count { !it.read }
    if (unreadCount == 0) {
        ShortcutBadger.removeCount(this)
    } else {
        ShortcutBadger.applyCount(this, unreadCount)
    }
}

@SuppressLint("NewApi")
fun Context.getThreadId(address: String): Long {
    return if (isMarshmallowPlus()) {
        try {
            Threads.getOrCreateThreadId(this, address)
        } catch (e: Exception) {
            0L
        }
    } else {
        0L
    }
}

@SuppressLint("NewApi")
fun Context.getThreadId(addresses: Set<String>): Long {
    return if (isMarshmallowPlus()) {
        try {
            Threads.getOrCreateThreadId(this, addresses)
        } catch (e: Exception) {
            0L
        }
    } else {
        0L
    }
}

fun Context.showReceivedMessageNotification(address: String, body: String, threadId: Long, bitmap: Bitmap?) {
    val privateCursor = getMyContactsCursor(false, true)?.loadInBackground()
    ensureBackgroundThread {
        val senderName = getNameFromAddress(address, privateCursor)

        Handler(Looper.getMainLooper()).post {
            showMessageNotification(address, body, threadId, bitmap, senderName)
        }
    }
}

fun Context.getNameFromAddress(address: String, privateCursor: Cursor?): String {
    var sender = getNameAndPhotoFromPhoneNumber(address).name
    if (address == sender) {
        val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(this, privateCursor)
        sender = privateContacts.firstOrNull { it.doesHavePhoneNumber(address) }?.name ?: address
    }
    return sender
}

@SuppressLint("NewApi", "UnspecifiedImmutableFlag")
fun Context.showMessageNotification(address: String, body: String, threadId: Long, bitmap: Bitmap?, sender: String) {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    if (isOreoPlus()) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
            .build()

        val name = getString(R.string.channel_received_sms)
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(NOTIFICATION_CHANNEL, name, importance).apply {
            setBypassDnd(false)
            enableLights(true)
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            notificationManager.createNotificationChannel(this)
        }
    }

    val intent = Intent(this, MessagesActivity::class.java).apply {
        putExtra(THREAD_ID, threadId)
        putExtra(isNotification, true)
        putExtra(THREAD_TITLE, "")
        putExtra(THREAD_NUMBER, "")
    }

    val pendingIntent = PendingIntent.getActivity(this, threadId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val summaryText = getString(R.string.new_message)
    val markAsReadIntent = Intent(this, MarkingAsReadService::class.java).apply {
        action = MARK_AS_READ
        putExtra(THREAD_ID, threadId)
    }
    var Otp = ""



    val markAsReadPendingIntent = PendingIntent.getBroadcast(this, 0, markAsReadIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)

    var replyAction: NotificationCompat.Action? = null

    if (isNougatPlus()) {
        val replyLabel = getString(R.string.reply)
        val remoteInput = RemoteInput.Builder(REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(this, ReplySMSService::class.java).apply {
            putExtra(THREAD_ID, threadId)
            putExtra(THREAD_NUMBER, address)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(applicationContext, threadId.hashCode(), replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        replyAction = NotificationCompat.Action.Builder(R.drawable.ic_send_vector, replyLabel, replyPendingIntent)
            .addRemoteInput(remoteInput)
            .build()
    }

    val largeIcon = bitmap ?: SimpleContactsHelperUtils(this).getContactLetterIcon(sender)
    val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL).apply {
        when (config.lockScreenVisibilitySetting) {
            LOCK_SCREEN_SENDER_MESSAGE -> {
                setContentTitle(sender)
                setLargeIcon(largeIcon)
                setContentText(body)
            }
            LOCK_SCREEN_SENDER -> {
                setContentTitle(sender)
                setLargeIcon(largeIcon)
            }
        }

        color = getAdjustedPrimaryColor()
        setSmallIcon(R.mipmap.ic_launcher)
        setStyle(NotificationCompat.BigTextStyle().setSummaryText(summaryText).bigText(body))
        setContentIntent(pendingIntent)
        priority = NotificationCompat.PRIORITY_MAX
        setDefaults(Notification.DEFAULT_LIGHTS)
        setCategory(Notification.CATEGORY_MESSAGE)
        setAutoCancel(true)
        setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
    }

    if (replyAction != null && config.lockScreenVisibilitySetting == LOCK_SCREEN_SENDER_MESSAGE) {
        builder.addAction(replyAction)
    }

    builder.addAction(R.drawable.icon_check, getString(R.string.mark_as_read), markAsReadPendingIntent)
        .setChannelId(NOTIFICATION_CHANNEL)


    if (body.isNotEmpty() &&
        (body.lowercase().contains("otp") || body.lowercase().contains("code")
            || body.lowercase().contains("pin"))
    ) {
        val pattern_4 = Pattern.compile("(\\d{4})")
        val matcher_4 = pattern_4.matcher(body)

        val pattern_5 = Pattern.compile("(\\d{5})")
        val matcher_5 = pattern_5.matcher(body)

        val pattern_6 = Pattern.compile("(\\d{6})")
        val matcher_6 = pattern_6.matcher(body)


        if (matcher_6.find() && matcher_6.group(0).length == 6) {
            Otp = matcher_6.group(0)
        } else if (matcher_5.find() && matcher_5.group(0).length == 5) {
            Otp = matcher_5.group(0)
        } else if (matcher_4.find() && matcher_4.group(0).length == 4) {
            Otp = matcher_4.group(0)
        }
        if(Otp.isNotEmpty()){
            val copyOTPIntent = Intent(this, MarkingAsReadService::class.java).apply {
                action = COPY_OTP
                putExtra(OTP, Otp)
                putExtra(THREAD_ID, threadId)
            }
            val copyOTPPendingIntent = PendingIntent.getBroadcast(this, 0, copyOTPIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)
            builder.addAction(R.drawable.logo_copy_black, getString(R.string.txt_copy)+" "+Otp, copyOTPPendingIntent)
                .setChannelId(NOTIFICATION_CHANNEL)
        }

    }


    notificationManager.notify(threadId.hashCode(), builder.build())
}

/*fun getFlagFor32(int: Int):Int{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_MUTABLE or int
    } else {
        int
    }
}*/

fun Context.removeDiacriticsIfNeeded(text: String): String {
    return if (config.useSimpleCharacters) text.normalizeString() else text
}

fun Context.getSmsDraft(threadId: Long): String? {
    val uri = Sms.Draft.CONTENT_URI
    val projection = arrayOf(Sms.BODY)
    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor.use {
            if (cursor?.moveToFirst() == true) {
                return cursor.getString(0)
            }
        }
    } catch (e: Exception) {
    }

    return null
}

fun Context.getAllDrafts(): HashMap<Long, String?> {
    val drafts = HashMap<Long, String?>()
    val uri = Sms.Draft.CONTENT_URI
    val projection = arrayOf(Sms.BODY, Sms.THREAD_ID)

    try {
        queryCursor(uri, projection) { cursor ->
            cursor.use {
                val threadId = cursor.getLongValue(Sms.THREAD_ID)
                val draft = cursor.getStringValue(Sms.BODY) ?: return@queryCursor
                drafts[threadId] = draft
            }
        }
    } catch (e: Exception) {
    }

    return drafts
}

fun Context.saveSmsDraft(body: String, threadId: Long) {
    val uri = Sms.Draft.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.BODY, body)
        put(Sms.DATE, System.currentTimeMillis().toString())
        put(Sms.TYPE, Sms.MESSAGE_TYPE_DRAFT)
        put(Sms.THREAD_ID, threadId)
    }

    try {
        contentResolver.insert(uri, contentValues)
    } catch (e: Exception) {
    }
}

fun Context.deleteSmsDraft(threadId: Long) {
    val uri = Sms.Draft.CONTENT_URI
    val projection = arrayOf(Sms._ID)
    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor.use {
            if (cursor?.moveToFirst() == true) {
                val draftId = cursor.getLong(0)
                val draftUri = Uri.withAppendedPath(Sms.CONTENT_URI, "/${draftId}")
                contentResolver.delete(draftUri, null, null)
            }
        }
    } catch (e: Exception) {
    }
}

fun Context.updateLastConversationMessage(threadId: Long) {
    val uri = Threads.CONTENT_URI
    val selection = "${Threads._ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    try {
        contentResolver.delete(uri, selection, selectionArgs)
        val newConversation = getConversations(threadId)[0]
        conversationsDB.insertOrUpdateMessage(newConversation)
    } catch (e: Exception) {
    }
}

fun Context.getFileSizeFromUri(uri: Uri): Long {
    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(uri, "r")
    } catch (e: FileNotFoundException) {
        null
    }

    // uses ParcelFileDescriptor#getStatSize underneath if failed
    val length = assetFileDescriptor?.use { it.length } ?: FILE_SIZE_NONE
    if (length != -1L) {
        return length
    }

    // if "content://" uri scheme, try contentResolver table
    if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        return contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            // maybe shouldn't trust ContentResolver for size:
            // https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex == -1) {
                return@use FILE_SIZE_NONE
            }
            cursor.moveToFirst()
            return try {
                cursor.getLong(sizeIndex)
            } catch (_: Throwable) {
                FILE_SIZE_NONE
            }
        } ?: FILE_SIZE_NONE
    } else {
        return FILE_SIZE_NONE
    }
}

fun Context.dialNumber(phoneNumber: String, callback: (() -> Unit)? = null) {
    Intent(Intent.ACTION_DIAL).apply {
        data = Uri.fromParts("tel", phoneNumber, null)

        try {
            startActivity(this)
            callback?.invoke()
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_app_found)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}
