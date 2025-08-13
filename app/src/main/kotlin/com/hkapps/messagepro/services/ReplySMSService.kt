package com.hkapps.messagepro.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.hkapps.messagepro.klinker.android.send_message.Settings
import com.hkapps.messagepro.klinker.android.send_message.Transaction
import com.hkapps.messagepro.extensions.notificationManager
import com.hkapps.messagepro.extensions.showErrorToast
import com.hkapps.messagepro.klinker.android.send_message.Message
import com.hkapps.messagepro.utils.ensureBackgroundThread
import com.hkapps.messagepro.utils.conversationsDB
import com.hkapps.messagepro.utils.markThreadMessagesReadNew
import com.hkapps.messagepro.utils.removeDiacriticsIfNeeded
import com.hkapps.messagepro.utils.REPLY
import com.hkapps.messagepro.utils.THREAD_ID
import com.hkapps.messagepro.utils.THREAD_NUMBER

class ReplySMSService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mAddress = intent.getStringExtra(THREAD_NUMBER)
        val mThreadID = intent.getLongExtra(THREAD_ID, 0L)
        var mMessage = RemoteInput.getResultsFromIntent(intent)!!.getCharSequence(REPLY)?.toString() ?: return

        mMessage = context.removeDiacriticsIfNeeded(mMessage)

        val settings = Settings()
        settings.useSystemSending = true
        settings.deliveryReports = true

        val transaction = Transaction(context, settings)
        val message = Message(mMessage, mAddress)

        try {
            val smsSentIntent = Intent(context, SMS_Service_Sended_Status_Receiver::class.java)
            val deliveredIntent = Intent(context, SendedSMSStatusService::class.java)

            transaction.setExplicitBroadcastForSentSms(smsSentIntent)
            transaction.setExplicitBroadcastForDeliveredSms(deliveredIntent)

            transaction.sendNewMessage(message, mThreadID)
        } catch (e: Exception) {
            context.showErrorToast(e)
        }

        context.notificationManager.cancel(mThreadID.hashCode())

        ensureBackgroundThread {
            context.markThreadMessagesReadNew(mThreadID)
            context.conversationsDB.markReadAsMessage(mThreadID)
        }
    }
}
