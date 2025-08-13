package com.hkapps.messagepro.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.hkapps.messagepro.R
import com.hkapps.messagepro.utils.Utility

class ActivityNotificationPermissions : AppCompatActivity() {

    var textView: TextView? = null
    var textView1: TextView? = null
    var tvSetAsDefault: TextView? = null
    var anim_out: Animation? = null
    var anim_in: Animation? = null
    var tvSkip: TextView? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_notification_permission)


        onBackPress()

        tvSkip = findViewById(R.id.tvSkip)
        tvSetAsDefault = findViewById(R.id.tvSetAsDefault)
        textView = findViewById(R.id.tvHead)
        textView1 = findViewById(R.id.tvStr)
        textView?.text = getString(R.string.text_header_notification)
        textView1?.text = getString(R.string.text_desc_notification)
        doAnimTitleSubTitle()

        tvSetAsDefault?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        tvSkip?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) {
                Utility.showSnackBar(
                    this,
                    getString(R.string.txt_please_grant_notification_permission_from_app_settings),
                    findViewById<View>(android.R.id.content).rootView,
                )
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }


    private fun doAnimTitleSubTitle() {
        textView!!.clearAnimation()
        textView1!!.clearAnimation()
        tvSetAsDefault!!.clearAnimation()

        anim_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        anim_out?.repeatCount = Animation.ABSOLUTE
        anim_out?.duration = 1500

        textView!!.animation = anim_out
        textView1!!.animation = anim_out
        tvSetAsDefault!!.animation = anim_out

        anim_in = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        anim_in?.repeatCount = Animation.ABSOLUTE
        anim_in?.duration = 1500
        anim_in?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                textView!!.startAnimation(anim_out)
                textView1!!.startAnimation(anim_out)
                tvSetAsDefault!!.startAnimation(anim_out)

            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }


    private fun onBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@ActivityNotificationPermissions, HomeActivity::class.java))
                finish()
            }
        })
    }


}
