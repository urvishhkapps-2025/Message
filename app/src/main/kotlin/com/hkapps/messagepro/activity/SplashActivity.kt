package com.hkapps.messagepro.activity

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hkapps.messagepro.BuildConfig
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.ads.AdsHelperClass
import com.hkapps.messagepro.ads.AppOpenAds
import com.hkapps.messagepro.model.RemoteAppDataModel
import com.hkapps.messagepro.ads.SharedPrefrenceClass
import com.hkapps.messagepro.extensions.mPref
import com.hkapps.messagepro.extensions.toast
import com.hkapps.messagepro.utils.*

class SplashActivity : BaseHomeActivity() {
    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null
    private var isAppOpenAdLoad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash)


        AdsHelperClass.setOpenAdsShowedCount(0)
        SharedPrefrenceClass.getInstance()!!.setBoolean("isSplash", true)

        mPref.dateFormat = DATE_FORMAT_SIX


        if (MainAppClass.isNetworkConnected(this)) {
            remoteConfig()
        } else {
            AdsHelperClass.setIsAdEnable(0)
            toHome()
        }
    }

    fun remoteConfig() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(30).build()
        mFirebaseRemoteConfig!!.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig!!.fetchAndActivate()
            .addOnCompleteListener(this) { task: Task<Boolean?> ->
                if (task.isSuccessful) {
                    val response = mFirebaseRemoteConfig!!.getString("ads")
                    if (!response.isEmpty()) {
                        Log.e("RemoteData: ", response)
                        doNext(response)
                    } else {
                        toHome()
                    }
                } else {
                    toHome()
                }
            }.addOnFailureListener { e: Exception ->
                Log.e("Splash: ", "RemoteData: " + e.message)
                toHome()
            }
    }

    fun parseAppUserListModel(jsonObject: String?): RemoteAppDataModel? {
        try {
            val gson = Gson()
            val token: TypeToken<RemoteAppDataModel> =
                object : TypeToken<RemoteAppDataModel>() {}
            return gson.fromJson(jsonObject, token.type)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }


    fun doNext(response: String?) {
        AdsHelperClass.setRemote(response)
        val appData = parseAppUserListModel(response)
        setAppData(appData!!)

        val appLifecycleObserver = AppOpenAds(MainAppClass.instance)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        Handler(Looper.getMainLooper()).postDelayed({
            if (AdsHelperClass.getIsAdEnable() == 1) {
                if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
                    Log.e(MainAppClass.ADMOB_TAG, "Ad Type ===> Admob")
                } else {
                    Log.e(MainAppClass.ADMOB_TAG, "Ad Type ===> Adx")
                }
                Log.e(MainAppClass.ADMOB_TAG, "All Ads ===> Enable")

                if (AdsHelperClass.getShowNative() == 1) {
                    MainAppClass.instance?.setmAdsIdClear()
                    if (BuildConfig.DEBUG) {
                        for (i in 0 until appData.nativeid.size) {
                            MainAppClass.instance?.setmAdsId(getString(R.string.admob_native_ads_id))
                        }
                    } else {
                        for (i in 0 until appData.nativeid.size) {
                            MainAppClass.instance?.setmAdsId(appData.nativeid[i])
                        }
                    }
                    MainAppClass.instance?.loadNativeOptional(0, this@SplashActivity)
                }

                if (AdsHelperClass.getShowAppOpen() == 1) {
                    MainAppClass.instance?.loadOpenAppAdsOnSplash(this) { isLoaded ->
                        Utility.PrintLog(
                            "Ads ",
                            "OpenApp loadOpenAppAdsOnSplash:  $isLoaded"
                        )
                        if (isLoaded && !isFinishing && !isDestroyed) {
                            Utility.PrintLog("Ads Next", "From OpenApp Load")
                            isAppOpenAdLoad = true
                            if (!isFinishing) loadAppOpenAd()
                        }
                    }
                }

                Handler(Looper.myLooper()!!).postDelayed({
                    if (!isAppOpenAdLoad) {
                        Utility.PrintLog("Ads Next ", "From OpenApp Time Out")
                        toHome()
                    }
                }, AdsHelperClass.getSplashTime() * 1000L)

            } else {
                Log.e(MainAppClass.ADMOB_TAG, "All Ads ===> Disable")
            }
        }, 2000)
    }

    private fun setAppData(appData: RemoteAppDataModel) {
        try {
            AdsHelperClass.setBannerAd(java.lang.String.valueOf(appData.bannerid))
            AdsHelperClass.setShowBanner(appData.banner)
            AdsHelperClass.setInterstitialAd(java.lang.String.valueOf(appData.interstitialid))
            AdsHelperClass.setShowInterstitial(appData.interstitial)
            AdsHelperClass.setInterstitialAdsClick(appData.ads_per_click)
            AdsHelperClass.setShowNative(appData.native)
            AdsHelperClass.setAppOpenAd(java.lang.String.valueOf(appData.openadid))
            AdsHelperClass.setShowAppOpen(appData.openad)
            AdsHelperClass.setAppOpenCount(appData.app_open_count)
            AdsHelperClass.setId(appData.id)
            AdsHelperClass.setAdType(java.lang.String.valueOf(appData.adtype))
            AdsHelperClass.setInAppreview(appData.inAppreview)
            AdsHelperClass.setIsAdEnable(appData.isAdEnable)
            AdsHelperClass.setAdsPerSession(appData.ads_per_session)
            AdsHelperClass.setIsSplashOn(appData.is_splash_on)
            AdsHelperClass.setSplashTime(appData.splash_time)

            //TEST
//            AdsHelperClass.setAdType("adx");
//            AdsHelperClass.setInAppreview(1);
//            AdsHelperClass.setTransferLink("com.instagram.android");
//            AdsHelperClass.setIsAdEnable(1);
//            AdsHelperClass.setExitAdEnable(0);
//            AdsHelperClass.setAdsPerSession(5);
//            AdsHelperClass.setAppOpenCount(2);
//            AdsHelperClass.setInterstitialAdsClick(2);
        } catch (e: java.lang.Exception) {
            Utility.PrintLog("Exception", e.message)
        }
    }


    private fun loadAppOpenAd() {
        if (!AdsHelperClass.isShowingFullScreenAd
            && MainAppClass.appOpenAd != null && MainAppClass.isNetworkConnected(this)
            && AdsHelperClass.getIsSplashOn() == 1
        ) {
            val callback: FullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    MainAppClass.appOpenAd = null
                    Utility.PrintLog(
                        MainAppClass.ADMOB_TAG,
                        "Open AD ===> The ad was dismissed."
                    )
                    toHome()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Utility.PrintLog(
                        MainAppClass.ADMOB_TAG,
                        "Open AD ===> The ad failed to show."
                    )
                }

                override fun onAdShowedFullScreenContent() {
                    Utility.PrintLog(MainAppClass.ADMOB_TAG, "Open AD ===> The ad was shown.")
                }
            }
            MainAppClass.appOpenAd!!.fullScreenContentCallback = callback
            MainAppClass.appOpenAd!!.show(this)
        }
    }

    private fun toHome() {

        if (!Utility.getIsIntroShow()) {
            startActivity(Intent(this, ActivityIntroduction::class.java))
            finish()
        } else {
            getPermission()
        }

    }



    private fun getPermission() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Log.e("Event: ", "askPermissions isQPlus")
                    askPermissions()
                } else {
                    startActivity(Intent(this, ActivityPermissions::class.java))
                    finish()
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                Log.e("Event: ", "askPermissions isQPlus else")
                askPermissions()
            } else {
                startActivity(Intent(this, ActivityPermissions::class.java))
                finish()
            }
        }
    }

    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()

                        }
                    } else {
                        startActivity(Intent(this, ActivityPermissions::class.java))
                        finish()
                    }
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        }
    }
}
