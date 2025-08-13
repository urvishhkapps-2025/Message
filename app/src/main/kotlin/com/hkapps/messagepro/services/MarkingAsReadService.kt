package com.hkapps.messagepro.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hkapps.messagepro.extensions.copyToClipboard
import com.hkapps.messagepro.extensions.notificationManager
import com.hkapps.messagepro.utils.conversationsDB
import com.hkapps.messagepro.utils.markThreadMessagesReadNew
import com.hkapps.messagepro.utils.updateUnreadCountBadge
import com.hkapps.messagepro.utils.*

class MarkingAsReadService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MARK_AS_READ -> {
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {
                    context.markThreadMessagesReadNew(threadId)
                    context.conversationsDB.markReadAsMessage(threadId)
                    context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversationsMessage())
                    Log.e("Event: ", " SMS_Service_Marking_As_Read MARK_AS_READ")
                    refreshMessages()
                }
                return
            }
            COPY_OTP -> {
                val otpCopy = intent.getStringExtra(OTP)
                Log.e("COPY_OTP: " , ""+otpCopy)
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {

                    if (otpCopy != null) {
                        context.copyToClipboard(otpCopy)
                    }
                    /*context.markThreadMessagesReadNew(threadId)
                    context.conversationsDB.markReadAsMessage(threadId)
                    context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversationsMessage())
                    refreshMessages()*/
                }
                return
            }
        }
    }
}
