package com.hkapps.messagepro.ads;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.hkapps.messagepro.BuildConfig;
import com.hkapps.messagepro.MainAppClass;
import com.hkapps.messagepro.R;


public class AppOpenAds implements LifecycleObserver, Application.ActivityLifecycleCallbacks {
    private static final String LOG_TAG = "AppOpenManager";
    private static boolean isShowingAd = false;
    private final MainAppClass myApplication;
    private AppOpenAd appOpenAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private Activity currentActivity;


    public AppOpenAds(MainAppClass myApplication) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        showAdIfAvailable();
        Log.d(LOG_TAG, "onStart");
    }

    public void showAdIfAvailable() {

        if (AdsHelperClass.getIsAdEnable() != 1) {
            return;
        }
        if (AdsHelperClass.getShowAppOpen() != 1) {
            return;
        }


        if (AdsHelperClass.isShowingFullScreenAd) {
            return;
        }
        if (isShowingAd) {
            return;
        }


        if (appOpenAd != null && !SharedPrefrenceClass.Companion.getInstance().getBoolean("isSplash", true)) {
            Log.d(LOG_TAG, "Will show ad.");
            FullScreenContentCallback callback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            AdsHelperClass.isShowingFullScreenAd = false;
                            appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            AdsHelperClass.isShowingFullScreenAd = false;
                            appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            AdsHelperClass.isShowingFullScreenAd = true;
                            isShowingAd = true;
                        }
                    };

            appOpenAd.setFullScreenContentCallback(callback);

            appOpenAd.show(currentActivity);
            int count = AdsHelperClass.getOpenAdsShowedCount();
            AdsHelperClass.setOpenAdsShowedCount(count + 1);
            AdsHelperClass.setOpenAdsShowedCount(count + 1);
            AdsHelperClass.isShowingFullScreenAd = true;
            isShowingAd = true;
        } else {
            Log.d(LOG_TAG, "Can not show ad.");
            fetchAd();
        }
    }

    public void fetchAd() {
        if (AdsHelperClass.getIsAdEnable() != 1) {
            return;
        }
        if (AdsHelperClass.getShowAppOpen() != 1) {
            return;
        }

        if (isShowingAd) {
            return;
        }
        if (AdsHelperClass.isShowingFullScreenAd) {
            return;
        }
        if (appOpenAd != null) {
            return;
        }

        int showCount = AdsHelperClass.getOpenAdsShowedCount();
        int totalLimit = AdsHelperClass.getAppOpenCount();
        if (showCount >= totalLimit) {
            Log.e("Ads: ", "Open App Limit Exist  ShowCount: " + showCount + "  totalLimit: " + totalLimit);
            return;
        }


        if (appOpenAd == null) {
            loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {

                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    appOpenAd = ad;
                }


                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    AdsHelperClass.isShowingFullScreenAd = false;
                }

            };


            String adUnitId = "";

            if (BuildConfig.DEBUG) {
                if (AdsHelperClass.getAdType().equalsIgnoreCase(AdsHelperClass.AD_TYPE_ADMOB)) {
                    adUnitId = myApplication.getString(R.string.app_open_ads_id);
                } else if (AdsHelperClass.getAdType().equalsIgnoreCase(AdsHelperClass.AD_TYPE_ADX)) {
                    adUnitId = myApplication.getString(R.string.adx_app_open_ads_id);
                } else
                    adUnitId = myApplication.getString(R.string.adx_app_open_ads_id);
            } else {
                adUnitId = AdsHelperClass.getAppOpenAd();
            }

            if (adUnitId == null) {
                return;
            }
            if (adUnitId.isEmpty()) {
                return;
            }
            Log.e("Ads: ", "Load Open App Manager id: " + adUnitId);
            AppOpenAd.load(myApplication, adUnitId, new AdRequest.Builder().build(),AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
    }

}
