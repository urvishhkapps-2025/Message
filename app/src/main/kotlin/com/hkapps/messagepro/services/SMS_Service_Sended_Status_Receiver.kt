package com.hkapps.messagepro.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hkapps.messagepro.klinker.android.send_message.SentReceiver
import com.hkapps.messagepro.extensions.getAdjustedPrimaryColor
import com.hkapps.messagepro.extensions.getMyContactsCursor
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.MessagesActivity
import com.hkapps.messagepro.utils.*

class SMS_Service_Sended_Status_Receiver : SentReceiver() {

    override fun onMessageStatusUpdated(context: Context, intent: Intent, receiverResultCode: Int) {
        if (intent.extras?.containsKey("message_uri") == true) {
            val uri = Uri.parse(intent.getStringExtra("message_uri"))
            val mMessageID = uri?.lastPathSegment?.toLong() ?: 0L
            ensureBackgroundThread {
                val type = if (intent.extras!!.containsKey("errorCode")) {
                    showSendingFailedNotification(context, mMessageID)
                    Telephony.Sms.MESSAGE_TYPE_FAILED
                } else {
                    Telephony.Sms.MESSAGE_TYPE_SENT
                }

                context.updateMessageTypeApp(mMessageID, type)
                context.messagesDB.updateType(mMessageID, type)
                Log.e("Event: ", " SMS_Service_Sended_Status_Receiver onMessageStatusUpdated")
                refreshMessages()
            }
        }
    }

    private fun showSendingFailedNotification(context: Context, mMessageID: Long) {
        Handler(Looper.getMainLooper()).post {
            val privateCursor = context.getMyContactsCursor(false, true)?.loadInBackground()
            ensureBackgroundThread {
                val address = context.getMessageRecipientAddress(mMessageID)
                val threadId = context.getThreadId(address)
                val senderName = context.getNameFromAddress(address, privateCursor)
                showNotification(context, senderName, threadId)
            }
        }
    }

    @SuppressLint("NewApi", "UnspecifiedImmutableFlag")
    private fun showNotification(context: Context, recipientName: String, threadId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (isOreoPlus()) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                .build()

            val name = context.getString(R.string.message_not_sent_short)
            val importance = NotificationManager.IMPORTANCE_HIGH
            NotificationChannel(NOTIFICATION_CHANNEL, name, importance).apply {
                setBypassDnd(false)
                enableLights(true)
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }
        }

        val intent = Intent(context, MessagesActivity::class.java).apply {
            putExtra(THREAD_ID, threadId)
            putExtra(isNotification, true)
        }

        val pendingIntent = PendingIntent.getActivity(context, threadId.hashCode(), intent, returnPendingIntentFlag(PendingIntent.FLAG_UPDATE_CURRENT), Bundle())
        val summaryText = String.format(context.getString(R.string.message_sending_error), recipientName)

        val largeIcon = SimpleContactsHelperUtils(context).getContactLetterIcon(recipientName)
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle(context.getString(R.string.message_not_sent_short))
            .setContentText(summaryText)
            .setColor(context.getAdjustedPrimaryColor())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeIcon)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
            .setChannelId(NOTIFICATION_CHANNEL)

        notificationManager.notify(threadId.hashCode(), builder.build())
    }
}

fun returnPendingIntentFlag(oldFlag: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    } else {
        oldFlag
    }
}

