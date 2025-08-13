package com.hkapps.messagepro.dialogs

import android.app.Dialog
import android.content.ContextWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.hkapps.messagepro.extensions.getCurrentFormattedDateTime
import com.hkapps.messagepro.extensions.isAValidFilename
import com.hkapps.messagepro.extensions.toast
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.utils.config
import com.hkapps.messagepro.utils.EXPORT_FILE_EXT
import java.io.File
import java.util.*

class ExportDialog(
    private val mActivity: BaseHomeActivity,
    private val callback: (file: File) -> Unit,
) {
    val date = mActivity.getCurrentFormattedDateTime()

    private val config = mActivity.config

    val cw = ContextWrapper(mActivity)
    val directory: File = cw.filesDir

    init {

        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_conformation)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvExportCancel = dialog.findViewById<TextView>(R.id.tvExportCancel)
        val tvExportOk = dialog.findViewById<TextView>(R.id.tvExportOk)
        tvExportCancel.setOnClickListener {
            dialog.dismiss()
        }

        tvExportOk.setOnClickListener {
            val filename = "${mActivity.getString(R.string.messages)}_${date}"
            when {
                filename.isEmpty() -> mActivity.toast(R.string.empty_name)
                filename.isAValidFilename() -> {
                    val file = File(directory, "$filename$EXPORT_FILE_EXT")
                    if (file.exists()) {
                        mActivity.toast(R.string.name_taken)
                        return@setOnClickListener
                    }

                    /* if (!ckExportSMS.isChecked && !ckExportMMS.isChecked) {
                         mActivity.toast(R.string.no_option_selected)
                         return@setOnClickListener
                     }*/
                    val currentTime: Date = Calendar.getInstance().time
                    config.exportSms = true
                    config.lastExportDate = currentTime.toString().trim()
                    callback(file)
                    dialog.dismiss()
                }
                else -> mActivity.toast(R.string.invalid_name)
            }
        }

        if (!dialog.isShowing) dialog.show()

    }
}
