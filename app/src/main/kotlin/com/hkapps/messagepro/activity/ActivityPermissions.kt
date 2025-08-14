package com.hkapps.messagepro.activity

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.hkapps.messagepro.R
import com.hkapps.messagepro.fragment.PermissionFragment

class ActivityPermissions : AppCompatActivity() {


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_introduction)
        onBackPress()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.contentContainer, PermissionFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun onBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

}
