package com.hkapps.messagepro.adapter

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.getAdjustedPrimaryColor
import com.hkapps.messagepro.listners.ActionModeListner
import com.hkapps.messagepro.views.FastScrolling
import com.hkapps.messagepro.R
import com.hkapps.messagepro.ads.AdsHelperClass
import com.hkapps.messagepro.views.CustomRecyclerView
import java.util.*

abstract class BaseAdapter(val mActivity: BaseActivity, val recyclerView: CustomRecyclerView, val fastScroller: FastScrolling? = null,
                           val itemClick: (Any) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    protected val resources = mActivity.resources!!
    protected val layoutInflater = mActivity.layoutInflater
    protected var adjustedPrimaryColor = mActivity.getAdjustedPrimaryColor()
    protected var actModeCallback: ActionModeListner
    protected var selectedKeys = LinkedHashSet<Int>()
    protected var positionOffset = 0
    protected var actMode: ActionMode? = null

    private var actBarTextView: TextView? = null
    private var lastLongPressedItem = -1

    abstract fun getActionMenuId(): Int

    abstract fun prepareActionMode(menu: Menu)

    abstract fun actionItemPressed(id: Int)

    abstract fun getSelectableItemCount(): Int

    abstract fun getIsItemSelectable(position: Int): Boolean

    abstract fun getItemSelectionKey(position: Int): Int?

    abstract fun getItemKeyPosition(key: Int): Int

    abstract fun onActionModeCreated()

    abstract fun onActionModeDestroyed()

    protected fun isOneItemSelected() = selectedKeys.size == 1

    init {
        fastScroller?.resetScrollPositions()

        actModeCallback = object : ActionModeListner() {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                actionItemPressed(item.itemId)
                return true
            }

            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu?): Boolean {
                if (getActionMenuId() == 0) {
                    return true
                }

                isSelectable = true
                actMode = actionMode
                actBarTextView = layoutInflater.inflate(R.layout.app_actionbar_title, null) as TextView
                actBarTextView!!.layoutParams = ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                actMode!!.customView = actBarTextView
                actBarTextView!!.setOnClickListener {
                    if (getSelectableItemCount() == selectedKeys.size) {
                        finishActMode()
                    } else {
                        selectAll()
                    }
                }
                mActivity.menuInflater.inflate(getActionMenuId(), menu)
                onActionModeCreated()
                return true
            }

            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                prepareActionMode(menu)
                return true
            }

            override fun onDestroyActionMode(actionMode: ActionMode) {
                isSelectable = false
                (selectedKeys.clone() as HashSet<Int>).forEach {
                    val position = getItemKeyPosition(it)
                    if (position != -1) {
                        toggleItemSelection(false, position, false)
                    }
                }
                updateTitle()
                selectedKeys.clear()
                actBarTextView?.text = ""
                actMode = null
                lastLongPressedItem = -1
                onActionModeDestroyed()
            }
        }
    }

    protected fun toggleItemSelection(select: Boolean, pos: Int, updateTitle: Boolean = true) {
        if (select && !getIsItemSelectable(pos)) {
            return
        }

        val itemKey = getItemSelectionKey(pos) ?: return
        if ((select && selectedKeys.contains(itemKey)) || (!select && !selectedKeys.contains(itemKey))) {
            return
        }

        if (select) {
            selectedKeys.add(itemKey)
        } else {
            selectedKeys.remove(itemKey)
        }

        notifyItemChanged(pos + positionOffset)

        if (updateTitle) {
            updateTitle()
        }

        if (selectedKeys.isEmpty()) {
            finishActMode()
        }
    }

    private fun updateTitle() {

        var selectableItemCount = 0

        if((AdsHelperClass.getShowNative() == 1)){
            selectableItemCount = getSelectableItemCount()
        }else{
            selectableItemCount = getSelectableItemCount()-1
        }

        val selectedCount = Math.min(selectedKeys.size, selectableItemCount)
        val oldTitle = actBarTextView?.text
        val newTitle = "$selectedCount / $selectableItemCount"
        if (oldTitle != newTitle) {
            actBarTextView?.text = newTitle
            actMode?.invalidate()
        }
    }

    fun itemLongClicked(position: Int) {
        recyclerView.setDragSelectActive(position)
        lastLongPressedItem = if (lastLongPressedItem == -1) {
            position
        } else {
            val min = Math.min(lastLongPressedItem, position)
            val max = Math.max(lastLongPressedItem, position)
            for (i in min..max) {
                toggleItemSelection(true, i, false)
            }
            updateTitle()
            position
        }
    }

    protected fun getSelectedItemPositions(sortDescending: Boolean = true): ArrayList<Int> {
        val positions = ArrayList<Int>()
        val keys = selectedKeys.toList()
        keys.forEach {
            val position = getItemKeyPosition(it)
            if (position != -1) {
                positions.add(position)
            }
        }

        if (sortDescending) {
            positions.sortDescending()
        }
        return positions
    }

    protected fun selectAll() {
        val cnt = itemCount - positionOffset
        for (i in 0 until cnt) {
            toggleItemSelection(true, i, false)
        }
        lastLongPressedItem = -1
        updateTitle()
    }

    protected fun setupDragListener(enable: Boolean) {
        if (enable) {
            recyclerView.setupDragListener(object : CustomRecyclerView.MyDragListener {
                override fun selectItem(position: Int) {
                    toggleItemSelection(true, position, true)
                }

                override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                    selectItemRange(initialSelection, Math.max(0, lastDraggedIndex - positionOffset), Math.max(0, minReached - positionOffset), maxReached - positionOffset)
                    if (minReached != maxReached) {
                        lastLongPressedItem = -1
                    }
                }
            })
        } else {
            recyclerView.setupDragListener(null)
        }
    }

    protected fun selectItemRange(from: Int, to: Int, min: Int, max: Int) {
        if (from == to) {
            (min..max).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            return
        }

        if (to < from) {
            for (i in to..from) {
                toggleItemSelection(true, i, true)
            }

            if (min > -1 && min < to) {
                (min until to).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            }

            if (max > -1) {
                for (i in from + 1..max) {
                    toggleItemSelection(false, i, true)
                }
            }
        } else {
            for (i in from..to) {
                toggleItemSelection(true, i, true)
            }

            if (max > -1 && max > to) {
                (to + 1..max).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            }

            if (min > -1) {
                for (i in min until from) {
                    toggleItemSelection(false, i, true)
                }
            }
        }
    }

    fun finishActMode() {
        actMode?.finish()
    }

    protected fun createViewHolder(layoutType: Int, parent: ViewGroup?): RecyclerView.ViewHolder {
        val view = layoutInflater.inflate(layoutType, parent, false)
        return ViewHolder(view)
    }

    protected fun bindViewHolder(holder: ViewHolder) {
        holder.itemView.tag = holder
    }

    protected fun removeSelectedItems(positions: ArrayList<Int>) {
        positions.forEach {
            notifyItemRemoved(it)
        }
        finishActMode()
        fastScroller?.measureRecyclerView()
    }

    open inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(any: Any, allowSingleClick: Boolean, allowLongClick: Boolean, callback: (itemView: View, layoutPosition: Int) -> Unit): View {
            return itemView.apply {
                callback(this, layoutPosition)

                if (allowSingleClick) {
                    setOnClickListener { viewClicked(any) }
                    setOnLongClickListener {
                        if (allowLongClick)
                            viewLongClicked()
                        else
                            viewClicked(any);
                        true
                    }
                } else {
                    setOnClickListener(null)
                    setOnLongClickListener(null)
                }
            }
        }

        fun viewClicked(any: Any) {
            if (actModeCallback.isSelectable) {
                val currentPosition = layoutPosition - positionOffset
                val isSelected = selectedKeys.contains(getItemSelectionKey(currentPosition))
                toggleItemSelection(!isSelected, currentPosition, true)
            } else {
                itemClick.invoke(any)
            }
            lastLongPressedItem = -1
        }

        fun viewLongClicked() {
            val currentPosition = layoutPosition - positionOffset
            if (!actModeCallback.isSelectable) {
                mActivity.startSupportActionMode(actModeCallback)
            }

            toggleItemSelection(true, currentPosition, true)
            itemLongClicked(currentPosition)
        }
    }
}
