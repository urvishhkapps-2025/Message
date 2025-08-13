package com.hkapps.messagepro.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.os.AsyncTask
import android.os.Handler
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.util.Pair
import androidx.documentfile.provider.DocumentFile
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.listners.CopyAndMoveListner
import com.hkapps.messagepro.model.FileDIRModel
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*

class CopyMoveTaskMessage(
    val mActivity: BaseActivity, val copyOnly: Boolean, val copyMediaOnly: Boolean, val conflictResolutions: LinkedHashMap<String, Int>,
    listener: CopyAndMoveListner, val copyHidden: Boolean
) : AsyncTask<Pair<ArrayList<FileDIRModel>, String>, Void, Boolean>() {
    private val INITIAL_PROGRESS_DELAY = 3000L
    private val PROGRESS_RECHECK_INTERVAL = 500L

    private var mListener: WeakReference<CopyAndMoveListner>? = null
    private var mTransferredFiles = ArrayList<FileDIRModel>()
    private var mDocuments = LinkedHashMap<String, DocumentFile?>()
    private var mFiles = ArrayList<FileDIRModel>()
    private var mFileCountToCopy = 0
    private var mDestinationPath = ""

    // progress indication
    private var mNotificationBuilder: NotificationCompat.Builder
    private var mCurrFilename = ""
    private var mCurrentProgress = 0L
    private var mMaxSize = 0
    private var mNotifId = 0
    private var mIsTaskOver = false
    private var mProgressHandler = Handler()

    init {
        mListener = WeakReference(listener)
        mNotificationBuilder = NotificationCompat.Builder(mActivity)
    }

    override fun doInBackground(vararg params: Pair<ArrayList<FileDIRModel>, String>): Boolean? {
        if (params.isEmpty()) {
            return false
        }

        val pair = params[0]
        mFiles = pair.first!!
        mDestinationPath = pair.second!!
        mFileCountToCopy = mFiles.size
        mNotifId = (System.currentTimeMillis() / 1000).toInt()
        mMaxSize = 0
        for (file in mFiles) {
            if (file.size == 0L) {
                file.size = file.getProperSize(mActivity, copyHidden)
            }

            val newPath = "$mDestinationPath/${file.name}"
            val fileExists = mActivity.getDoesFilePathExist(newPath)
            if (getConflictResolution(conflictResolutions, newPath) != CONFLICT_SKIP || !fileExists) {
                mMaxSize += (file.size / 1000).toInt()
            }
        }

        mProgressHandler.postDelayed({
            initProgressNotification()
            updateProgress()
        }, INITIAL_PROGRESS_DELAY)

        for (file in mFiles) {
            try {
                val newPath = "$mDestinationPath/${file.name}"
                var newFileDirItem = FileDIRModel(newPath, newPath.getFilenameFromPath(), file.isDirectory)
                if (mActivity.getDoesFilePathExist(newPath)) {
                    val resolution = getConflictResolution(conflictResolutions, newPath)
                    if (resolution == CONFLICT_SKIP) {
                        mFileCountToCopy--
                        continue
                    } else if (resolution == CONFLICT_KEEP_BOTH) {
                        val newFile = fetchAlternativeFile(File(newFileDirItem.path))
                        newFileDirItem = FileDIRModel(newFile.path, newFile.name, newFile.isDirectory)
                    }
                }

                copy(file, newFileDirItem)
            } catch (e: Exception) {
                mActivity.showErrorToast(e)
                return false
            }
        }

        return true
    }

    fun fetchAlternativeFile(file: File): File {
        var fileIndex = 1
        var newFile: File?
        do {
            val newName = String.format("%s(%d).%s", file.nameWithoutExtension, fileIndex, file.extension)
            newFile = File(file.parent, newName)
            fileIndex++
        } while (mActivity.getDoesFilePathExist(newFile!!.absolutePath))
        return newFile
    }

    override fun onPostExecute(success: Boolean) {
        if (mActivity.isFinishing || mActivity.isDestroyed) {
            return
        }

        mProgressHandler.removeCallbacksAndMessages(null)
        mActivity.notificationManager.cancel(mNotifId)
        val listener = mListener?.get() ?: return

        if (success) {
            listener.copySucceeded(copyOnly, mTransferredFiles.size >= mFileCountToCopy, mDestinationPath, mTransferredFiles.size == 1)
        } else {
            listener.copyFailed()
        }
    }

    private fun initProgressNotification() {
        val channelId = "Copy/Move"
        val title = mActivity.getString(if (copyOnly) R.string.copying else R.string.moving)
        if (isOreoPlus()) {
            val importance = NotificationManager.IMPORTANCE_LOW
            NotificationChannel(channelId, title, importance).apply {
                enableLights(false)
                enableVibration(false)
                mActivity.notificationManager.createNotificationChannel(this)
            }
        }

        mNotificationBuilder.setContentTitle(title)
            .setSmallIcon(R.drawable.ic_copy_vector)
            .setChannelId(channelId)
    }

    private fun updateProgress() {
        if (mIsTaskOver) {
            mActivity.notificationManager.cancel(mNotifId)
            cancel(true)
            return
        }

        mNotificationBuilder.apply {
            setContentText(mCurrFilename)
            setProgress(mMaxSize, (mCurrentProgress / 1000).toInt(), false)
            mActivity.notificationManager.notify(mNotifId, build())
        }

        mProgressHandler.removeCallbacksAndMessages(null)
        mProgressHandler.postDelayed({
            updateProgress()

            if (mCurrentProgress / 1000 >= mMaxSize) {
                mIsTaskOver = true
            }
        }, PROGRESS_RECHECK_INTERVAL)
    }

    private fun copy(source: FileDIRModel, destination: FileDIRModel) {
        if (source.isDirectory) {
            copyDirectory(source, destination.path)
        } else {
            copyFile(source, destination)
        }
    }

    private fun copyDirectory(source: FileDIRModel, destinationPath: String) {
        if (!mActivity.createDirectorySync(destinationPath)) {
            val error = String.format(mActivity.getString(R.string.could_not_create_folder), destinationPath)
            mActivity.showErrorToast(error)
            return
        }

        if (mActivity.isPathOnOTG(source.path)) {
            val children = mActivity.getDocumentFile(source.path)?.listFiles() ?: return
            for (child in children) {
                val newPath = "$destinationPath/${child.name}"
                if (File(newPath).exists()) {
                    continue
                }

                val oldPath = "${source.path}/${child.name}"
                val oldFileDirItem = FileDIRModel(oldPath, child.name!!, child.isDirectory, 0, child.length())
                val newFileDirItem = FileDIRModel(newPath, child.name!!, child.isDirectory)
                copy(oldFileDirItem, newFileDirItem)
            }
            mTransferredFiles.add(source)
        } else if (mActivity.isRestrictedSAFOnlyRoot(source.path)) {
            mActivity.getAndroidSAFFileItems(source.path, true) { files ->
                for (child in files) {
                    val newPath = "$destinationPath/${child.name}"
                    if (mActivity.getDoesFilePathExist(newPath)) {
                        continue
                    }

                    val oldPath = "${source.path}/${child.name}"
                    val oldFileDirItem = FileDIRModel(oldPath, child.name, child.isDirectory, 0, child.size)
                    val newFileDirItem = FileDIRModel(newPath, child.name, child.isDirectory)
                    copy(oldFileDirItem, newFileDirItem)
                }
                mTransferredFiles.add(source)
            }
        } else {
            val children = File(source.path).list()
            for (child in children) {
                val newPath = "$destinationPath/$child"
                if (mActivity.getDoesFilePathExist(newPath)) {
                    continue
                }

                val oldFile = File(source.path, child)
                val oldFileDirItem = oldFile.toFileDirItem(mActivity)
                val newFileDirItem = FileDIRModel(newPath, newPath.getFilenameFromPath(), oldFile.isDirectory)
                copy(oldFileDirItem, newFileDirItem)
            }
            mTransferredFiles.add(source)
        }
    }

    private fun copyFile(source: FileDIRModel, destination: FileDIRModel) {
        if (copyMediaOnly && !source.path.isMediaFile()) {
            mCurrentProgress += source.size
            return
        }

        val directory = destination.getParentPath()
        if (!mActivity.createDirectorySync(directory)) {
            val error = String.format(mActivity.getString(R.string.could_not_create_folder), directory)
            mActivity.showErrorToast(error)
            mCurrentProgress += source.size
            return
        }

        mCurrFilename = source.name
        var inputStream: InputStream? = null
        var out: OutputStream? = null
        try {
            if (!mDocuments.containsKey(directory) && mActivity.needsStupidWritePermissions(destination.path)) {
                mDocuments[directory] = mActivity.getDocumentFile(directory)
            }

            out = mActivity.getFileOutputStreamSync(destination.path, source.path.getMimeType(), mDocuments[directory])
            inputStream = mActivity.getFileInputStreamSync(source.path)!!

            var copiedSize = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = inputStream.read(buffer)
            while (bytes >= 0) {
                out!!.write(buffer, 0, bytes)
                copiedSize += bytes
                mCurrentProgress += bytes
                bytes = inputStream.read(buffer)
            }

            out?.flush()

            if (source.size == copiedSize && mActivity.getDoesFilePathExist(destination.path)) {
                mTransferredFiles.add(source)
                if (copyOnly && destination.path.isAudioFast()) {
                    mActivity.rescanPath(destination.path) {
                        if (mActivity.mPref.keepLastModified) {
                            copyOldLastModified(source.path, destination.path)
                            File(destination.path).setLastModified(File(source.path).lastModified())
                        }
                    }
                } else if (mActivity.mPref.keepLastModified) {
                    copyOldLastModified(source.path, destination.path)
                    File(destination.path).setLastModified(File(source.path).lastModified())
                }

                if (!copyOnly) {
                    inputStream.close()
                    out?.close()
                    mActivity.deleteFileBg(source)
                    mActivity.deleteFromMediaStore(source.path)
                }
            }
        } catch (e: Exception) {
            mActivity.showErrorToast(e)
        } finally {
            inputStream?.close()
            out?.close()
        }
    }

    private fun copyOldLastModified(sourcePath: String, destinationPath: String) {
        val projection = arrayOf(
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        var selectionArgs = arrayOf(sourcePath)
        val cursor = mActivity.applicationContext.contentResolver.query(uri, projection, selection, selectionArgs, null)

        cursor?.use {
            if (cursor.moveToFirst()) {
                val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
                val dateModified = cursor.getIntValue(MediaStore.Images.Media.DATE_MODIFIED)

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
                    put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
                }

                selectionArgs = arrayOf(destinationPath)
                mActivity.applicationContext.contentResolver.update(uri, values, selection, selectionArgs)
            }
        }
    }
}
