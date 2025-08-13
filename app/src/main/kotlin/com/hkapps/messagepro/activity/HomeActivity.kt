package com.hkapps.messagepro.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.hkapps.messagepro.BuildConfig
import com.hkapps.messagepro.MainAppClass.Companion.instance
import com.hkapps.messagepro.R
import com.hkapps.messagepro.ads.AdsHelperClass
import com.hkapps.messagepro.ads.SharedPrefrenceClass
import com.hkapps.messagepro.ads.native_ads_event.EventConstants
import com.hkapps.messagepro.ads.native_ads_event.EventListner
import com.hkapps.messagepro.ads.native_ads_event.NotifierFactoryApp
import com.hkapps.messagepro.databinding.ActivityHomeBinding
import com.hkapps.messagepro.extensions.appLaunched
import com.hkapps.messagepro.extensions.showErrorToast
import com.hkapps.messagepro.extensions.toast
import com.hkapps.messagepro.fragment.*
import com.hkapps.messagepro.utils.PERMISSION_READ_CONTACTS
import com.hkapps.messagepro.utils.PERMISSION_READ_SMS
import com.hkapps.messagepro.utils.PERMISSION_SEND_SMS
import com.hkapps.messagepro.utils.refreshMessages


class HomeActivity : BaseHomeActivity(), EventListner {

    var pagerAdapter: ViewPagerAdapter? = null
    var doubleBackToExitPressedOnce = false

    private lateinit var binding: ActivityHomeBinding

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            if (doubleBackToExitPressedOnce) {
                finish()
                return
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed(
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SharedPrefrenceClass.getInstance()!!.setBoolean("isSplash", false)
        binding.fabContactList.setOnClickListener {
            Intent(this, ContactsActivity::class.java).apply {
                startActivity(this)
            }
        }
        binding.tvSearch.setOnClickListener {
            val intent = Intent(this, SearchMessagesActivity::class.java)
            startActivity(
                intent,
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    binding.linSearch, getString(R.string.app_name)
                ).toBundle()
            )
        }
        binding.tvSetting1.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            instance?.displayInterstitialAds(this, intent, false)
        }
        binding.tvSetting2.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            instance?.displayInterstitialAds(this, intent, false)
        }

        appLaunched(BuildConfig.APPLICATION_ID)

        pagerAdapter = ViewPagerAdapter(supportFragmentManager, this)
        pagerAdapter!!.addFrag(AllSMSFragment().newInstance(), R.drawable.icon_tab_all, "All")
        pagerAdapter!!.addFrag(PersonalFragment().newInstance(), R.drawable.icon_tab_personal, "Personal")
        pagerAdapter!!.addFrag(OTPsFragment().newInstance(), R.drawable.icon_tag_otp, "OTPs")
        pagerAdapter!!.addFrag(TransactionsFragment().newInstance(), R.drawable.icon_tab_transactions, "Transactions")
        pagerAdapter!!.addFrag(OffersFragment().newInstance(), R.drawable.icon_tab_offers, "Offers")
        binding.viewPager.adapter = pagerAdapter

        binding.tabLayout.setTabRippleColorResource(android.R.color.transparent)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                Log.e("onTabSelected: ", "" + tab.position)
                setCurrentTab(tab.position)
                if (tab.position == 0) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as AllSMSFragment
                    fragmet.newInstance2()
                }

                if (tab.position == 1) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as PersonalFragment
                    fragmet.newInstance2()
                }
                if (tab.position == 2) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as OTPsFragment
                    fragmet.newInstance2()

                }
                if (tab.position == 3) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as TransactionsFragment
                    fragmet.newInstance2()

                }
                if (tab.position == 4) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as OffersFragment
                    fragmet.newInstance2()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            tab!!.customView = pagerAdapter!!.getTabView(i)
        }
        setCurrentTab(0)

        if (AdsHelperClass.getIsAdEnable() == 1 && AdsHelperClass.getShowNative() == 1) {
            registerAdsListener()
        }
        var fl_adplaceholder: FrameLayout = findViewById(R.id.fl_adplaceholder)
        instance!!.loadNativeAd(fl_adplaceholder, this, 0)

        inAppUpdate()

    }

    private val MAKE_DEFAULT_APP_REQUEST = 1
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            showErrorToast("Done")
                            toast("All Permission Given")
                            refreshMessages()
                        }
                    } else {
                        toast(getString(R.string.unknown_error_occurred))
                        finish()
                    }
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        }
    }

    fun registerAdsListener() {
        val notifier = NotifierFactoryApp.instance?.getNotifier(NotifierFactoryApp.EVENT_NOTIFIER_AD_STATUS)
        notifier?.registerListener(this, 1000)
    }

    override fun eventNotify(eventType: Int, eventObject: Any?): Int {
        Log.e("Update: ", "eventNotify")
        var eventState: Int = EventConstants.EVENT_IGNORED
        when (eventType) {
            EventConstants.EVENT_AD_LOADED_NATIVE -> {
                Log.e("Update: ", "Case")
                eventState = EventConstants.EVENT_PROCESSED
                this.runOnUiThread(object : Runnable {
                    override fun run() {
                        Handler((Looper.myLooper())!!).postDelayed(object : Runnable {
                            override fun run() {
                                var fl_adplaceholder: FrameLayout = findViewById(R.id.fl_adplaceholder)
                                instance!!.loadNativeAd(fl_adplaceholder, this@HomeActivity, 0)
                            }
                        }, 500)
                    }
                })
            }
        }
        return eventState
    }

    companion object {
        const val TAG2 = "AppUpdate"
        const val RC_APP_UPDATE = 11
    }

    override fun onStop() {
        if (mAppUpdateManager != null) {
            mAppUpdateManager!!.unregisterListener(installStateUpdatedListener)
        }
        super.onStop()
    }

    private fun inAppUpdate() {
        mAppUpdateManager = AppUpdateManagerFactory.create(this)
        mAppUpdateManager!!.registerListener(installStateUpdatedListener)
        mAppUpdateManager!!.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                Log.e(TAG2, "OnSuccess")
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE /*AppUpdateType.IMMEDIATE*/)
                ) {
                    try {
                        mAppUpdateManager!!.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this, RC_APP_UPDATE
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                } else {
                    Log.e(
                        TAG2, "checkForAppUpdateAvailability: something else"
                    )
                }
            }
    }

    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(
            binding.relParent!!,
            "New app is ready!",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Install") {
            if (mAppUpdateManager != null) {
                mAppUpdateManager!!.completeUpdate()
            }
        }
        snackbar.setActionTextColor(resources.getColor(R.color.whiteColor))
        snackbar.show()
    }


    override fun onResume() {
        super.onResume()
        try {
            mAppUpdateManager!!.appUpdateInfo
                .addOnSuccessListener { result ->
                    if (result.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        // If an in-app update is already running, resume the update.
                        try {
                            mAppUpdateManager!!.startUpdateFlowForResult(
                                result,
                                AppUpdateType.IMMEDIATE,
                                this, RC_APP_UPDATE
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            e.printStackTrace()
                        }
                    }
                }
            mAppUpdateManager!!.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate()
                    }
                }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    private var mAppUpdateManager: AppUpdateManager? = null
    var installStateUpdatedListener: InstallStateUpdatedListener =
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                } else if (state.installStatus() == InstallStatus.INSTALLED) {
                    if (mAppUpdateManager != null) {
                        mAppUpdateManager!!.unregisterListener(this)
                    }
                } else {
                    Log.i(
                        TAG2, "InstallStateUpdatedListener: state: " + state.installStatus()
                    )
                }
            }
        }

    inner class ViewPagerAdapter(manager: FragmentManager?, val conttex: Context) : FragmentPagerAdapter(manager!!) {
        private val mFragmentList: MutableList<Fragment> = ArrayList()
        private val mFragmentTitleList: MutableList<String> = ArrayList()
        private val mFragmentIconList: MutableList<Int> = ArrayList()


        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFrag(fragment: Fragment, icon: Int, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
            mFragmentIconList.add(icon)
        }

        fun getTabView(position: Int): View {
            val tabView: View = LayoutInflater.from(conttex).inflate(R.layout.layout_home_tabs, null)
            (tabView.findViewById<View>(R.id.tvTopTitle) as TextView).text = mFragmentTitleList[position]
            (tabView.findViewById<View>(R.id.tvTopTitle) as TextView)
                .setTextColor(resources.getColor(R.color.text_only_blue))
            Glide.with(this@HomeActivity).load(mFragmentIconList[position])
                .into((tabView.findViewById<View>(R.id.ivTabThumb) as ImageView))
            (tabView.findViewById<View>(R.id.tvTopTitle) as TextView)
                .setTextColor(resources.getColor(R.color.text_only_blue))
            (tabView.findViewById<View>(R.id.linTabParent) as LinearLayout)
                .setBackgroundResource(R.drawable.logo_selection_holder)

            return tabView
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }
    }


    private fun setCurrentTab(tabPos: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val tabView: View = binding.tabLayout.getTabAt(i)!!.customView!!
            val tvTopTitle = tabView.findViewById<TextView>(R.id.tvTopTitle)
            val linTabParent = tabView.findViewById<LinearLayout>(R.id.linTabParent)
            val ivTabThumb = tabView.findViewById<ImageView>(R.id.ivTabThumb)

            if (i == tabPos) {
                tvTopTitle.setTextColor(resources.getColor(R.color.text_only_blue))
                ivTabThumb.setColorFilter(resources.getColor(R.color.text_only_blue), android.graphics.PorterDuff.Mode.SRC_ATOP)
                linTabParent.setBackgroundResource(R.drawable.logo_selection_holder)
            } else {
                tvTopTitle.setTextColor(resources.getColor(R.color.text_grey))
                ivTabThumb.setColorFilter(resources.getColor(R.color.text_grey), android.graphics.PorterDuff.Mode.SRC_ATOP)
                linTabParent.setBackgroundResource(R.drawable.tab_no_selection)
            }
            binding.tabLayout.getTabAt(i)!!.customView = tabView
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.e("Event: ", "askPermissions RESULT_OK")
                askPermissions()
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        } else if (requestCode == RC_APP_UPDATE) {
            if (resultCode != AppCompatActivity.RESULT_OK) {
                Log.e(
                    TAG2,
                    "onActivityResult: app download failed"
                )
            }
        }
    }

}

