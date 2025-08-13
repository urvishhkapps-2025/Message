package com.hkapps.messagepro.dialogs

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import com.hkapps.messagepro.R
import com.hkapps.messagepro.extensions.onGlobalLayout
import com.hkapps.messagepro.extensions.setupDialogStuff
import com.hkapps.messagepro.model.RadioModel

class RadioButtonsDialog(
    val mActivity: Activity, val items: ArrayList<RadioModel>, val checkedItemId: Int = -1, val titleId: Int = 0,
    showOKButton: Boolean = false, val cancelCallback: (() -> Unit)? = null, val callback: (newValue: Any) -> Unit,
) {
    private val dialog: AlertDialog
    private var wasInit = false
    private var selectedItemId = -1

    init {
        val view = mActivity.layoutInflater.inflate(R.layout.dialog_radiogroup, null)
        view.findViewById<RadioGroup>(R.id.radioGroup).apply {
            for (i in 0 until items.size) {
                val radioButton = (mActivity.layoutInflater.inflate(R.layout.dialog_radiobutton, null) as RadioButton).apply {
                    text = items[i].title
                    isChecked = items[i].id == checkedItemId
                    id = i
                    setTextColor(mActivity.resources.getColor(R.color.text_black))
                    setOnClickListener { itemSelected(i) }
                }

                if (items[i].id == checkedItemId) {
                    selectedItemId = i
                }

                addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }

        val builder = AlertDialog.Builder(mActivity)
            .setOnCancelListener { cancelCallback?.invoke() }

        if (selectedItemId != -1 && showOKButton) {
            builder.setPositiveButton(R.string.ok) { dialog, which ->
                itemSelected(selectedItemId)
            }
        }

        dialog = builder.create().apply {
            mActivity.setupDialogStuff(view, this, titleId)
        }

        if (selectedItemId != -1) {
            view.findViewById<ScrollView>(R.id.radioGroupScrollParent).apply {
                onGlobalLayout {
                    scrollY = view.findViewById<RadioGroup>(R.id.radioGroup).findViewById<View>(selectedItemId).bottom - height
                }
            }
        }

        wasInit = true
    }

    private fun itemSelected(checkedId: Int) {
        if (wasInit) {
            callback(items[checkedId].value)
            dialog.dismiss()
        }
    }
}
