package com.hkapps.messagepro

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Process
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.FirebaseApp
import com.hkapps.messagepro.ads.AdsHelperClass
import com.hkapps.messagepro.ads.native_ads_event.AppOpenAdsListners
import com.hkapps.messagepro.ads.native_ads_event.EventConstants
import com.hkapps.messagepro.ads.native_ads_event.EventNotifierApp
import com.hkapps.messagepro.ads.native_ads_event.NotifierFactoryApp
import com.hkapps.messagepro.extensions.mPref
import com.hkapps.messagepro.listners.OnActivityResultLauncher
import com.hkapps.messagepro.utils.Utility
import com.hkapps.messagepro.utils.isNougatPlus
import com.tappx.sdk.android.Tappx
import java.util.*

class MainAppClass : Application() {
    var sContext: Context? = null
    val ADMOB_TAG = "Ads:"
    var context: Context? = null

    //NativeAd
    var mNativeAdsGHome: ArrayList<NativeAd> = ArrayList()
    var mAdsId = ArrayList<String>()

    //InterstitialAd
    var mInterstitialAd: InterstitialAd? = null
    var mAdManagerInterstitialAd: AdManagerInterstitialAd? = null


    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
        mInstance = this
        sContext = applicationContext
        application = this

        when (mPref.appTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        val testDeviceIds = Arrays.asList("")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(this) { initializationStatus: InitializationStatus? -> }
        FirebaseApp.initializeApp(this);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        checkAppReplacingState()

        Tappx.setCollectLocationEnabled(applicationContext,true);

    }

    fun checkUseEnglish() {
        if (mPref.useEnglish && !isNougatPlus()) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
    }

    public override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)
    }

    private fun checkAppReplacingState() {
        if (resources == null) {
            Process.killProcess(Process.myPid())
        }
    }

    companion object {
        const val ADMOB_TAG = "Ads:"
        var loadAdsDialog: Dialog? = null
        private var mInstance: MainAppClass? = null
        var application: MainAppClass? = null
        var appOpenAd: AppOpenAd? = null

        @JvmStatic
        @get:Synchronized
        val instance: MainAppClass?
            get() {
                synchronized(MainAppClass::class.java) {
                    synchronized(MainAppClass::class.java) {
                        application = mInstance
                    }
                }
                return application
            }

        fun isNetworkConnected(activity: Activity): Boolean {
            val cm = activity.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        }

    }


    // Load Banner Ads
    fun loadBanner(adContainerView: RelativeLayout, activity: Activity) {

//        adContainerView.setVisibility(View.GONE);
        if (!isNetworkConnected(activity)) {
            return
        }
        if (AdsHelperClass.getIsAdEnable() != 1) {
            return
        }
        if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
            loadAdMobBanner(adContainerView, activity)
        } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
            loadAdxBanner(adContainerView, activity)
        }
    }

    private fun loadAdMobBanner(adContainerView: RelativeLayout, activity: Activity) {
        if (AdsHelperClass.getShowBanner() == 1) {
            val adUnitId: String
            adUnitId = if (BuildConfig.DEBUG) getString(R.string.admob_banner_ads_id) else {
                AdsHelperClass.getBannerAd()
            }
            Utility.PrintLog(ADMOB_TAG, "BannerAd ID ==> $adUnitId")
            if (TextUtils.isEmpty(adUnitId)) {
                return
            }

            //BannerAd
            val admobManagerAdView = AdView(activity)
            admobManagerAdView.adUnitId = adUnitId
            adContainerView.addView(admobManagerAdView)
            val adRequest = AdRequest.Builder().build()
            val adSize = getAdSize(activity)
            admobManagerAdView.setAdSize(adSize)
            admobManagerAdView.loadAd(adRequest)
            admobManagerAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    adContainerView.visibility = View.VISIBLE
                    Utility.PrintLog(ADMOB_TAG, "BannerAd ==> onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    adContainerView.visibility = View.GONE
                    Utility.PrintLog(ADMOB_TAG, "BannerAd ==> onAdFailedToLoad " + loadAdError.message)
                }
            }
        }
    }

    private fun loadAdxBanner(adContainerView: RelativeLayout, activity: Activity) {
        if (AdsHelperClass.getShowBanner() == 1) {
            val adUnitId: String
            adUnitId = if (BuildConfig.DEBUG) {
                getString(R.string.adx_banner_ads_id)
            } else {
                AdsHelperClass.getBannerAd()
            }
            Utility.PrintLog(ADMOB_TAG, "BannerAd ID ==> $adUnitId")
            if (TextUtils.isEmpty(adUnitId)) {
                return
            }
            val adXManagerAdView = AdManagerAdView(activity)
            adXManagerAdView.adUnitId = adUnitId
            adContainerView.addView(adXManagerAdView)
            val adRequest = AdRequest.Builder().build()
            val adSize = getAdSize(activity)
            adXManagerAdView.setAdSize(adSize)
            adXManagerAdView.loadAd(adRequest)
            adXManagerAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    adContainerView.visibility = View.VISIBLE
                    Utility.PrintLog(ADMOB_TAG, "BannerAd ==> onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Utility.PrintLog(ADMOB_TAG, "BannerAd ==> onAdFailedToLoad " + loadAdError.message)
                }
            }
        }
    }

    private fun getAdSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }


    //Native
    fun getGNativeHome(): List<NativeAd?>? {
        return mNativeAdsGHome
    }

    fun setmAdsId(id: String?) {
        mAdsId.add(id!!)
    }

    fun getmAdsIdSize(): Int {
        return mAdsId.size
    }

    fun setmAdsIdClear() {
        mAdsId.clear()
    }

    fun loadNativeOptional(adxCount: Int, activity: Activity?) {
        if (!isNetworkConnected(activity!!)) {
            return
        }
        if (AdsHelperClass.getIsAdEnable() != 1) {
            return
        }
        if (AdsHelperClass.getShowNative() == 1) {
            if (adxCount == 0) {
                mNativeAdsGHome = ArrayList()
                Utility.PrintLog(ADMOB_TAG, "NativeAds ID ==> 0")
            }
            val builder: AdLoader.Builder
            val adUnitId: String
            adUnitId = if (BuildConfig.DEBUG) {
                if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
                    getString(R.string.admob_native_ads_id)
                } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
                    getString(R.string.adx_native_ads_id)
                } else {
                    return
                }
            } else {
                mAdsId[adxCount]
            }
            Utility.PrintLog(ADMOB_TAG, "NativeAds ID ==> $adUnitId")
            if (TextUtils.isEmpty(adUnitId)) {
                return
            }
            builder = AdLoader.Builder(this, adUnitId)
            builder.forNativeAd { nativeAd: NativeAd? ->
                mNativeAdsGHome.add(nativeAd!!)
                val nextConunt = adxCount + 1
                if (nextConunt < mAdsId.size) {
                    loadNativeOptional(nextConunt, activity)
                }
                if (nextConunt == mAdsId.size) {
                    Utility.PrintLog(ADMOB_TAG, "NativeAds ID => last ==> $adxCount")
                    val notifier: EventNotifierApp = NotifierFactoryApp.Companion.instance!!.getNotifier(NotifierFactoryApp.EVENT_NOTIFIER_AD_STATUS)
                    notifier.eventNotify(EventConstants.EVENT_AD_LOADED_NATIVE, null)
                }
            }.withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Utility.PrintLog(ADMOB_TAG, "NativeAds ID ==> " + "Success to onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Utility.PrintLog(ADMOB_TAG, "NativeAds ID => " + loadAdError.message)
                }
            })
            val videoOptions = VideoOptions.Builder()
                .setStartMuted(true)
                .build()
            val adOptions = NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build()
            builder.withNativeAdOptions(adOptions)
            val adLoader = builder.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Utility.PrintLog(ADMOB_TAG, "onAdFailedToLoad ==> " + adError.message)
                    if (mNativeAdsGHome.size == 0) {
                        Utility.PrintLog(ADMOB_TAG, "onAdFailedToLoad mNativeAdsGHome.size() ==>  0")
                    } else {
                        Utility.PrintLog(ADMOB_TAG, "onAdFailedToLoad mNativeAdsGHome.size() ==>  Event")
                    }
                }
            }).build()
            if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
                adLoader.loadAd(AdRequest.Builder().build())
            } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
                adLoader.loadAd(AdManagerAdRequest.Builder().build())
            }
        }
    }


    fun loadNativeAd(fl_adplaceholder: FrameLayout, activity: Context?, adNo: Int) {
        try {
            if (instance!!.getGNativeHome() != null && instance!!.getGNativeHome()!!.size > 0
                && instance!!.getGNativeHome()!![adNo] != null
            ) {
                val nativeAd: NativeAd = instance!!.getGNativeHome()!![adNo]!!
                if (nativeAd != null) {
                    val adView = LayoutInflater.from(activity).inflate(R.layout.ads_native_list, null) as NativeAdView
                    populateUnifiedNativeAdViewNoVideo( nativeAd, adView)
                    fl_adplaceholder.visibility = View.VISIBLE
                    fl_adplaceholder.removeAllViews()
                    fl_adplaceholder.addView(adView)
                    Utility.PrintLog(ADMOB_TAG, "NativeAds ID ==> " + "Success to show")
                } else {
                    fl_adplaceholder.visibility = View.GONE
                    Utility.PrintLog(ADMOB_TAG, "NativeAds ID ==> $adNo null 111")
                }
            } else {
                fl_adplaceholder.visibility = View.GONE
                Utility.PrintLog(ADMOB_TAG, "NativeAds ID ==> $adNo null 222")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fl_adplaceholder.visibility = View.GONE
            Utility.PrintLog(ADMOB_TAG, "NativeAds ID ==> " + "Fail " + e.message)
        }
    }


    fun populateUnifiedNativeAdViewNoVideo(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        try {
            (adView.headlineView as TextView).text = nativeAd.headline
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }
        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon!!.drawable
            )
            (adView.iconView as ImageView).visibility = View.VISIBLE
        }
        if (nativeAd.price == null) {
            adView.priceView!!.visibility = View.INVISIBLE
        } else {
            adView.priceView!!.visibility = View.VISIBLE
            (adView.priceView as TextView).text = nativeAd.price
        }
        if (nativeAd.store == null) {
            adView.storeView!!.visibility = View.INVISIBLE
        } else {
            adView.storeView!!.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }
        if (nativeAd.starRating == null) {
            adView.starRatingView!!.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView!!.visibility = View.VISIBLE
        }
        if (nativeAd.advertiser == null) {
            adView.advertiserView!!.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView!!.visibility = View.VISIBLE
        }
        adView.storeView!!.visibility = View.GONE
        adView.priceView!!.visibility = View.GONE
        adView.setNativeAd(nativeAd)
    }

    // Load InterstitialAds
    /*onClick*/
    fun loadInterstitialAd(activity: Activity, intent: Intent?, isFinish: Boolean) {
        if (!isNetworkConnected(activity)) {
            doNext(activity, intent, isFinish)
            return
        }
        if (AdsHelperClass.getIsAdEnable() != 1) {
            doNext(activity, intent, isFinish)
            return
        }
        if (AdsHelperClass.ads_per_session == AdsHelperClass.getAdsPerSession()) {
            doNext(activity, intent, isFinish)
            return
        }
        if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
            loadAdmobInterstitialAd(activity, intent, isFinish)
        } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
            loadAdxInterstitialAd(activity, intent, isFinish)
        } else {
            doNext(activity, intent, isFinish)
        }
    }

    private fun loadAdmobInterstitialAd(activity: Activity, intent: Intent?, isFinish: Boolean) {
        if (AdsHelperClass.getShowInterstitial() == 1) {
            val adUnitId: String
            adUnitId = if (BuildConfig.DEBUG) getString(R.string.admob_interstitial_ads_id) else {
                AdsHelperClass.getInterstitialAd()
            }
            Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> $adUnitId")
            if (TextUtils.isEmpty(adUnitId)) {
                return
            }
            loadAdsDialog = Dialog(activity)
            loadAdsDialog!!.setContentView(R.layout.app_layout_loading)
            loadAdsDialog!!.setCanceledOnTouchOutside(false)
            loadAdsDialog!!.setCancelable(false)
            loadAdsDialog!!.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            loadAdsDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadAdsDialog!!.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
            loadAdsDialog!!.show()
            (loadAdsDialog!!.findViewById<View>(R.id.title) as TextView).text = "Loading Ads..."
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(this, adUnitId, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        Utility.PrintLog(ADMOB_TAG, "InterstitialAd ==> onAdLoaded")
                        if (loadAdsDialog != null && loadAdsDialog!!.isShowing) {
                            loadAdsDialog!!.dismiss()
                        }
                        displayInterstitialAds(activity, intent!!, isFinish)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error
                        Utility.PrintLog(ADMOB_TAG, "InterstitialAd ==> " + loadAdError.message)
                        mInterstitialAd = null
                        if (loadAdsDialog != null && loadAdsDialog!!.isShowing) {
                            loadAdsDialog!!.dismiss()
                        }
                        doNext(activity, intent, isFinish)
                    }
                })
        } else {
            doNext(activity, intent, isFinish)
        }
    }

    private fun loadAdxInterstitialAd(activity: Activity, intent: Intent?, isFinish: Boolean) {
        if (AdsHelperClass.getShowInterstitial() == 1) {
            val adUnitId: String
            adUnitId = if (BuildConfig.DEBUG) getString(R.string.adx_interstitial_ads_id) else {
                AdsHelperClass.getInterstitialAd()
            }
            Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> $adUnitId")
            if (TextUtils.isEmpty(adUnitId)) {
                return
            }
            loadAdsDialog = Dialog(activity)
            loadAdsDialog!!.setContentView(R.layout.app_layout_loading)
            loadAdsDialog!!.setCanceledOnTouchOutside(false)
            loadAdsDialog!!.setCancelable(false)
            loadAdsDialog!!.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            loadAdsDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadAdsDialog!!.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
            loadAdsDialog!!.show()
            (loadAdsDialog!!.findViewById<View>(R.id.title) as TextView).text = "Loading Ads..."
            val adManagerAdRequest = AdManagerAdRequest.Builder().build()
            AdManagerInterstitialAd.load(this, adUnitId, adManagerAdRequest,
                object : AdManagerInterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                        mAdManagerInterstitialAd = interstitialAd
                        Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==>" + "onAdLoaded")
                        if (loadAdsDialog != null && loadAdsDialog!!.isShowing) {
                            loadAdsDialog!!.dismiss()
                        }
                        displayInterstitialAds(activity, intent!!, isFinish)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error
                        Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> " + loadAdError.message)
                        mAdManagerInterstitialAd = null
                    }
                })
        } else {
            doNext(activity, intent, isFinish)
        }
    }

    /*onResultLaunch*/
    fun loadInterstitialAdResultLaunch(activity: Activity, resultLauncher: OnActivityResultLauncher) {
        if (!isNetworkConnected(activity)) {
            doNext(activity,resultLauncher)
            return
        }
        if (AdsHelperClass.getIsAdEnable() != 1) {
            doNext(activity, resultLauncher)
            return
        }
        if (AdsHelperClass.ads_per_session == AdsHelperClass.getAdsPerSession()) {
            doNext(activity,  resultLauncher)
            return
        }
        if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
            loadAdmobInterstitialAdResultLaunch(activity, resultLauncher)
        } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
            loadAdxInterstitialAdResultLaunch(activity,  resultLauncher)
        } else {
            doNext(activity,  resultLauncher)
        }
    }

    private fun loadAdmobInterstitialAdResultLaunch(activity: Activity,  resultLauncher: OnActivityResultLauncher) {
        if (AdsHelperClass.getShowInterstitial() == 1) {
            val adUnitId: String
            adUnitId = if (BuildConfig.DEBUG) getString(R.string.admob_interstitial_ads_id) else {
                AdsHelperClass.getInterstitialAd()
            }
            Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> $adUnitId")
            if (TextUtils.isEmpty(adUnitId)) {
                return
            }
            loadAdsDialog = Dialog(activity)
            loadAdsDialog!!.setContentView(R.layout.app_layout_loading)
            loadAdsDialog!!.setCanceledOnTouchOutside(false)
            loadAdsDialog!!.setCancelable(false)
            loadAdsDialog!!.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            loadAdsDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadAdsDialog!!.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
            loadAdsDialog!!.show()
            (loadAdsDialog!!.findViewById<View>(R.id.title) as TextView).text = "Loading Ads..."
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(this, adUnitId, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        Utility.PrintLog(ADMOB_TAG, "InterstitialAd ==> onAdLoaded")
                        if (loadAdsDialog != null && loadAdsDialog!!.isShowing) {
                            loadAdsDialog!!.dismiss()
                        }
                        displayInterstitialAdsResultLaunch(activity,  resultLauncher)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error
                        Utility.PrintLog(ADMOB_TAG, "InterstitialAd ==> " + loadAdError.message)
                        mInterstitialAd = null
                        if (loadAdsDialog != null && loadAdsDialog!!.isShowing) {
                            loadAdsDialog!!.dismiss()
                        }
                        doNext(activity, resultLauncher)
                    }
                })
        } else {
            doNext(activity,  resultLauncher)
        }
    }

    private fun loadAdxInterstitialAdResultLaunch(activity: Activity, resultLauncher: OnActivityResultLauncher) {
        if (AdsHelperClass.getShowInterstitial() == 1) {
            val adUnitId: String
            adUnitId = if (BuildConfig.DEBUG) getString(R.string.adx_interstitial_ads_id) else {
                AdsHelperClass.getInterstitialAd()
            }
            Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> $adUnitId")
            if (TextUtils.isEmpty(adUnitId)) {
                return
            }
            loadAdsDialog = Dialog(activity)
            loadAdsDialog!!.setContentView(R.layout.app_layout_loading)
            loadAdsDialog!!.setCanceledOnTouchOutside(false)
            loadAdsDialog!!.setCancelable(false)
            loadAdsDialog!!.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            loadAdsDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadAdsDialog!!.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
            loadAdsDialog!!.show()
            (loadAdsDialog!!.findViewById<View>(R.id.title) as TextView).text = "Loading Ads..."
            val adManagerAdRequest = AdManagerAdRequest.Builder().build()
            AdManagerInterstitialAd.load(this, adUnitId, adManagerAdRequest,
                object : AdManagerInterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                        mAdManagerInterstitialAd = interstitialAd
                        Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==>" + "onAdLoaded")
                        if (loadAdsDialog != null && loadAdsDialog!!.isShowing) {
                            loadAdsDialog!!.dismiss()
                        }
                        displayInterstitialAdsResultLaunch(activity,  resultLauncher)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error
                        Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> " + loadAdError.message)
                        mAdManagerInterstitialAd = null
                    }
                })
        } else {
            doNext(activity, resultLauncher)
        }
    }


    // Display InterstitialAds
    /*onClick*/
    fun displayInterstitialAds(activity: Activity, intent: Intent, isFinished: Boolean) {
        if (AdsHelperClass.getIsAdEnable() == 1 && AdsHelperClass.getShowInterstitial() == 1) {
            if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
                displayAdMobInterstitialAd(activity, intent, isFinished)
            } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
                displayAdxInterstitialAd(activity, intent, isFinished)
            } else {
                doNext(activity, intent, isFinished)
            }
        } else {
            doNext(activity, intent, isFinished)
        }
    }

    private fun displayAdMobInterstitialAd(activity: Activity, intent: Intent, isFinished: Boolean) {
        val count: Int = AdsHelperClass.getInterstitialAdsCount()
        if (count % AdsHelperClass.getInterstitialAdsClick() == 0
            && AdsHelperClass.ads_per_session != AdsHelperClass.getAdsPerSession()
        ) {
            if (mInterstitialAd != null) {
                Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> " + "Showed")
                mInterstitialAd!!.show(activity)
                AdsHelperClass.isShowingFullScreenAd = true
                AdsHelperClass.ads_per_session++
            } else {
                loadInterstitialAd(activity, intent, isFinished)
                AdsHelperClass.isShowingFullScreenAd = false
                return
            }
        } else {
            doNext(activity, intent, isFinished)
        }
        AdsHelperClass.setInterstitialAdsCount(count + 1)
        if (mInterstitialAd != null) {
            mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> The ad was dismissed.")
                    doNext(activity, intent, isFinished)
                    AdsHelperClass.isShowingFullScreenAd = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> The ad failed to show.")
                }

                override fun onAdShowedFullScreenContent() {
                    mInterstitialAd = null
                    Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> The ad was shown.")
                }
            }
        }
    }

    private fun displayAdxInterstitialAd(activity: Activity, intent: Intent, isFinished: Boolean) {
        val count: Int = AdsHelperClass.getInterstitialAdsCount()
        if (count % AdsHelperClass.getInterstitialAdsClick() == 0
            && AdsHelperClass.ads_per_session != AdsHelperClass.getAdsPerSession()
        ) {
            if (mAdManagerInterstitialAd != null) {
                Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> " + "Showed")
                mAdManagerInterstitialAd!!.show(activity)
                AdsHelperClass.isShowingFullScreenAd = true
                AdsHelperClass.ads_per_session++
            } else {
                loadInterstitialAd(activity, intent, isFinished)
                AdsHelperClass.isShowingFullScreenAd = false
                return
            }
        } else {
            doNext(activity, intent, isFinished)
        }
        AdsHelperClass.setInterstitialAdsCount(count + 1)
        if (mAdManagerInterstitialAd != null) {
            mAdManagerInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> The ad was dismissed.")
                    doNext(activity, intent, isFinished)
                    AdsHelperClass.isShowingFullScreenAd = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> The ad failed to show.")
                }

                override fun onAdShowedFullScreenContent() {
                    mAdManagerInterstitialAd = null
                    Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> The ad was shown.")
                }
            }
        }
    }


    /*onResultLaunch*/
    fun displayInterstitialAdsResultLaunch(activity: Activity, resultLauncher: OnActivityResultLauncher) {
        if (AdsHelperClass.getIsAdEnable() == 1 && AdsHelperClass.getShowInterstitial() == 1) {
            if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
                displayAdMobInterstitialAdResultLauncher(activity,  resultLauncher)
            } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
                displayAdxInterstitialAdResultLauncher(activity,  resultLauncher)
            } else {
                doNext(activity, resultLauncher)
            }
        } else {
            doNext(activity, resultLauncher)
        }
    }

    private fun displayAdMobInterstitialAdResultLauncher(activity: Activity, resultLauncher: OnActivityResultLauncher) {
        val count: Int = AdsHelperClass.getInterstitialAdsCount()
        if (count % AdsHelperClass.getInterstitialAdsClick() == 0
            && AdsHelperClass.ads_per_session != AdsHelperClass.getAdsPerSession()
        ) {
            if (mInterstitialAd != null) {
                Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> " + "Showed")
                mInterstitialAd!!.show(activity)
                AdsHelperClass.isShowingFullScreenAd = true
                AdsHelperClass.ads_per_session++
            } else {
                loadInterstitialAdResultLaunch(activity, resultLauncher)
                AdsHelperClass.isShowingFullScreenAd = false
                return
            }
        } else {
            doNext(activity, resultLauncher)
        }
        AdsHelperClass.setInterstitialAdsCount(count + 1)
        if (mInterstitialAd != null) {
            mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> The ad was dismissed.")
                    doNext(activity,  resultLauncher)
                    AdsHelperClass.isShowingFullScreenAd = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> The ad failed to show.")
                }

                override fun onAdShowedFullScreenContent() {
                    mInterstitialAd = null
                    Utility.PrintLog(ADMOB_TAG, "Admob InterstitialAd ==> The ad was shown.")
                }
            }
        }
    }

    private fun displayAdxInterstitialAdResultLauncher(activity: Activity, resultLauncher: OnActivityResultLauncher) {
        val count: Int = AdsHelperClass.getInterstitialAdsCount()
        if (count % AdsHelperClass.getInterstitialAdsClick() == 0
            && AdsHelperClass.ads_per_session != AdsHelperClass.getAdsPerSession()
        ) {
            if (mAdManagerInterstitialAd != null) {
                Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> " + "Showed")
                mAdManagerInterstitialAd!!.show(activity)
                AdsHelperClass.isShowingFullScreenAd = true
                AdsHelperClass.ads_per_session++
            } else {
                loadInterstitialAdResultLaunch(activity,  resultLauncher)
                AdsHelperClass.isShowingFullScreenAd = false
                return
            }
        } else {
            doNext(activity,  resultLauncher)
        }
        AdsHelperClass.setInterstitialAdsCount(count + 1)
        if (mAdManagerInterstitialAd != null) {
            mAdManagerInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> The ad was dismissed.")
                    doNext(activity, resultLauncher)
                    AdsHelperClass.isShowingFullScreenAd = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> The ad failed to show.")
                }

                override fun onAdShowedFullScreenContent() {
                    mAdManagerInterstitialAd = null
                    Utility.PrintLog(ADMOB_TAG, "Adx InterstitialAd ==> The ad was shown.")
                }
            }
        }
    }


    private fun doNext(activity: Activity, intent: Intent?, isFinished: Boolean) {
        if (intent != null) {
            activity.startActivity(intent)
        }
        if (isFinished) {
            activity.finish()
        }
    }

    private fun doNext(activity: Activity, resultLauncher: OnActivityResultLauncher) {
            resultLauncher.onLauncher()
    }

    // app open Ads
    fun loadOpenAppAdsOnSplash(activity: Activity, appCallBackOpenAppAds: AppOpenAdsListners) {
        if (AdsHelperClass.getIsAdEnable() != 1) {
            return
        }
        if (AdsHelperClass.getShowAppOpen() != 1) {
            return
        }
        if (!isNetworkConnected(activity)) {
            return
        }
        if (BuildConfig.DEBUG) {
            Utility.PrintLog("Ads: ", "Load Open App class")
            var adUnitId = ""
            adUnitId = if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADMOB)) {
                activity.getString(R.string.app_open_ads_id)
            } else if (AdsHelperClass.getAdType().equals(AdsHelperClass.AD_TYPE_ADX)) {
                activity.getString(R.string.adx_app_open_ads_id)
            } else {
                return
            }
            AppOpenAd.load(this, adUnitId, AdRequest.Builder().build(), AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appCallBackOpenAppAds.onAdLoad(true)
                    Utility.PrintLog("Ads ", "OpenApp loadOpenAppAdsOnSplash:  onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    appCallBackOpenAppAds.onAdLoad(false)
                    Utility.PrintLog("Ads ", "OpenApp loadOpenAppAdsOnSplash:  onAdFailedToLoad")
                }
            })
        } else {
            val adUnitId: String = AdsHelperClass.getAppOpenAd() ?: return
            if (adUnitId.isEmpty()) {
                return
            }
            Utility.PrintLog("Ads: ", "Load Open App class")
            AppOpenAd.load(this, adUnitId, AdRequest.Builder().build(), AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appCallBackOpenAppAds.onAdLoad(true)
                    Utility.PrintLog("Ads ", "OpenApp loadOpenAppAdsOnSplash:  onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    appCallBackOpenAppAds.onAdLoad(false)
                    Utility.PrintLog("Ads ", "OpenApp loadOpenAppAdsOnSplash:  onAdFailedToLoad")
                }
            })
        }
    }


}
