package com.hkapps.messagepro.ads;

public class AdsHelperClass {

    public static final String MY_PREFERANCE = "my_preferance";
    //ads
    public static final String AD_TYPE_ADMOB = "admob";
    public static final String AD_TYPE_ADX = "adx";

    public static final String BANNER_APP = "banner_ad";
    public static final String SHOW_BANNER = "show_banner";

    public static final String INTERSTITIAL_AD = "interstitial_ad";
    public static final String SHOW_INTERSTITIAL = "show_interstitial";
    private static final String INTERSTITIAL_ADS_COUNT = "interstitial_ads_count";

    public static final String NATIVE_ADS = "native_ad_1";
    public static final String SHOW_NATIVE = "show_native";

    public static final String APP_OPEN_AD = "app_open_ad";
    public static final String SHOW_APP_OPEN = "show_app_open";
    public static final String APP_OPEN_COUNT = "app_open_count";

    public static final String ID = "id";
    public static final String AD_TYPE = "ad_type";
    public static final String IS_AD_ENABLE = "is_ad_enable";
    public static final String IN_APP_REVIEW = "in_appreview";
    public static final String ADS_PER_SESSION = "ads_per_session";
    private static final String INTERSTITIAL_ADS_CLICK = "interstitial_ads_click";
    private static final String GIVE_REVIEW_SUCCESS = "give_review_success";
    private static final String REVIEW_COUNT = "review_count";
    private static final String IS_SPLASH_ON = "is_splash_on";
    private static final String SPLASH_TIME = "splash_time";
    private static final String REMOTE = "remote";

    //other
    public static boolean isShowingFullScreenAd = false;
    public static int ads_per_session = 0;
    public static final String OPENADS_SHOWED_COUNT = "openads_showed_count";

    public static int getInterstitialAdsCount() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(INTERSTITIAL_ADS_COUNT, 0);
    }

    public static void setInterstitialAdsCount(int i) {
        SharedPrefrenceClass.Companion.getInstance().setInt(INTERSTITIAL_ADS_COUNT, i);
    }

    public static int getIsAdEnable() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(IS_AD_ENABLE, 0);
    }

    public static void setIsAdEnable(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(IS_AD_ENABLE, key);
    }

    public static String getAdType() {
        return SharedPrefrenceClass.Companion.getInstance().getString(AD_TYPE, "");
    }

    public static void setAdType(String key) {
        SharedPrefrenceClass.Companion.getInstance().setString(AD_TYPE, key);
    }

    public static int getId() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(ID, 0);
    }

    public static void setId(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(ID, key);
    }

    public static int getInAppreview() {
       return SharedPrefrenceClass.Companion.getInstance().getInt(IN_APP_REVIEW,0);
    }

    public static void setInAppreview(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(IN_APP_REVIEW, key);
    }



    public static int getInterstitialAdsClick() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(INTERSTITIAL_ADS_CLICK, 0);
    }

    public static void setInterstitialAdsClick(int i) {
        SharedPrefrenceClass.Companion.getInstance().setInt(INTERSTITIAL_ADS_CLICK, i);
    }

    public static int getAdsPerSession() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(ADS_PER_SESSION, 0);
    }

    public static void setAdsPerSession(int i) {
        SharedPrefrenceClass.Companion.getInstance().setInt(ADS_PER_SESSION, i);
    }


    public static int getShowAppOpen() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(SHOW_APP_OPEN, 0);
    }

    public static void setShowAppOpen(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(SHOW_APP_OPEN, key);
    }

    public static int getAppOpenCount() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(APP_OPEN_COUNT, 0);
    }

    public static void setAppOpenCount(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(APP_OPEN_COUNT, key);
    }

    public static int getShowBanner() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(SHOW_BANNER, 0);
    }

    public static void setShowBanner(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(SHOW_BANNER, key);
    }

    public static int getShowInterstitial() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(SHOW_INTERSTITIAL, 0);
    }

    public static void setShowInterstitial(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(SHOW_INTERSTITIAL, key);
    }

    public static int getShowNative() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(SHOW_NATIVE, 0);
    }

    public static void setShowNative(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(SHOW_NATIVE, key);
    }

    public static String getBannerAd() {
        return SharedPrefrenceClass.Companion.getInstance().getString(BANNER_APP, "");
    }

    public static void setBannerAd(String key) {
        SharedPrefrenceClass.Companion.getInstance().setString(BANNER_APP, key);
    }

    public static String getAppOpenAd() {
        return SharedPrefrenceClass.Companion.getInstance().getString(APP_OPEN_AD, "");
    }

    public static void setAppOpenAd(String key) {
        SharedPrefrenceClass.Companion.getInstance().setString(APP_OPEN_AD, key);
    }

    public static String getInterstitialAd() {
        return SharedPrefrenceClass.Companion.getInstance().getString(INTERSTITIAL_AD, "");
    }

    public static void setInterstitialAd(String key) {
        SharedPrefrenceClass.Companion.getInstance().setString(INTERSTITIAL_AD, key);
    }

    public static String getRemote() {
        return SharedPrefrenceClass.Companion.getInstance().getString(REMOTE, "");
    }

    public static void setRemote(String key) {
        SharedPrefrenceClass.Companion.getInstance().setString(REMOTE, key);
    }

    public static int getReviewCount() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(REVIEW_COUNT, 0);
    }

    public static void setReviewCount(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(REVIEW_COUNT, key);
    }

    public static boolean getReviewSuccess() {
        return SharedPrefrenceClass.Companion.getInstance().getBoolean(GIVE_REVIEW_SUCCESS, false);
    }

    public static void setReviewSuccess(boolean key) {
        SharedPrefrenceClass.Companion.getInstance().setBoolean(GIVE_REVIEW_SUCCESS, key);
    }

    public static int getIsSplashOn() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(IS_SPLASH_ON, 0);
    }

    public static void setIsSplashOn(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(IS_SPLASH_ON, key);
    }

    public static int getSplashTime() {
        return SharedPrefrenceClass.Companion.getInstance().getInt(SPLASH_TIME, 0);
    }

    public static void setSplashTime(int key) {
        SharedPrefrenceClass.Companion.getInstance().setInt(SPLASH_TIME, key);
    }

    public static int getOpenAdsShowedCount() {
        int type = SharedPrefrenceClass.Companion.getInstance().getInt(OPENADS_SHOWED_COUNT, 1);
        return type;
    }

    public static void setOpenAdsShowedCount(int str) {
        SharedPrefrenceClass.Companion.getInstance().setInt(OPENADS_SHOWED_COUNT, str);
    }

}
