package com.hkapps.messagepro.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.ads.AdsHelperClass
import com.hkapps.messagepro.extensions.getTextSize
import com.hkapps.messagepro.extensions.highlightTextPart
import com.hkapps.messagepro.model.SearchModel
import com.hkapps.messagepro.utils.SimpleContactsHelperUtils
import com.hkapps.messagepro.views.CustomRecyclerView

class ChatHistoryBySearchAdapter(
    activity: BaseHomeActivity,
    var mSearchResults: ArrayList<SearchModel>,
    recyclerView: CustomRecyclerView,
    highlightText: String,
    itemClick: (Any) -> Unit,
) : BaseAdapter(activity, recyclerView, null, itemClick) {

    private var fontSize = activity.getTextSize()
    private var textToHighlight = highlightText

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = mSearchResults.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = mSearchResults.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = mSearchResults.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history_by_search, viewGroup, false)
        return ViewHolder(view1)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val holderMessage = holder as ViewHolder
        val searchResult = mSearchResults[position]
        holderMessage.bindView(searchResult, true, false) { itemView, layoutPosition ->
            setupView(itemView, searchResult)
        }

        holder.itemView.tag = holder
    }

    fun updateFontSize() {
        fontSize = mActivity.getTextSize()
        notifyDataSetChanged()
    }


    override fun getItemCount() = mSearchResults.size


    private fun setupView(view: View, searchResult: SearchModel) {
        view.apply {
            findViewById<TextView>(R.id.tvConversationTitle).apply {
                text = searchResult.title?.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            findViewById<TextView>(R.id.tvConversationDesc).apply {
                text = searchResult.snippet?.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            findViewById<TextView>(R.id.tvConversationDate).apply {
                text = searchResult.date
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            }

            SimpleContactsHelperUtils(context).loadContactImage(searchResult.photoUri!!, findViewById<ImageView>(R.id.ivThumb), searchResult.title!!)
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (mSearchResults[position].date!!.isEmpty() && mSearchResults[position].title!!.isEmpty()
            && AdsHelperClass.getIsAdEnable() == 1 && AdsHelperClass.getShowNative() == 1
        ) {
            1
        } else 2
    }


}
