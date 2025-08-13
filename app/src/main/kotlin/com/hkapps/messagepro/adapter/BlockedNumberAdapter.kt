package com.hkapps.messagepro.adapter

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.copyToClipboard
import com.hkapps.messagepro.extensions.deleteBlockedNumber
import com.hkapps.messagepro.listners.RefreshingRecyclerListner
import com.hkapps.messagepro.model.BlockedNumberModel
import com.hkapps.messagepro.utils.SimpleContactsHelperUtils

class BlockedNumberAdapter(
    activity: BaseActivity, var blockedNumbers: ArrayList<BlockedNumberModel>, val mListener: RefreshingRecyclerListner?,
    recyclerView: com.hkapps.messagepro.views.CustomRecyclerView, itemClick: (Any) -> Unit,
) : BaseAdapter(activity, recyclerView, null, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.app_menu_cab_blocked_numbers

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.menu_menu_cab_copy_number).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.menu_menu_cab_copy_number -> copyNumberToClipboard()
            R.id.menu_cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = blockedNumbers.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = blockedNumbers.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = blockedNumbers.indexOfFirst { it.id.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.layout_block_no, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val blockedNumber = blockedNumbers[position]
        if (holder is ViewHolder) {
            holder.bindView(blockedNumber, true, true) { itemView, layoutPosition ->
                setupView(itemView, blockedNumber)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = blockedNumbers.size

    private fun getSelectedItems() = blockedNumbers.filter { selectedKeys.contains(it.id.toInt()) } as ArrayList<BlockedNumberModel>

    private fun setupView(view: View, blockedNumber: BlockedNumberModel) {
        view.apply {
//            manage_blocked_number_holder?.isSelected = selectedKeys.contains(blockedNumber.id.toInt())

            if (selectedKeys.contains(blockedNumber.id.toInt())) {
                findViewById<RelativeLayout>(R.id.manage_blocked_number_holder).setBackgroundColor(mActivity.resources.getColor(R.color.activated_item_foreground))
            } else {
                findViewById<RelativeLayout>(R.id.manage_blocked_number_holder).setBackgroundColor(mActivity.resources.getColor(R.color.transparent))
            }

            findViewById<TextView>(R.id.manage_blocked_number_title).apply {
                text = blockedNumber.number
            }
            SimpleContactsHelperUtils(context).loadContactImage(blockedNumber.number, findViewById<ImageView>(R.id.ivThumb), blockedNumber.number)
        }
    }

    private fun copyNumberToClipboard() {
        val selectedNumber = getSelectedItems().firstOrNull() ?: return
        mActivity.copyToClipboard(selectedNumber.number)
        finishActMode()
    }

    private fun deleteSelection() {
        val deleteBlockedNumbers = ArrayList<BlockedNumberModel>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            deleteBlockedNumbers.add(it)
            mActivity.deleteBlockedNumber(it.number)
        }

        blockedNumbers.removeAll(deleteBlockedNumbers)
        removeSelectedItems(positions)
        if (blockedNumbers.isEmpty()) {
            mListener?.refreshItems()
        }
    }
}
