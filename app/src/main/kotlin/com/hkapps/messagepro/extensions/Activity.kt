package com.hkapps.messagepro.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.text.Html
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.dialogs.AlertSideLoadDialog
import com.hkapps.messagepro.dialogs.WritePermissionDialog
import com.hkapps.messagepro.model.FileDIRModel
import com.hkapps.messagepro.utils.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

fun AppCompatActivity.updateActionBarTitle(text: String, color: Int = mPref.primaryColor) {
    supportActionBar?.title = Html.fromHtml("<font color='${color.getContrastColor().toHex()}'>$text</font>")
}

fun Activity.appLaunched(appId: String) {
    mPref.internalStoragePath = getInternalStoragePath()
    updateSDCardPath()
    mPref.appId = appId
    mPref.appRunCount++
}


fun BaseActivity.isShowingSAFDialog(path: String): Boolean {
    return if ((!isRPlus() && isPathOnSD(path) && !isSDCardSetAsDefaultStorage() && (mPref.sdTreeUri.isEmpty() || !hasProperStoredTreeUri(false)))) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                WritePermissionDialog(this, false) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra("android.content.extra.SHOW_ADVANCED", true)
                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_SD)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_SD)
                            checkedDocumentPath = path
                        } catch (e: Exception) {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun BaseActivity.isShowingAndroidSAFDialog(path: String): Boolean {
    return if (isRestrictedSAFOnlyRoot(path) && (getAndroidTreeUri(path).isEmpty() || !hasProperStoredAndroidTreeUri(path))) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                com.hkapps.messagepro.dialogs.AlertDialogCustom(this, resources.getString(R.string.confirm_storage_access_android_text)) { ->
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra("android.content.extra.SHOW_ADVANCED", true)
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, createAndroidDataOrObbUri(path))
                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB)
                            checkedDocumentPath = path
                        } catch (e: Exception) {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun BaseActivity.isShowingOTGDialog(path: String): Boolean {
    return if (!isRPlus() && isPathOnOTG(path) && (mPref.OTGTreeUri.isEmpty() || !hasProperStoredTreeUri(true))) {
        showOTGPermissionDialog(path)
        true
    } else {
        false
    }
}

fun BaseActivity.showOTGPermissionDialog(path: String) {
    runOnUiThread {
        if (!isDestroyed && !isFinishing) {
            WritePermissionDialog(this, true) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    try {
                        startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                        checkedDocumentPath = path
                        return@apply
                    } catch (e: Exception) {
                        type = "*/*"
                    }

                    try {
                        startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                        checkedDocumentPath = path
                    } catch (e: Exception) {
                        toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }
}

fun Activity.launchViewIntent(url: String) {
    ensureBackgroundThread {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            launchActivityIntent(this)
        }
    }
}

fun Activity.redirectToRateUs() {
    try {
        launchViewIntent("market://details?id=${packageName.removeSuffix(".debug")}")
    } catch (ignored: ActivityNotFoundException) {
        launchViewIntent(getStoreUrl())
    }
}

fun Activity.shareTextIntent(text: String) {
    ensureBackgroundThread {
        Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)

            try {
                startActivity(Intent.createChooser(this, getString(R.string.share_via)))
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    showErrorToast(e)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

fun BaseActivity.deleteFileBg(fileDirItem: FileDIRModel, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    val path = fileDirItem.path
    if (isRestrictedSAFOnlyRoot(path)) {
        deleteAndroidSAFDirectory(path, allowDeleteFolder, callback)
    } else {
        val file = File(path)
        if (file.absolutePath.startsWith(internalStoragePath) && !file.canWrite()) {
            callback?.invoke(false)
            return
        }

        var fileDeleted = !isPathOnOTG(path) && ((!file.exists() && file.length() == 0L) || file.delete())
        if (fileDeleted) {
            deleteFromMediaStore(path) { needsRescan ->
                if (needsRescan) {
                    rescanAndDeletePath(path) {
                        runOnUiThread {
                            callback?.invoke(true)
                        }
                    }
                } else {
                    runOnUiThread {
                        callback?.invoke(true)
                    }
                }
            }
        } else {
            if (getIsPathDirectory(file.absolutePath) && allowDeleteFolder) {
                fileDeleted = deleteRecursively(file)
            }

            if (!fileDeleted) {
                if (needsStupidWritePermissions(path)) {
                    handleSAFDialog(path) {
                        if (it) {
                            trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
                        }
                    }
                } else if (isRPlus()) {
                    val fileUris = getFileUrisFromFileDirItems(arrayListOf(fileDirItem))
                    deleteSDK30Uris(fileUris) { success ->
                        runOnUiThread {
                            callback?.invoke(success)
                        }
                    }
                }
            }
        }
    }
}

private fun deleteRecursively(file: File): Boolean {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return file.delete()
        for (child in files) {
            deleteRecursively(child)
        }
    }

    return file.delete()
}

fun Activity.scanPathRecursively(path: String, callback: (() -> Unit)? = null) {
    applicationContext.scanPathRecursively(path, callback)
}


fun Activity.scanPathsRecursively(paths: List<String>, callback: (() -> Unit)? = null) {
    applicationContext.scanPathsRecursively(paths, callback)
}

fun Activity.rescanPath(path: String, callback: (() -> Unit)? = null) {
    applicationContext.rescanPath(path, callback)
}

fun Activity.rescanPaths(paths: List<String>, callback: (() -> Unit)? = null) {
    applicationContext.rescanPaths(paths, callback)
}

fun BaseActivity.renameFile(oldPath: String, newPath: String, callback: ((success: Boolean) -> Unit)? = null) {
    if (isRestrictedSAFOnlyRoot(oldPath)) {
        handleAndroidSAFDialog(oldPath) {
            if (!it) {
                runOnUiThread {
                    callback?.invoke(false)
                }
                return@handleAndroidSAFDialog
            }

            try {
                val success = renameAndroidSAFDocument(oldPath, newPath)
                runOnUiThread {
                    callback?.invoke(success)
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false)
                }
            }
        }
    } else if (needsStupidWritePermissions(newPath)) {
        handleSAFDialog(newPath) {
            if (!it) {
                return@handleSAFDialog
            }

            val document = getSomeDocumentFile(oldPath)
            if (document == null || (File(oldPath).isDirectory != document.isDirectory)) {
                runOnUiThread {
                    callback?.invoke(false)
                }
                return@handleSAFDialog
            }

            try {
                ensureBackgroundThread {
                    try {
                        DocumentsContract.renameDocument(applicationContext.contentResolver, document.uri, newPath.getFilenameFromPath())
                    } catch (ignored: FileNotFoundException) {
                        // FileNotFoundException is thrown in some weird cases, but renaming works just fine
                    } catch (e: Exception) {
                        showErrorToast(e)
                        callback?.invoke(false)
                        return@ensureBackgroundThread
                    }

                    updateInMediaStore(oldPath, newPath)
                    rescanPaths(arrayListOf(oldPath, newPath)) {
                        if (!mPref.keepLastModified) {
                            updateLastModified(newPath, System.currentTimeMillis())
                        }
                        deleteFromMediaStore(oldPath)
                        runOnUiThread {
                            callback?.invoke(true)
                        }
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false)
                }
            }
        }
    } else {
        val oldFile = File(oldPath)
        val newFile = File(newPath)
        val tempFile = createTempFile(oldFile) ?: return
        val oldToTempSucceeds = oldFile.renameTo(tempFile)
        val tempToNewSucceeds = tempFile.renameTo(newFile)
        if (oldToTempSucceeds && tempToNewSucceeds) {
            if (newFile.isDirectory) {
                updateInMediaStore(oldPath, newPath)
                rescanPath(newPath) {
                    runOnUiThread {
                        callback?.invoke(true)
                    }
                    scanPathRecursively(newPath)
                }
            } else {
                if (!mPref.keepLastModified) {
                    newFile.setLastModified(System.currentTimeMillis())
                }
                updateInMediaStore(oldPath, newPath)
                scanPathsRecursively(arrayListOf(newPath)) {
                    runOnUiThread {
                        callback?.invoke(true)
                    }
                }
            }
        } else {
            tempFile.delete()
            runOnUiThread {
                callback?.invoke(false)
            }
        }
    }
}

fun Activity.createTempFile(file: File): File? {
    return if (file.isDirectory) {
        createTempDir("temp", "${System.currentTimeMillis()}", file.parentFile)
    } else {
        if (isRPlus()) {
            try {
                kotlin.io.path.createTempFile(file.parentFile.toPath(), "temp", "${System.currentTimeMillis()}").toFile()
            } catch (e: Exception) {
                showErrorToast(e)
                null
            }
        } else {
            createTempFile("temp", "${System.currentTimeMillis()}", file.parentFile)
        }
    }
}

fun Activity.hideKeyboard() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}

fun Activity.showKeyboard(et: EditText) {
    et.requestFocus()
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun BaseActivity.getFileOutputStream(fileDirItem: FileDIRModel, allowCreatingNewFile: Boolean = false, callback: (outputStream: OutputStream?) -> Unit) {
    if (needsStupidWritePermissions(fileDirItem.path)) {
        handleSAFDialog(fileDirItem.path) {
            if (!it) {
                return@handleSAFDialog
            }

            var document = getDocumentFile(fileDirItem.path)
            if (document == null && allowCreatingNewFile) {
                document = getDocumentFile(fileDirItem.getParentPath())
            }

            if (document == null) {
                showFileCreateError(fileDirItem.path)
                callback(null)
                return@handleSAFDialog
            }

            if (!getDoesFilePathExist(fileDirItem.path)) {
                document = document.createFile("", fileDirItem.name) ?: getDocumentFile(fileDirItem.path)
            }

            if (document?.exists() == true) {
                try {
                    callback(applicationContext.contentResolver.openOutputStream(document.uri))
                } catch (e: FileNotFoundException) {
                    showErrorToast(e)
                    callback(null)
                }
            } else {
                showFileCreateError(fileDirItem.path)
                callback(null)
            }
        }
    } else {
        val file = File(fileDirItem.path)
        if (file.parentFile?.exists() == false) {
            file.parentFile.mkdirs()
        }

        try {
            callback(FileOutputStream(file))
        } catch (e: Exception) {
            callback(null)
        }
    }
}

fun BaseActivity.showFileCreateError(path: String) {
    val error = String.format(getString(R.string.could_not_create_file), path)
    mPref.sdTreeUri = ""
    showErrorToast(error)
}

fun BaseActivity.getFileOutputStreamSync(path: String, mimeType: String, parentDocumentFile: DocumentFile? = null): OutputStream? {
    val targetFile = File(path)

    return when {
        isRestrictedSAFOnlyRoot(path) -> {
            val uri = getAndroidSAFUri(path)
            if (!getDoesFilePathExist(path)) {
                createAndroidSAFFile(path)
            }
            applicationContext.contentResolver.openOutputStream(uri)
        }

        needsStupidWritePermissions(path) -> {
            var documentFile = parentDocumentFile
            if (documentFile == null) {
                if (getDoesFilePathExist(targetFile.parentFile.absolutePath)) {
                    documentFile = getDocumentFile(targetFile.parent)
                } else {
                    documentFile = getDocumentFile(targetFile.parentFile.parent)
                    documentFile = documentFile!!.createDirectory(targetFile.parentFile.name) ?: getDocumentFile(targetFile.parentFile.absolutePath)
                }
            }

            if (documentFile == null) {
                val casualOutputStream = createCasualFileOutputStream(this, targetFile)
                return if (casualOutputStream == null) {
                    showFileCreateError(targetFile.parent)
                    null
                } else {
                    casualOutputStream
                }
            }

            try {
                val newDocument = documentFile.createFile(mimeType, path.getFilenameFromPath()) ?: getDocumentFile(path)
                applicationContext.contentResolver.openOutputStream(newDocument!!.uri)
            } catch (e: Exception) {
                showErrorToast(e)
                null
            }
        }

        else -> return createCasualFileOutputStream(this, targetFile)
    }
}

private fun createCasualFileOutputStream(activity: BaseActivity, targetFile: File): OutputStream? {
    if (targetFile.parentFile?.exists() == false) {
        targetFile.parentFile?.mkdirs()
    }

    return try {
        FileOutputStream(targetFile)
    } catch (e: Exception) {
        activity.showErrorToast(e)
        null
    }
}


fun BaseActivity.createDirectorySync(directory: String): Boolean {
    if (getDoesFilePathExist(directory)) {
        return true
    }

    if (needsStupidWritePermissions(directory)) {
        val documentFile = getDocumentFile(directory.getParentPath()) ?: return false
        val newDir = documentFile.createDirectory(directory.getFilenameFromPath()) ?: getDocumentFile(directory)
        return newDir != null
    }

    if (isRestrictedSAFOnlyRoot(directory)) {
        return createAndroidSAFDirectory(directory)
    }

    return File(directory).mkdirs()
}


fun Activity.setupDialogStuff(
    view: View,
    dialog: AlertDialog,
    titleId: Int = 0,
    titleText: String = "",
    cancelOnTouchOutside: Boolean = true,
    callback: (() -> Unit)? = null
) {
    if (isDestroyed || isFinishing) {
        return
    }

    val adjustedPrimaryColor = getAdjustedPrimaryColor()

    var title: TextView? = null
    if (titleId != 0 || titleText.isNotEmpty()) {
        title = layoutInflater.inflate(R.layout.app_d_title, null) as TextView
        title.findViewById<TextView>(R.id.dialog_title_textview).apply {
            if (titleText.isNotEmpty()) {
                text = titleText
            } else {
                setText(titleId)
            }
//            setTextColor(mPref.textColor)
        }
    }

    // if we use the same primary and background color, use the text color for dialog confirmation buttons
    val dialogButtonColor = if (adjustedPrimaryColor == mPref.backgroundColor) {
        mPref.textColor
    } else {
        adjustedPrimaryColor
    }

    dialog.apply {
        setView(view)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCustomTitle(title)
        setCanceledOnTouchOutside(cancelOnTouchOutside)
        show()
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(dialogButtonColor)
        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(dialogButtonColor)
        getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(dialogButtonColor)

        val bgDrawable = resources.getColoredDrawableWithColor(R.drawable.apk_background_dialog, mPref.backgroundColor)
        window?.setBackgroundDrawable(bgDrawable)
    }
    callback?.invoke()
}

fun AppCompatActivity.checkAppSideloading(): Boolean {
    val isSideloaded = when (mPref.appSideloadingStatus) {
        SIDELOADING_TRUE -> true
        SIDELOADING_FALSE -> false
        else -> isAppSideloaded()
    }

    mPref.appSideloadingStatus = if (isSideloaded) SIDELOADING_TRUE else SIDELOADING_FALSE
    if (isSideloaded) {
        showSideloadingDialog()
    }

    return isSideloaded
}

fun AppCompatActivity.isAppSideloaded(): Boolean {
    return try {
        getDrawable(R.mipmap.ic_launcher)
        false
    } catch (e: Exception) {
        true
    }
}

fun AppCompatActivity.showSideloadingDialog() {
    AlertSideLoadDialog(this) {
        finish()
    }
}

fun BaseActivity.getTempFile(folderName: String, fileName: String): File? {
    val folder = File(cacheDir, folderName)
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, fileName)
}
