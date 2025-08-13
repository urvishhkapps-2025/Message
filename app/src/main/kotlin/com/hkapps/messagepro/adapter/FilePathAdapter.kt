package com.hkapps.messagepro.adapter

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.getTextSize

class FilePathAdapter(
    activity: BaseActivity, val mPaths: List<String>, recyclerView: com.hkapps.messagepro.views.CustomRecyclerView,
    itemClick: (Any) -> Unit
) : BaseAdapter(activity, recyclerView, null, itemClick) {

    private var fontSize = 0f

    init {
        fontSize = activity.getTextSize()
    }

    override fun getActionMenuId() = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.list_raw_file_path, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val path = mPaths[position]
        if (holder is ViewHolder) {
            holder.bindView(path, true, false) { itemView, layoutPosition ->
                setupView(itemView, path)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = mPaths.size

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = mPaths.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemKeyPosition(key: Int) = mPaths.indexOfFirst { it.hashCode() == key }

    override fun getItemSelectionKey(position: Int) = mPaths[position].hashCode()

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    private fun setupView(view: View, path: String) {
        view.apply {
            findViewById<TextView>(R.id.filepicker_favorite_label).text = path
            findViewById<TextView>(R.id.filepicker_favorite_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
        }
    }
}
