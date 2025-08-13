package com.hkapps.messagepro.adapter

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.extensions.highlightTextPart
import com.hkapps.messagepro.model.SearchModel
import com.hkapps.messagepro.utils.SimpleContactsHelperUtils

class SuggestionAdapter(
    mActivity: BaseHomeActivity,
    var searchResults: ArrayList<SearchModel>,
    recyclerView: com.hkapps.messagepro.views.CustomRecyclerView,
    itemClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, null, itemClick) {

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = searchResults.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = searchResults.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = searchResults.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.list_raw_suggestions, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val searchResult = searchResults[position]
        if (holder is ViewHolder) {
            holder.bindView(searchResult, true, false) { itemView, layoutPosition ->
                setupView(itemView, searchResult)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = searchResults.size

    private fun setupView(view: View, searchResult: SearchModel) {
        view.apply {

            val paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics).toInt()
            findViewById<LinearLayout>(R.id.llMain).setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            findViewById<TextView>(R.id.tvTitle).apply {
                text = searchResult.title!!.highlightTextPart("", adjustedPrimaryColor)
            }

            SimpleContactsHelperUtils(context).loadContactImage(searchResult.photoUri!!, findViewById<ImageView>(R.id.ivThumb), searchResult.title!!)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (!mActivity.isDestroyed && !mActivity.isFinishing && holder.itemView.findViewById<ImageView>(R.id.ivThumb) != null) {
            Glide.with(mActivity).clear(holder.itemView.findViewById<ImageView>(R.id.ivThumb))
        }
    }
}
