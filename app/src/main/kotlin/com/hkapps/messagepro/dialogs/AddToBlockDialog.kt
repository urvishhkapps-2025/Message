package com.hkapps.messagepro.dialogs

import android.app.Dialog
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.hkapps.messagepro.extensions.addBlockedNumber
import com.hkapps.messagepro.extensions.deleteBlockedNumber
import com.hkapps.messagepro.extensions.value
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.model.BlockedNumberModel

class AddToBlockDialog(val mActivity: BaseActivity, val originalNumber: BlockedNumberModel? = null, val callback: () -> Unit) {

    init {
        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_add_to_block)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<EditText>(R.id.etAddNumber).apply {
            requestFocus()
            if (originalNumber != null) {
                setText(originalNumber.number)
            }
        }

        dialog.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.tvOk).setOnClickListener {
            val newBlockedNumber = dialog.findViewById<EditText>(R.id.etAddNumber).value
            if (originalNumber != null && newBlockedNumber != originalNumber.number) {
                mActivity.deleteBlockedNumber(originalNumber.number)
            }

            if (newBlockedNumber.isNotEmpty()) {
                mActivity.addBlockedNumber(newBlockedNumber)
            }

            callback()
            dialog.dismiss()
        }
        if (!dialog.isShowing) dialog.show()
    }
}
