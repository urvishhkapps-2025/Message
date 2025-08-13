package com.hkapps.messagepro.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.hkapps.messagepro.BuildConfig
import com.hkapps.messagepro.ads.SharedPrefrenceClass.Companion.getInstance

object Utility {

    @JvmStatic
    fun checkNotificationPermission(
        activity: Activity,
    ): Boolean {
        return if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    @JvmStatic
    fun showSnackBar(activity: Activity, msg: String?, view: View) {
        if (!activity.isFinishing) {
            Snackbar.make(
                view,
                msg!!,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    @JvmStatic
    fun PrintLog(key: String?, log: String?) {
        if (BuildConfig.DEBUG) {
            Log.e(key, log!!)
        }
    }

    @JvmStatic
    fun decodeSampledBitmapFromResource(
        res: Resources?, resId: Int,
        reqWidth: Int, reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        //        AppUtils.printLog("decodeSampled Method", "Done.....");
        return BitmapFactory.decodeResource(res, resId, options)
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val mHeight = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (mHeight > reqHeight || width > reqWidth) {
            val mHalfHeight = mHeight / 2
            val mHalfWidth = width / 2
            while (mHalfHeight / inSampleSize >= reqHeight
                && mHalfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

//        AppUtils.printLog("Calculate Size Method", "Done........");
        return inSampleSize
    }

    @JvmStatic
    fun getIsIntroShow(): Boolean {
        return getInstance()!!.getBoolean("INTRO_SHOW", false)
    }

    @JvmStatic
    fun setIsIntroShow(key: Boolean) {
        getInstance()!!.setBoolean("INTRO_SHOW", key)
    }

}
