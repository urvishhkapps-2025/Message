package com.hkapps.messagepro.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.hkapps.messagepro.extensions.isNumberBlocked
import com.hkapps.messagepro.model.ContactsModel
import com.hkapps.messagepro.model.MessagesModel
import com.hkapps.messagepro.utils.*

class ReceiverService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        var address = ""
        var body = ""
        var subject = ""
        var date = 0L
        var threadId = 0L
        var status = Telephony.Sms.STATUS_NONE
        val type = Telephony.Sms.MESSAGE_TYPE_INBOX
        val read = 0
        val subscriptionId = intent.getIntExtra("subscription", -1)

        ensureBackgroundThread {
            messages.forEach {
                address = it.originatingAddress ?: ""
                subject = it.pseudoSubject
                status = it.status
                body += it.messageBody
                date = Math.min(it.timestampMillis, System.currentTimeMillis())
                threadId = context.getThreadId(address)
            }

            Handler(Looper.getMainLooper()).post {
                if (!context.isNumberBlocked(address)) {
                    ensureBackgroundThread {
                        val newMessageId = context.insertNewSMS(address, subject, body, date, read, threadId, type, subscriptionId)

                        val conversation = context.getConversations(threadId).firstOrNull() ?: return@ensureBackgroundThread
                        try {
                            context.conversationsDB.insertOrUpdateMessage(conversation)
                        } catch (ignored: Exception) {
                        }

                        try {
                            context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversationsMessage())
                        } catch (ignored: Exception) {
                        }

                        val mParticipant = ContactsModel(0, 0, address, "", arrayListOf(address), ArrayList(), ArrayList())
                        val mParticipants = arrayListOf(mParticipant)
                        val mMessageDate = (date / 1000).toInt()
                        val mMessage =
                            MessagesModel(newMessageId, body, type, status, mParticipants, mMessageDate, false, threadId, false, null, address, "", subscriptionId)
                        context.messagesDB.insertOrUpdateMessage(mMessage)
                        Log.e("Event: ", " SMS_Service_Receiver BroadcastReceiver")
                        refreshMessages()
                    }

                    context.showReceivedMessageNotification(address, body, threadId, null)
                }
            }
        }
    }
}
