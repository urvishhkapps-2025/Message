package com.hkapps.messagepro.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtils {

    companion object{
        fun isPermissionGranted_R_W(activity: Activity?): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val readPermission = ContextCompat.checkSelfPermission(
                    activity!!, Manifest.permission.READ_EXTERNAL_STORAGE
                )
                (readPermission == PackageManager.PERMISSION_GRANTED)
            } else {
                val readPermission = ContextCompat.checkSelfPermission(
                    activity!!, Manifest.permission.READ_EXTERNAL_STORAGE
                )
                val writePermission = ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
            }
        }

        fun takePermission_R_W(activity: Activity?, request_code: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    activity!!,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    request_code
                )
            } else {
                ActivityCompat.requestPermissions(
                    activity!!,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    request_code
                )
            }
        }
    }
}
