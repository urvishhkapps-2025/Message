package com.hkapps.messagepro.dialogs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.*

class StoragePickerDialog(
    val mActivity: BaseActivity, val mPath: String, val isShowRoot: Boolean, pickSingleOption: Boolean,
    val callback: (pickedPath: String) -> Unit
) {
    private val ID_INTERNAL = 1
    private val ID_SD = 2
    private val ID_OTG = 3
    private val ID_ROOT = 4

    private lateinit var mDialog: AlertDialog
    private lateinit var radioGroup: RadioGroup
    private var defaultSelectedId = 0
    private val availableStorages = ArrayList<String>()

    init {
        availableStorages.add(mActivity.internalStoragePath)
        when {
            mActivity.hasExternalSDCard() -> availableStorages.add(mActivity.sdCardPath)
            mActivity.hasOTGConnected() -> availableStorages.add("otg")
            isShowRoot -> availableStorages.add("root")
        }

        if (pickSingleOption && availableStorages.size == 1) {
            callback(availableStorages.first())
        } else {
            initDialog()
        }
    }

    private fun initDialog() {
        val inflater = LayoutInflater.from(mActivity)
        val resources = mActivity.resources
        val layoutParams = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val view = inflater.inflate(R.layout.dialog_radiogroup, null)
        radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
        val basePath = mPath.getBasePath(mActivity)

        val internalButton = inflater.inflate(R.layout.dialog_radiobutton, null) as RadioButton
        internalButton.apply {
            id = ID_INTERNAL
            text = resources.getString(R.string.internal)
            isChecked = basePath == context.internalStoragePath
            setOnClickListener { internalPicked() }
            if (isChecked) {
                defaultSelectedId = id
            }
        }
        radioGroup.addView(internalButton, layoutParams)

        if (mActivity.hasExternalSDCard()) {
            val sdButton = inflater.inflate(R.layout.dialog_radiobutton, null) as RadioButton
            sdButton.apply {
                id = ID_SD
                text = resources.getString(R.string.sd_card)
                isChecked = basePath == context.sdCardPath
                setOnClickListener { sdPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(sdButton, layoutParams)
        }

        if (mActivity.hasOTGConnected()) {
            val otgButton = inflater.inflate(R.layout.dialog_radiobutton, null) as RadioButton
            otgButton.apply {
                id = ID_OTG
                text = resources.getString(R.string.usb)
                isChecked = basePath == context.otgPath
                setOnClickListener { otgPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(otgButton, layoutParams)
        }

        if (isShowRoot) {
            val rootButton = inflater.inflate(R.layout.dialog_radiobutton, null) as RadioButton
            rootButton.apply {
                id = ID_ROOT
                text = resources.getString(R.string.root)
                isChecked = basePath == "/"
                setOnClickListener { rootPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(rootButton, layoutParams)
        }

        mDialog = AlertDialog.Builder(mActivity)
            .create().apply {
                mActivity.setupDialogStuff(view, this, R.string.select_storage)
            }
    }

    private fun internalPicked() {
        mDialog.dismiss()
        callback(mActivity.internalStoragePath)
    }

    private fun sdPicked() {
        mDialog.dismiss()
        callback(mActivity.sdCardPath)
    }

    private fun otgPicked() {
        mActivity.handleOTGPermission {
            if (it) {
                callback(mActivity.otgPath)
                mDialog.dismiss()
            } else {
                radioGroup.check(defaultSelectedId)
            }
        }
    }

    private fun rootPicked() {
        mDialog.dismiss()
        callback("/")
    }
}
