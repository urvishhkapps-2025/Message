package com.hkapps.messagepro.activity

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
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

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.contentContainer, PermissionFragment())
        transaction.addToBackStack(null)
        transaction.commit()

    }

    override fun onBackPressed() {
        finish()
    }

}
