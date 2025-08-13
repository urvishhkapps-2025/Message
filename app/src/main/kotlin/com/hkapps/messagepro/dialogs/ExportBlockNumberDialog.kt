package com.hkapps.messagepro.dialogs

import android.app.Dialog
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.*

import com.hkapps.messagepro.utils.*
import java.io.File

class ExportBlockNumberDialog(
    val mActivity: BaseActivity,
    val path: String,
    val hidePath: Boolean,
    callback: (file: File) -> Unit,
) {
    private var realPath = if (path.isEmpty()) mActivity.internalStoragePath else path
    private val config = mActivity.mPref

    init {

        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_export_blocked_no)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvExportBlockDisc = dialog.findViewById<TextView>(R.id.tvExportBlockDisc)
        val tvExportBlockTitle = dialog.findViewById<TextView>(R.id.tvExportBlockTitle)
        val tvExportBlockCancel = dialog.findViewById<TextView>(R.id.tvExportBlockCancel)
        val tvExportBlockOk = dialog.findViewById<TextView>(R.id.tvExportBlockOk)
        val rtExportBlockFileName = dialog.findViewById<EditText>(R.id.rtExportBlockFileName)

        tvExportBlockDisc.text = mActivity.humanizePath(realPath)
        rtExportBlockFileName.setText("${mActivity.getString(R.string.blocked_numbers)}_${mActivity.getCurrentFormattedDateTime()}")

        if (hidePath) {
            tvExportBlockTitle.beGone()
            tvExportBlockDisc.beGone()
        } else {
            tvExportBlockDisc.setOnClickListener {
                FilePickerDialog(mActivity, realPath, false, showFAB = true) {
                    tvExportBlockDisc.text = mActivity.humanizePath(it)
                    realPath = it
                }
            }
        }


        tvExportBlockOk.setOnClickListener {
            val filename = rtExportBlockFileName.value
            when {
                filename.isEmpty() -> mActivity.toast(R.string.empty_name)
                filename.isAValidFilename() -> {
                    val file = File(realPath, "$filename$BLOCKED_NUMBERS_EXPORT_EXTENSION")
                    if (!hidePath && file.exists()) {
                        mActivity.toast(R.string.name_taken)
                        return@setOnClickListener
                    }

                    ensureBackgroundThread {
                        config.lastBlockedNumbersExportPath = file.absolutePath.getParentPath()
                        callback(file)
                        dialog.dismiss()
                    }
                }
                else -> mActivity.toast(R.string.invalid_name)
            }
        }


        tvExportBlockCancel.setOnClickListener {
            dialog.dismiss()
        }

        if (!dialog.isShowing) dialog.show()
    }
}
