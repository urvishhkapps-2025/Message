package com.hkapps.messagepro.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.hkapps.messagepro.klinker.android.send_message.DeliveredReceiver
import com.hkapps.messagepro.utils.ensureBackgroundThread
import com.hkapps.messagepro.utils.messagesDB
import com.hkapps.messagepro.utils.updateMessageStatusNew
import com.hkapps.messagepro.utils.refreshMessages

class SendedSMSStatusService : DeliveredReceiver() {

    override fun onMessageStatusUpdated(context: Context, intent: Intent, receiverResultCode: Int) {
        if (intent.extras?.containsKey("message_uri") == true) {
            val uri = Uri.parse(intent.getStringExtra("message_uri"))
            val mMessageID = uri?.lastPathSegment?.toLong() ?: 0L
            ensureBackgroundThread {
                val status = Telephony.Sms.STATUS_COMPLETE
                context.updateMessageStatusNew(mMessageID, status)
                val updated = context.messagesDB.updateStatusMessages(mMessageID, status)
                if (updated == 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        ensureBackgroundThread {
                            context.messagesDB.updateStatusMessages(mMessageID, status)
                        }
                    }, 2000)
                }
                Log.e("Event: ", " SMS_Service_Sended_Status DeliveredReceiver")
                refreshMessages()
            }
        }
    }
}
