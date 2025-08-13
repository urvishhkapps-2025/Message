package com.hkapps.messagepro.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hkapps.messagepro.R


class ImportSmsAdapter(
    val context: Context,
    val arrayList: ArrayList<String>?,
    val containerClickListner: onContainerClickListner?
) :
    RecyclerView.Adapter<ImportSmsAdapter.AdapterVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_raw_import_sms, parent, false)
        return AdapterVH(view)
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val path = arrayList!![position]
        val filename: String = path.substring(path.lastIndexOf("/") + 1)
        holder.tvFileName.text = filename
        if (arrayList.size - 1 == position) {
            holder.mView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return arrayList!!.size
    }

    inner class AdapterVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mView: View = itemView.findViewById(R.id.mView)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)

        init {

            itemView.setOnClickListener {
                containerClickListner?.onContainerClick(arrayList!![layoutPosition])
            }
        }
    }

    interface onContainerClickListner {
        fun onContainerClick(path: String)
    }

}
