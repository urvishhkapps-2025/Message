package com.hkapps.messagepro.mms.transaction;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import com.hkapps.messagepro.android.database.SqliteWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;


import com.hkapps.messagepro.android.mms.ContentType;
import com.hkapps.messagepro.android.mms.MmsException;
import com.hkapps.messagepro.android.mms.pdu_alt.GenericPdu;
import com.hkapps.messagepro.android.mms.pdu_alt.PduParser;
import com.hkapps.messagepro.android.mms.pdu_alt.PduPersister;

import android.util.Log;
import com.hkapps.messagepro.klinker.android.send_message.BroadcastUtils;
import com.hkapps.messagepro.klinker.android.send_message.Settings;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;

public class PushReceiver extends BroadcastReceiver {
    private static final String TAG = "loggg";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = true;

    static final String[] PROJECTION = new String[] {
            Mms.CONTENT_LOCATION,
            Mms.LOCKED
    };

    static final int COLUMN_CONTENT_LOCATION      = 0;

    private static Set<String> downloadedUrls = new HashSet<String>();
    private static final ExecutorService PUSH_RECEIVER_EXECUTOR = Executors.newSingleThreadExecutor();

    private class ReceivePushTask extends AsyncTask<Intent,Void,Void> {
        private Context mContext;
        private PendingResult pendingResult;

        ReceivePushTask(Context context, PendingResult pendingResult) {
            mContext = context;
            this.pendingResult = pendingResult;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Log.v(TAG, "receiving a new mms message");
            Intent intent = intents[0];

            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(pushData);
            GenericPdu pdu = parser.parse();

            if (null == pdu) {
                Log.e(TAG, "Invalid PUSH data");
                return null;
            }

            PduPersister p = PduPersister.getPduPersister(mContext);
            ContentResolver cr = mContext.getContentResolver();
            int type = pdu.getMessageType();
            long threadId = -1;
            int subId = intent.getIntExtra("subscription", Settings.DEFAULT_SUBSCRIPTION_ID);

            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            if (pendingResult != null) {
                pendingResult.finish();
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, intent.getAction() + " " + intent.getType());
        if ((intent.getAction().equals(WAP_PUSH_DELIVER_ACTION) || intent.getAction().equals(WAP_PUSH_RECEIVED_ACTION))
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Received PUSH Intent: " + intent);
            }

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            if ((!sharedPrefs.getBoolean("receive_with_stock", false) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && sharedPrefs.getBoolean("override", true))
                    || Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                MmsConfig.init(context);
                new ReceivePushTask(context, null).executeOnExecutor(PUSH_RECEIVER_EXECUTOR, intent);

                Log.v("mms_receiver", context.getPackageName() + " received and aborted");
            } else {
                clearAbortBroadcast();
                Intent notificationBroadcast = new Intent(com.hkapps.messagepro.klinker.android.send_message.Transaction.NOTIFY_OF_MMS);
                notificationBroadcast.putExtra("receive_through_stock", true);
                BroadcastUtils.sendExplicitBroadcast(
                        context,
                        notificationBroadcast,
                        com.hkapps.messagepro.klinker.android.send_message.Transaction.NOTIFY_OF_MMS);

                Log.v("mms_receiver", context.getPackageName() + " received and not aborted");
            }
        }
    }

    public static String getContentLocation(Context context, Uri uri)
            throws MmsException {
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                uri, PROJECTION, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    String location = cursor.getString(COLUMN_CONTENT_LOCATION);
                    cursor.close();
                    return location;
                }
            } finally {
                cursor.close();
            }
        }

        throw new MmsException("Cannot get X-Mms-Content-Location from: " + uri);
    }

}
