package com.hkapps.messagepro.activity

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hkapps.messagepro.MainAppClass.Companion.instance
import com.hkapps.messagepro.R
import com.hkapps.messagepro.databinding.ActivitySettingsBinding
import com.hkapps.messagepro.dialogs.RadioButtonsDialog
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.model.RadioModel
import com.hkapps.messagepro.utils.*
import java.util.Locale


class SettingsActivity : BaseHomeActivity() {
    private var blockedNumbersAtPause = -1
    var appTopToolbar: Toolbar? = null

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        appTopToolbar = findViewById(R.id.appTopToolbar)
        setSupportActionBar(appTopToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.title = resources.getString(R.string.settings)
        appTopToolbar?.navigationIcon = ContextCompat.getDrawable(this, R.drawable.icon_back)


        instance!!.loadBanner(binding.adViewBanner, this)
    }


    override fun onResume() {
        super.onResume()

        setupPurchaseThankYou()
        setupCustomizeNotifications()
        setupBackupAndRestore()
        setupTheme()
        setupUseEnglish()
        setupManageBlockedNumbers()
        setupChangeDateTimeFormat()
        setupFontSize()
        setupShowCharacterCounter()
        setupUseSimpleCharacters()
        setupEnableDeliveryReports()
        setupLockScreenVisibility()
        setupMMSFileSizeLimit()
        setPolicyAndRate()

        if (blockedNumbersAtPause != -1 && blockedNumbersAtPause != getBlockedNumbers().hashCode()) {
            refreshMessages()
        }
    }

    private fun setPolicyAndRate() {
        binding.rlPolicy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, "https://hkappsblog.blogspot.com/2023/05/privacy-policy-for-message-pro-sms.html".toUri())
            startActivity(browserIntent)
        }
        binding.rlRateUs.setOnClickListener {
            launchMarket()
        }
    }

    private fun launchMarket() {
        val uri = "market://details?id=$packageName".toUri()
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Unable to find app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        blockedNumbersAtPause = getBlockedNumbers().hashCode()
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupPurchaseThankYou() {
        binding.rlLanguage.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupCustomizeNotifications() {
        binding.rlCustomNotifications.beVisibleIf(isOreoPlus())

        if (binding.rlCustomNotifications.isGone()) {
            binding.rlLockScreen.background = resources.getDrawable(R.drawable.ripple_all_corners, theme)
        }

        binding.rlCustomNotifications.setOnClickListener {
            gotoSystenNotificationsSetting()
        }
    }

    private fun setupBackupAndRestore() {
        binding.rlBackupandRestore.setOnClickListener {
            gotoBackupRestore()
        }
    }


    fun gotoBackupRestore() {
        val intent = Intent(this, BackupRestoreActivity::class.java)
        instance?.displayInterstitialAds(this, intent, false)

    }


    fun gotoSystenNotificationsSetting() {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(this)
        }
    }

    private fun setupUseEnglish() {
        binding.rlLanguage.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        binding.switchLanguage.isChecked = config.useEnglish



        binding.rlLanguage.setOnClickListener {
            binding.switchLanguage.toggle()
            config.useEnglish = binding.switchLanguage.isChecked
            System.exit(0)
        }
    }

    private fun setupTheme() {
        binding.switchTheme.isChecked = mPref.appTheme != AppCompatDelegate.MODE_NIGHT_NO

        binding.rlTheme.setOnClickListener {
            when (mPref.appTheme) {
                AppCompatDelegate.MODE_NIGHT_YES -> {
                    mPref.appTheme = AppCompatDelegate.MODE_NIGHT_NO
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }

                AppCompatDelegate.MODE_NIGHT_NO -> {
                    mPref.appTheme = AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
            binding.switchTheme.toggle()
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setupManageBlockedNumbers() {
        binding.rlBlockedNumber.beVisibleIf(isNougatPlus())
        binding.rlBlockedNumber.setOnClickListener {
            startActivity(Intent(this, BlockedNumberActivity::class.java))
        }
    }

    private fun setupChangeDateTimeFormat() {
        binding.switchTimeFormate.isChecked = mPref.use24HourFormat
        binding.switchTimeFormate.setOnClickListener {
            mPref.use24HourFormat = binding.switchTimeFormate.isChecked
            Log.e("Event: ", " setupChangeDateTimeFormat")
            refreshMessages()
        }
    }

    private fun setupFontSize() {
        binding.tvFontSizeDesc.text = getFontSizeText()
        binding.rlFontSize.setOnClickListener {
            val items = arrayListOf(
                RadioModel(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioModel(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioModel(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioModel(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioButtonsDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                binding.tvFontSizeDesc.text = getFontSizeText()
            }
        }
    }

    private fun setupShowCharacterCounter() {
        val aa = config.showCharacterCounter
        binding.switchCharacterCoundown.isChecked = aa
        binding.rlCharacterCoundown.setOnClickListener {
            binding.switchCharacterCoundown.toggle()
            val bb = binding.switchCharacterCoundown.isChecked
            config.showCharacterCounter = bb
        }
    }

    private fun setupUseSimpleCharacters() {
        binding.switchDiacritic.isChecked = config.useSimpleCharacters
        binding.rlDiacritic.setOnClickListener {
            binding.switchDiacritic.toggle()
            config.useSimpleCharacters = binding.switchDiacritic.isChecked
        }
    }

    private fun setupEnableDeliveryReports() {
        binding.switchDeliveryReport.isChecked = config.enableDeliveryReports
        binding.rlDeliveryReport.setOnClickListener {
            binding.switchDeliveryReport.toggle()
            config.enableDeliveryReports = binding.switchDeliveryReport.isChecked
        }
    }

    private fun setupLockScreenVisibility() {
        binding.tvLockScreenDesc.text = getLockScreenVisibilityText()
        binding.rlLockScreen.setOnClickListener {
            val items = arrayListOf(
                RadioModel(LOCK_SCREEN_SENDER_MESSAGE, getString(R.string.sender_and_message)),
                RadioModel(LOCK_SCREEN_SENDER, getString(R.string.sender_only)),
                RadioModel(LOCK_SCREEN_NOTHING, getString(R.string.nothing)),
            )

            RadioButtonsDialog(this@SettingsActivity, items, config.lockScreenVisibilitySetting) {
                config.lockScreenVisibilitySetting = it as Int
                binding.tvLockScreenDesc.text = getLockScreenVisibilityText()
            }
        }
    }

    private fun getLockScreenVisibilityText() = getString(
        when (config.lockScreenVisibilitySetting) {
            LOCK_SCREEN_SENDER_MESSAGE -> R.string.sender_and_message
            LOCK_SCREEN_SENDER -> R.string.sender_only
            else -> R.string.nothing
        }
    )

    private fun setupMMSFileSizeLimit() {
        binding.tvMMSFileSizeDesc.text = getMMSFileLimitText()
        binding.rlMMSFileSize.setOnClickListener {
            val items = arrayListOf(
                RadioModel(1, getString(R.string.mms_file_size_limit_100kb), FILE_SIZE_100_KB),
                RadioModel(2, getString(R.string.mms_file_size_limit_200kb), FILE_SIZE_200_KB),
                RadioModel(3, getString(R.string.mms_file_size_limit_300kb), FILE_SIZE_300_KB),
                RadioModel(4, getString(R.string.mms_file_size_limit_600kb), FILE_SIZE_600_KB),
                RadioModel(5, getString(R.string.mms_file_size_limit_1mb), FILE_SIZE_1_MB),
                RadioModel(6, getString(R.string.mms_file_size_limit_2mb), FILE_SIZE_2_MB),
                RadioModel(7, getString(R.string.mms_file_size_limit_none), FILE_SIZE_NONE),
            )

            val checkedItemId = items.find { it.value == config.mmsFileSizeLimit }?.id ?: 7
            RadioButtonsDialog(this@SettingsActivity, items, checkedItemId) {
                config.mmsFileSizeLimit = it as Long
                binding.tvMMSFileSizeDesc.text = getMMSFileLimitText()
            }
        }
    }

    private fun getMMSFileLimitText() = getString(
        when (config.mmsFileSizeLimit) {
            FILE_SIZE_100_KB -> R.string.mms_file_size_limit_100kb
            FILE_SIZE_200_KB -> R.string.mms_file_size_limit_200kb
            FILE_SIZE_300_KB -> R.string.mms_file_size_limit_300kb
            FILE_SIZE_600_KB -> R.string.mms_file_size_limit_600kb
            FILE_SIZE_1_MB -> R.string.mms_file_size_limit_1mb
            FILE_SIZE_2_MB -> R.string.mms_file_size_limit_2mb
            else -> R.string.mms_file_size_limit_none
        }
    )

}
