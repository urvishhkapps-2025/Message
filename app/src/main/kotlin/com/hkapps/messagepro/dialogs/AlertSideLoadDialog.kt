package com.hkapps.messagepro.dialogs

import android.app.Activity
import android.app.Dialog
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.hkapps.messagepro.extensions.getStringsPackageName
import com.hkapps.messagepro.extensions.launchViewIntent
import com.hkapps.messagepro.R

class AlertSideLoadDialog(val mActivity: Activity, val callback: () -> Unit) {
    private val url = "https://play.google.com/store/apps/details?id=${mActivity.getStringsPackageName()}"

    init {
        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_alert)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvAlertTitle = dialog.findViewById<TextView>(R.id.tvAlertTitle)
        val tvAlertDesc = dialog.findViewById<TextView>(R.id.tvAlertDesc)
        val tvAlertCancel = dialog.findViewById<TextView>(R.id.tvAlertCancel)
        val tvAlertOk = dialog.findViewById<TextView>(R.id.tvAlertOk)

        val text = String.format(mActivity.getString(R.string.sideloaded_app), url)
        tvAlertDesc.text = Html.fromHtml(text)
        tvAlertDesc.movementMethod = LinkMovementMethod.getInstance()


        tvAlertCancel.setOnClickListener {
            dialog.dismiss()
            callback()
        }

        tvAlertOk.setOnClickListener {
            dialog.dismiss()
            downloadApp()
        }
        if (!dialog.isShowing) dialog.show()
    }

    private fun downloadApp() {
        mActivity.launchViewIntent(url)
    }

}
