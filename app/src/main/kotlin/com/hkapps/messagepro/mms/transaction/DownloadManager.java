
package com.hkapps.messagepro.mms.transaction;

import android.content.Context;
import android.net.Uri;

/**
 * In order to avoid downloading duplicate MMS.
 * We should manage to call SMSManager.downloadMultimediaMessage().
 */
public class DownloadManager {
    private static DownloadManager ourInstance = new DownloadManager();

    public static DownloadManager getInstance() {
        return ourInstance;
    }

    private DownloadManager() {

    }

    public void downloadMultimediaMessage(final Context context, final String location, Uri uri, boolean byPush, int subscriptionId) {

    }
}
