package com.hkapps.messagepro.adapter

import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.ads.native_ads_event.EventConstants
import com.hkapps.messagepro.ads.native_ads_event.EventListner
import com.hkapps.messagepro.ads.native_ads_event.NotifierFactoryApp
import com.hkapps.messagepro.extensions.getTextSize
import com.hkapps.messagepro.extensions.highlightTextPart
import com.hkapps.messagepro.model.SearchModel
import com.hkapps.messagepro.utils.SimpleContactsHelperUtils

class SearchAdapter(
    activity: BaseHomeActivity,
    var mSearchResults: ArrayList<SearchModel>,
    recyclerView: com.hkapps.messagepro.views.CustomRecyclerView,
    highlightText: String,
    itemClick: (Any) -> Unit
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

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_search_result, parent)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_native_ads, viewGroup, false)
            AdHolderGoogle(view)
        } else {
            val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history_by_search, viewGroup, false)
            ViewHolder(view1)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is AdHolderGoogle) {
            val adHolderGoogle = holder
            adHolderGoogle.registerAdsListener()
            MainAppClass.instance!!.loadNativeAd(adHolderGoogle.fl_adplaceholder, mActivity, 0)
        } else if (holder is ViewHolder) {
            val searchResult = mSearchResults[position]
            holder.bindView(searchResult, true, false) { itemView, layoutPosition ->
                setupView(itemView, searchResult)
            }

        }
        holder.itemView.tag = holder
    }

    fun updateFontSize() {
        fontSize = mActivity.getTextSize()
        notifyDataSetChanged()
    }


    override fun getItemCount() = mSearchResults.size

    fun updateItems(newItems: ArrayList<SearchModel>, highlightText: String = "") {
        if (newItems.hashCode() != mSearchResults.hashCode()) {
            mSearchResults = newItems.clone() as ArrayList<SearchModel>
            textToHighlight = highlightText
            notifyDataSetChanged()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    private fun setupView(view: View, searchResult: SearchModel) {
        view.apply {
            findViewById<TextView>(R.id.tvConversationTitle).apply {
                text = searchResult.title!!.highlightTextPart(textToHighlight, adjustedPrimaryColor)
//                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            findViewById<TextView>(R.id.tvConversationDesc).apply {
                text = searchResult.snippet!!.highlightTextPart(textToHighlight, adjustedPrimaryColor)
//                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            findViewById<TextView>(R.id.tvConversationDate).apply {
                text = searchResult.date
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
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

    override fun getItemViewType(position: Int): Int {
        return if (mSearchResults[position].date!!.isEmpty() && mSearchResults[position].title!!.isEmpty()) {
            1
        } else 2
    }

    open inner class AdHolderGoogle(view: View) : RecyclerView.ViewHolder(view),
        EventListner {
        var fl_adplaceholder: FrameLayout = view.findViewById(R.id.fl_adplaceholder)
        fun registerAdsListener() {
            val notifier = NotifierFactoryApp.instance?.getNotifier(NotifierFactoryApp.EVENT_NOTIFIER_AD_STATUS)
            notifier?.registerListener(this, 1000)
        }

        override fun eventNotify(eventType: Int, eventObject: Any?): Int {
            var eventState = EventConstants.EVENT_IGNORED
            if (eventType == EventConstants.EVENT_AD_LOADED_NATIVE) {
                eventState = EventConstants.EVENT_PROCESSED
                mActivity.runOnUiThread {
                    Handler(Looper.myLooper()!!).postDelayed(
                        { MainAppClass.instance!!.loadNativeAd(fl_adplaceholder, mActivity, 0) },
                        500
                    )
                }
            }
            return eventState
        }

    }

}
