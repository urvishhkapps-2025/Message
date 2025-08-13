package com.hkapps.messagepro.fragment

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.hkapps.messagepro.extensions.getPermissionString
import com.hkapps.messagepro.extensions.hasPermission
import com.hkapps.messagepro.model.MessagesModel
import com.hkapps.messagepro.utils.isOnMainThread


open class BaseFragment : Fragment() {
    var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    var isAskingPermissions = false
    private val GENERIC_PERM_HANDLER = 100
    var mActivity: Activity? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity)
            mActivity = context
    }

    fun toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
        try {
            if (isOnMainThread()) {
                doToast(requireContext(), msg, length)
            } else {
                Handler(Looper.getMainLooper()).post {
                    doToast(requireContext(), msg, length)
                }
            }
        } catch (e: Exception) {
            Log.i("Exception", e.toString())
        }
    }

    private fun doToast(context: Context, message: String, length: Int) {
        if (context is Activity) {
            if (!context.isFinishing && !context.isDestroyed) {
                Toast.makeText(context, message, length).show()
            }
        } else {
            Toast.makeText(context, message, length).show()
        }
    }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (requireActivity().hasPermission(permissionId)) {
            callback(true)
        } else {
            isAskingPermissions = true
            actionOnPermission = callback
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(requireActivity().getPermissionString(permissionId)), GENERIC_PERM_HANDLER)
        }
    }

    fun checkIsInList(phoneNumber: String, messagesNew: ArrayList<MessagesModel>): Boolean {
        for (contactNumber in messagesNew) {
            if (phoneNumber.contains(contactNumber.participants[0].phoneNumbers.toString())) {
                return true
            }
        }
        return false
    }

}
