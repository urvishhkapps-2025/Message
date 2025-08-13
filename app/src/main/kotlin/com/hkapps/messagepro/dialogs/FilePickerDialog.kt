package com.hkapps.messagepro.dialogs

import android.os.Environment
import android.os.Parcelable
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.adapter.FilePathAdapter
import com.hkapps.messagepro.adapter.FolderPickAdapter
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.model.FileDIRModel
import com.hkapps.messagepro.utils.ensureBackgroundThread
import com.hkapps.messagepro.views.Breadcrumbs
import com.hkapps.messagepro.views.CustomRecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import java.io.File
import java.util.Locale

class FilePickerDialog(
    val contex: BaseActivity,
    var currentFilePath: String = Environment.getExternalStorageDirectory().toString(),
    val isPickFile: Boolean = true,
    var isShowHidden: Boolean = false,
    val showFAB: Boolean = false,
    val canAddShowHiddenButton: Boolean = false,
    val forceShowRoot: Boolean = false,
    val showFavoritesButton: Boolean = false,
    val callback: (pickedPath: String) -> Unit,
) : Breadcrumbs.BreadcrumbsListener {

    private var mFirstUpdate = true
    private var mPrevPath = ""
    private var mScrollStates = HashMap<String, Parcelable>()

    private lateinit var mDialog: AlertDialog
    private var mDialogView = contex.layoutInflater.inflate(R.layout.dialog_folder_pick, null)

    init {
        if (!contex.getDoesFilePathExist(currentFilePath)) {
            currentFilePath = contex.internalStoragePath
        }

        if (!contex.getIsPathDirectory(currentFilePath)) {
            currentFilePath = currentFilePath.getParentPath()
        }

        // do not allow copying files in the recycle bin manually
        if (currentFilePath.startsWith(contex.filesDir.absolutePath)) {
            currentFilePath = contex.internalStoragePath
        }

        mDialogView.findViewById<com.hkapps.messagepro.views.Breadcrumbs>(R.id.fpBreadcrumb).apply {
            listener = this@FilePickerDialog
            updateFontSize(contex.getTextSize())
        }

        tryUpdateItems()
        setupFavorites()

        val builder = AlertDialog.Builder(contex, R.style.ThemeDialogCustom)
            .setNegativeButton(R.string.cancel, null)
            .setOnKeyListener { dialogInterface, i, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_UP && i == KeyEvent.KEYCODE_BACK) {
                    val breadcrumbs = mDialogView.findViewById<com.hkapps.messagepro.views.Breadcrumbs>(R.id.fpBreadcrumb)
                    if (breadcrumbs.itemsCount > 1) {
                        breadcrumbs.removeBreadcrumb()
                        currentFilePath = breadcrumbs.getLastItem().path.trimEnd('/')
                        tryUpdateItems()
                    } else {
                        mDialog.dismiss()
                    }
                }
                true
            }

        if (!isPickFile) {
            builder.setPositiveButton(R.string.ok, null)
        }

        if (showFAB) {
            mDialogView.findViewById<FloatingActionButton>(R.id.fabFilepicker).apply {
                beVisible()
                setOnClickListener { createNewFolder() }
            }
        }

        val secondaryFabBottomMargin = contex.resources.getDimension(if (showFAB) R.dimen.margin_76 else R.dimen.margin_16).toInt()
        mDialogView.findViewById<LinearLayout>(R.id.llFabHolder).apply {
            (layoutParams as CoordinatorLayout.LayoutParams).bottomMargin = secondaryFabBottomMargin
        }

        mDialogView.findViewById<RecyclerViewFastScroller>(R.id.fastscroller).updateColors(contex.resources.getColor(R.color.text_only_blue))

        mDialogView.findViewById<TextView>(R.id.fpFavoritesLabel).text = "${contex.getString(R.string.favorites)}:"
        mDialogView.findViewById<FloatingActionButton>(R.id.fabFavorite).apply {
            beVisibleIf(showFavoritesButton && context.mPref.favorites.isNotEmpty())
            setOnClickListener {
                if (mDialogView.findViewById<RelativeLayout>(R.id.rlFPFavoriteHolder).isVisible()) {
                    hideFavorites()
                } else {
                    showFavorites()
                }
            }
        }

        mDialog = builder.create().apply {
            contex.setupDialogStuff(mDialogView, this, getTitle())
        }

        if (!isPickFile) {
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                verifyPath()
            }
        }
    }

    private fun getTitle() = if (isPickFile) R.string.select_file else R.string.select_folder

    private fun createNewFolder() {
        CreateFolderDialog(contex, currentFilePath) {
            callback(it)
            mDialog.dismiss()
        }
    }

    private fun tryUpdateItems() {
        ensureBackgroundThread {
            getItems(currentFilePath) {
                contex.runOnUiThread {
                    updateItems(it as ArrayList<FileDIRModel>)
                }
            }
        }
    }

    private fun updateItems(items: ArrayList<FileDIRModel>) {
        if (!containsDirectory(items) && !mFirstUpdate && !isPickFile && !showFAB) {
            verifyPath()
            return
        }

        val sortedItems = items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
        val adapter = FolderPickAdapter(contex, sortedItems, mDialogView.findViewById<CustomRecyclerView>(R.id.recyclerViewFilepicker)) {
            if ((it as FileDIRModel).isDirectory) {
                currentFilePath = it.path
                tryUpdateItems()
            } else if (isPickFile) {
                currentFilePath = it.path
                verifyPath()
            }
        }

        val layoutManager = mDialogView.findViewById<CustomRecyclerView>(R.id.recyclerViewFilepicker).layoutManager as LinearLayoutManager
        mScrollStates[mPrevPath.trimEnd('/')] = layoutManager.onSaveInstanceState()!!

        mDialogView.apply {
            findViewById<CustomRecyclerView>(R.id.recyclerViewFilepicker).adapter = adapter
            findViewById<com.hkapps.messagepro.views.Breadcrumbs>(R.id.fpBreadcrumb).setBreadcrumb(currentFilePath)
            layoutManager.onRestoreInstanceState(mScrollStates[currentFilePath.trimEnd('/')])
        }

        mFirstUpdate = false
        mPrevPath = currentFilePath
    }

    private fun verifyPath() {
        if (contex.isRestrictedSAFOnlyRoot(currentFilePath)) {
            val document = contex.getSomeAndroidSAFDocument(currentFilePath) ?: return
            if ((isPickFile && document.isFile) || (!isPickFile && document.isDirectory)) {
                sendSuccess()
            }
        } else if (contex.isPathOnOTG(currentFilePath)) {
            val fileDocument = contex.getSomeDocumentFile(currentFilePath) ?: return
            if ((isPickFile && fileDocument.isFile) || (!isPickFile && fileDocument.isDirectory)) {
                sendSuccess()
            }
        } else {
            val file = File(currentFilePath)
            if ((isPickFile && file.isFile) || (!isPickFile && file.isDirectory)) {
                sendSuccess()
            }
        }
    }

    private fun sendSuccess() {
        currentFilePath = if (currentFilePath.length == 1) {
            currentFilePath
        } else {
            currentFilePath.trimEnd('/')
        }

        callback(currentFilePath)
        mDialog.dismiss()
    }

    private fun getItems(path: String, callback: (List<FileDIRModel>) -> Unit) {
        when {
            contex.isRestrictedSAFOnlyRoot(path) -> {
                contex.handleAndroidSAFDialog(path) {
                    contex.getAndroidSAFFileItems(path, isShowHidden) {
                        callback(it)
                    }
                }
            }

            contex.isPathOnOTG(path) -> contex.getOTGItems(path, isShowHidden, false, callback)
            else -> {
                val lastModifieds = contex.getFolderLastModifieds(path)
                getRegularItems(path, lastModifieds, callback)
            }
        }
    }

    private fun getRegularItems(path: String, lastModifieds: HashMap<String, Long>, callback: (List<FileDIRModel>) -> Unit) {
        val items = ArrayList<FileDIRModel>()
        val files = File(path).listFiles()?.filterNotNull()
        if (files == null) {
            callback(items)
            return
        }

        for (file in files) {
            if (!isShowHidden && file.name.startsWith('.')) {
                continue
            }

            val curPath = file.absolutePath
            val curName = curPath.getFilenameFromPath()
            val size = file.length()
            var lastModified = lastModifieds.remove(curPath)
            val isDirectory = if (lastModified != null) false else file.isDirectory
            if (lastModified == null) {
                lastModified = 0    // we don't actually need the real lastModified that badly, do not check file.lastModified()
            }

            val children = if (isDirectory) file.getDirectChildrenCount(contex, isShowHidden) else 0
            items.add(FileDIRModel(curPath, curName, isDirectory, children, size, lastModified))
        }
        callback(items)
    }

    private fun containsDirectory(items: List<FileDIRModel>) = items.any { it.isDirectory }

    private fun setupFavorites() {
        FilePathAdapter(contex, contex.mPref.favorites.toMutableList(), mDialogView.findViewById<CustomRecyclerView>(R.id.recyclerViewFPFavorites)) {
            currentFilePath = it as String
            verifyPath()
        }.apply {
            mDialogView.findViewById<CustomRecyclerView>(R.id.recyclerViewFPFavorites).adapter = this
        }
    }

    private fun showFavorites() {
        mDialogView.apply {
            findViewById<RelativeLayout>(R.id.rlFPFavoriteHolder).beVisible()
            findViewById<RelativeLayout>(R.id.rlFilepickerHolder).beGone()
            val drawable = contex.resources.getColoredDrawableWithColor(R.drawable.logo_folder, contex.getAdjustedPrimaryColor().getContrastColor())
            findViewById<FloatingActionButton>(R.id.fabFavorite).setImageDrawable(drawable)
        }
    }

    private fun hideFavorites() {
        mDialogView.apply {
            findViewById<RelativeLayout>(R.id.rlFPFavoriteHolder).beGone()
            findViewById<RelativeLayout>(R.id.rlFilepickerHolder).beVisible()
            val drawable = contex.resources.getColoredDrawableWithColor(R.drawable.logo_star, contex.getAdjustedPrimaryColor().getContrastColor())
            findViewById<FloatingActionButton>(R.id.fabFavorite).setImageDrawable(drawable)
        }
    }

    override fun breadcrumbClicked(id: Int) {
        if (id == 0) {
            StoragePickerDialog(contex, currentFilePath, forceShowRoot, true) {
                currentFilePath = it
                tryUpdateItems()
            }
        } else {
            val item = mDialogView.findViewById<com.hkapps.messagepro.views.Breadcrumbs>(R.id.fpBreadcrumb).getItem(id)
            if (currentFilePath != item.path.trimEnd('/')) {
                currentFilePath = item.path
                tryUpdateItems()
            }
        }
    }
}
