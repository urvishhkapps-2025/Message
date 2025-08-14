package com.hkapps.messagepro.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.hkapps.messagepro.extensions.normalizeString
import com.hkapps.messagepro.utils.SimpleContactsHelperUtils
import com.hkapps.messagepro.model.ContactsModel
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity

class AutoCompeletAdapter(val mActivity: BaseHomeActivity, val mContacts: ArrayList<ContactsModel>) : ArrayAdapter<ContactsModel>(mActivity, 0, mContacts) {
    var mResultList = ArrayList<ContactsModel>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contact = mResultList.getOrNull(position)
        var listItem = convertView
        if (listItem == null || listItem.tag != contact?.name?.isNotEmpty()) {
            listItem = LayoutInflater.from(mActivity).inflate(R.layout.list_raw_auto_compelete, parent, false)
        }

        listItem!!.apply {
            tag = contact?.name?.isNotEmpty()
            findViewById<View>(R.id.item_contact_frame).apply {
                isClickable = false
                isFocusable = false
            }

            if (contact != null) {
                findViewById<TextView>(R.id.item_contact_name).text = contact.name
                findViewById<TextView>(R.id.item_contact_number).text = contact.phoneNumbers.first()
                SimpleContactsHelperUtils(context).loadContactImage(contact.photoUri, findViewById(R.id.item_contact_image), contact.name)
            }
        }

        return listItem
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            if (constraint != null) {
                mResultList.clear()
                val searchString = constraint.toString().normalizeString()
                mContacts.forEach {
                    if (it.doesContainPhoneNumber(searchString) || it.name.contains(searchString, true)) {
                        mResultList.add(it)
                    }
                }

                mResultList.sortWith(compareBy { !it.name.startsWith(searchString, true) })

                filterResults.values = mResultList
                filterResults.count = mResultList.size
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if ((results?.count ?: -1) > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        override fun convertResultToString(resultValue: Any?) = (resultValue as? ContactsModel)?.name
    }

    override fun getItem(index: Int) = mResultList[index]

    override fun getCount() = mResultList.size
}
