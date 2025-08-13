package com.hkapps.messagepro.adapter

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.model.FileDIRModel
import com.hkapps.messagepro.utils.getFilePlaceholderDrawables
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import java.util.Locale

class FolderPickAdapter(
    mActivity: BaseActivity, val mFileDirItems: List<FileDIRModel>, recyclerView: com.hkapps.messagepro.views.CustomRecyclerView,
    itemClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, null, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    private lateinit var fileDrawable: Drawable
    private lateinit var folderDrawable: Drawable
    private var fileDrawables = HashMap<String, Drawable>()
    private val hasOTGConnected = mActivity.hasOTGConnected()
    private var fontSize = 0f
    private val cornerRadius = resources.getDimension(R.dimen.margin_4).toInt()
    private val dateFormat = mActivity.mPref.dateFormat
    private val timeFormat = mActivity.getTimeFormat()

    init {
        initDrawables()
        fontSize = mActivity.getTextSize()
    }

    override fun getActionMenuId() = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.list_raw_folder_pick, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val fileDirItem = mFileDirItems[position]
        if (holder is ViewHolder) {
            holder.bindView(fileDirItem, true, false) { itemView, layoutPosition ->
                setupView(itemView, fileDirItem)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = mFileDirItems.size

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = mFileDirItems.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemKeyPosition(key: Int) = mFileDirItems.indexOfFirst { it.path.hashCode() == key }

    override fun getItemSelectionKey(position: Int) = mFileDirItems[position].path.hashCode()

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (!mActivity.isDestroyed && !mActivity.isFinishing) {
            Glide.with(mActivity).clear(holder.itemView.findViewById<ImageView>(R.id.ivThumb)!!)
        }
    }

    private fun setupView(view: View, fileDirItem: FileDIRModel) {
        view.apply {
            findViewById<TextView>(R.id.tvFolderTitle).text = fileDirItem.name
            findViewById<TextView>(R.id.tvFolderTitle).setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            findViewById<TextView>(R.id.tvFolderSize).setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            if (fileDirItem.isDirectory) {
                findViewById<ImageView>(R.id.ivThumb).setImageDrawable(folderDrawable)
                findViewById<TextView>(R.id.tvFolderSize).text = getChildrenCnt(fileDirItem)
            } else {
                findViewById<TextView>(R.id.tvFolderSize).text = fileDirItem.size.formatSize()
                val path = fileDirItem.path
                val placeholder = fileDrawables.getOrElse(fileDirItem.name.substringAfterLast(".").lowercase(Locale.getDefault()), { fileDrawable })
                val options = RequestOptions()
                    .signature(fileDirItem.getKey())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .error(placeholder)

                var itemToLoad = if (fileDirItem.name.endsWith(".apk", true)) {
                    val packageInfo = context.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
                    if (packageInfo != null) {
                        val appInfo = packageInfo.applicationInfo
                        appInfo!!.sourceDir = path
                        appInfo.publicSourceDir = path
                        appInfo.loadIcon(context.packageManager)
                    } else {
                        path
                    }
                } else {
                    path
                }

                if (!mActivity.isDestroyed && !mActivity.isFinishing) {
                    if (mActivity.isRestrictedSAFOnlyRoot(path)) {
                        itemToLoad = mActivity.getAndroidSAFUri(path)
                    } else if (hasOTGConnected && itemToLoad is String && mActivity.isPathOnOTG(itemToLoad)) {
                        itemToLoad = itemToLoad.getOTGPublicPath(mActivity)
                    }

                    if (itemToLoad.toString().isGif()) {
                        Glide.with(mActivity).asBitmap().load(itemToLoad).apply(options).into(findViewById<ImageView>(R.id.ivThumb))
                    } else {
                        Glide.with(mActivity)
                            .load(itemToLoad)
                            .transition(withCrossFade())
                            .apply(options)
                            .transform(CenterCrop(), RoundedCorners(cornerRadius))
                            .into(findViewById<ImageView>(R.id.ivThumb))
                    }
                }
            }
        }
    }

    private fun getChildrenCnt(item: FileDIRModel): String {
        val children = item.children
        return mActivity.resources.getQuantityString(R.plurals.items, children, children)
    }

    private fun initDrawables() {
        folderDrawable = resources.getDrawable(R.drawable.logo_folder)
        fileDrawable = resources.getDrawable(R.drawable.icon_f_null)
        fileDrawables = getFilePlaceholderDrawables(mActivity)
    }

    override fun onChange(position: Int) = mFileDirItems.getOrNull(position)?.getBubbleText(mActivity, dateFormat, timeFormat) ?: ""
}
