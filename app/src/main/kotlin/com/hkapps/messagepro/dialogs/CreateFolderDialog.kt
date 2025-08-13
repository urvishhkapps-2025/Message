package com.hkapps.messagepro.dialogs

import android.app.Dialog
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import java.io.File

class CreateFolderDialog(val mActivity: BaseActivity, val path: String, val callback: (path: String) -> Unit) {
    init {
        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_make_folder)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvCreateNewFolderOk = dialog.findViewById<TextView>(R.id.tvCreateNewFolderOk)
        val tvCreateNewFolderCancel = dialog.findViewById<TextView>(R.id.tvCreateNewFolderCancel)
        val tvCreateNewFolderPath = dialog.findViewById<TextView>(R.id.tvCreateNewFolderPath)
        val etFolderName = dialog.findViewById<EditText>(R.id.etFolderName)


        tvCreateNewFolderPath.text = "${mActivity.humanizePath(path).trimEnd('/')}/"
        mActivity.showKeyboard(etFolderName)

        tvCreateNewFolderCancel.setOnClickListener {
            dialog.dismiss()
        }

        tvCreateNewFolderOk.setOnClickListener {
            dialog.dismiss()
            val name = etFolderName.value
            when {
                name.isEmpty() -> mActivity.toast(R.string.empty_name)
                name.isAValidFilename() -> {
                    val file = File(path, name)
                    if (file.exists()) {
                        mActivity.toast(R.string.name_taken)
                        return@setOnClickListener
                    }

                    createFolder("$path/$name", dialog)
                }
                else -> mActivity.toast(R.string.invalid_name)
            }
        }

        if (!dialog.isShowing) dialog.show()
    }

    private fun createFolder(path: String, dialog: Dialog) {
        try {
            when {
                mActivity.isRestrictedSAFOnlyRoot(path) && mActivity.createAndroidSAFDirectory(path) -> sendSuccess(dialog, path)
                mActivity.needsStupidWritePermissions(path) -> mActivity.handleSAFDialog(path) {
                    if (it) {
                        try {
                            val documentFile = mActivity.getDocumentFile(path.getParentPath())
                            val newDir = documentFile?.createDirectory(path.getFilenameFromPath()) ?: mActivity.getDocumentFile(path)
                            if (newDir != null) {
                                sendSuccess(dialog, path)
                            } else {
                                mActivity.toast(R.string.unknown_error_occurred)
                            }
                        } catch (e: SecurityException) {
                            mActivity.showErrorToast(e)
                        }
                    }
                }
                File(path).mkdirs() -> sendSuccess(dialog, path)
                else -> mActivity.toast(R.string.unknown_error_occurred)
            }
        } catch (e: Exception) {
            mActivity.showErrorToast(e)
        }
    }

    private fun sendSuccess(dialog: Dialog, path: String) {
        callback(path.trimEnd('/'))
        dialog.dismiss()
    }
}
