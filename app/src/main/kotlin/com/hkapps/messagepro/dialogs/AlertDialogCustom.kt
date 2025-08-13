package com.hkapps.messagepro.dialogs

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.hkapps.messagepro.R

class AlertDialogCustom(
    mActivity: Activity, message: String = "", messageId: Int = R.string.proceed_with_deletion,
    val callback: () -> Unit
) {

    init {
        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_alert)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvAlertTitle = dialog.findViewById<TextView>(R.id.tvAlertTitle)
        val tvAlertDesc = dialog.findViewById<TextView>(R.id.tvAlertDesc)
        val tvAlertCancel = dialog.findViewById<TextView>(R.id.tvAlertCancel)
        val tvAlertOk = dialog.findViewById<TextView>(R.id.tvAlertOk)

        tvAlertDesc.text = if (message.isEmpty()) mActivity.resources.getString(messageId) else message

        tvAlertCancel.setOnClickListener {
            dialog.dismiss()
        }

        tvAlertOk.setOnClickListener {
            dialog.dismiss()
            callback()
        }
        if (!dialog.isShowing) dialog.show()
    }
}
