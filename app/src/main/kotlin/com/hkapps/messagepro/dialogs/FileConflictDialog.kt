package com.hkapps.messagepro.dialogs

import android.app.Activity
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.hkapps.messagepro.R
import com.hkapps.messagepro.R.id.conflict_dialog_radio_keep_both
import com.hkapps.messagepro.R.id.conflict_dialog_radio_merge
import com.hkapps.messagepro.R.id.conflict_dialog_radio_skip
import com.hkapps.messagepro.extensions.beVisibleIf
import com.hkapps.messagepro.extensions.mPref
import com.hkapps.messagepro.extensions.setupDialogStuff
import com.hkapps.messagepro.model.FileDIRModel
import com.hkapps.messagepro.utils.CONFLICT_KEEP_BOTH
import com.hkapps.messagepro.utils.CONFLICT_MERGE
import com.hkapps.messagepro.utils.CONFLICT_OVERWRITE
import com.hkapps.messagepro.utils.CONFLICT_SKIP

class FileConflictDialog(
    val mActivity: Activity, val fileDirItem: FileDIRModel, val showApplyToAllCheckbox: Boolean,
    val callback: (resolution: Int, applyForAll: Boolean) -> Unit
) {
    val view = mActivity.layoutInflater.inflate(R.layout.app_d_file_conflict, null)!!

    init {
        view.apply {
            val stringBase = if (fileDirItem.isDirectory) R.string.folder_already_exists else R.string.file_already_exists
            findViewById<TextView>(R.id.conflict_dialog_title).text = String.format(mActivity.getString(stringBase), fileDirItem.name)
            findViewById<CheckBox>(R.id.conflict_dialog_apply_to_all).isChecked = mActivity.mPref.lastConflictApplyToAll
            findViewById<CheckBox>(R.id.conflict_dialog_apply_to_all).beVisibleIf(showApplyToAllCheckbox)
            findViewById<RadioButton>(R.id.conflict_dialog_radio_merge).beVisibleIf(fileDirItem.isDirectory)

            val resolutionButton = when (mActivity.mPref.lastConflictResolution) {
                CONFLICT_OVERWRITE -> findViewById<RadioButton>(R.id.conflict_dialog_radio_overwrite)
                CONFLICT_MERGE -> findViewById<RadioButton>(R.id.conflict_dialog_radio_merge)
                else -> findViewById<RadioButton>(R.id.conflict_dialog_radio_skip)
            }
            resolutionButton.isChecked = true
        }

        AlertDialog.Builder(mActivity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                mActivity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        val resolution = when (view.findViewById<RadioGroup>(R.id.conflict_dialog_radio_group).checkedRadioButtonId) {
            conflict_dialog_radio_skip -> CONFLICT_SKIP
            conflict_dialog_radio_merge -> CONFLICT_MERGE
            conflict_dialog_radio_keep_both -> CONFLICT_KEEP_BOTH
            else -> CONFLICT_OVERWRITE
        }

        val applyToAll = view.findViewById<CheckBox>(R.id.conflict_dialog_apply_to_all).isChecked
        mActivity.mPref.apply {
            lastConflictApplyToAll = applyToAll
            lastConflictResolution = resolution
        }

        callback(resolution, applyToAll)
    }
}
