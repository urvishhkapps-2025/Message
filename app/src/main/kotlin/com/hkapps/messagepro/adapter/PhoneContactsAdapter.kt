package com.hkapps.messagepro.adapter

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hkapps.messagepro.extensions.getTextSize
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.views.FastScrolling
import com.hkapps.messagepro.model.ContactsModel
import com.hkapps.messagepro.utils.SimpleContactsHelperUtils
import java.util.*

class PhoneContactsAdapter(
    mActivity: BaseHomeActivity, var mContacts: ArrayList<ContactsModel>, recyclerView: com.hkapps.messagepro.views.CustomRecyclerView, fastScroller: FastScrolling?,
    itemClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, fastScroller, itemClick) {
    private var fontSize = mActivity.getTextSize()

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = mContacts.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = mContacts.getOrNull(position)?.rawId

    override fun getItemKeyPosition(key: Int) = mContacts.indexOfFirst { it.rawId == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.list_raw_chat_history_by_search, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val contact = mContacts[position]
            holder.bindView(contact, true, false) { itemView, layoutPosition ->
                setupView(itemView, contact)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = mContacts.size

    fun updateContacts(newContacts: ArrayList<ContactsModel>) {
        val oldHashCode = mContacts.hashCode()
        val newHashCode = newContacts.hashCode()
        if (newHashCode != oldHashCode) {
            mContacts = newContacts
            notifyDataSetChanged()
        }
    }

    private fun setupView(view: View, contact: ContactsModel) {
        view.apply {
            findViewById<TextView>(R.id.tvConversationTitle).apply {
                text = contact.name
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            findViewById<TextView>(R.id.tvConversationDate).visibility = View.GONE

            findViewById<TextView>(R.id.tvConversationDesc).apply {
                text = contact.phoneNumbers.first()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            SimpleContactsHelperUtils(context).loadContactImage(contact.photoUri, findViewById(R.id.ivThumb), contact.name)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (!mActivity.isDestroyed && !mActivity.isFinishing) {
            Glide.with(mActivity).clear(holder.itemView.findViewById<ImageView>(R.id.ivThumb))
        }
    }
}
