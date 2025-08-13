package com.hkapps.messagepro.activity

import android.app.Dialog
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hkapps.messagepro.MainAppClass.Companion.instance
import com.hkapps.messagepro.R
import com.hkapps.messagepro.adapter.ImportSmsAdapter
import com.hkapps.messagepro.databinding.ActivityBackupRestoreBinding
import com.hkapps.messagepro.dialogs.ExportDialog
import com.hkapps.messagepro.extensions.getFileOutputStream
import com.hkapps.messagepro.extensions.showErrorToast
import com.hkapps.messagepro.extensions.toFileDirItem
import com.hkapps.messagepro.extensions.toast
import com.hkapps.messagepro.utils.*
import java.io.File
import java.io.OutputStream


class BackupRestoreActivity : BaseHomeActivity() {

    var appTopToolbar: Toolbar? = null
    protected val PERMISSIONS_REQUEST_EXPORT = 111
    protected val PERMISSIONS_REQUEST_IMPORT = 222
    private val smsExporter by lazy { MessagesExporterUtils(this) }

    private lateinit var binding: ActivityBackupRestoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appTopToolbar = findViewById(R.id.appTopToolbar)
        setSupportActionBar(appTopToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.title = resources.getString(R.string.backup_and_restore)
        appTopToolbar?.navigationIcon = ContextCompat.getDrawable(this, R.drawable.icon_back)
        instance!!.loadBanner(binding.adViewBanner, this)

        setClicks()
        setLastBackupDate()
    }

    private fun setLastBackupDate() {
        if (TextUtils.isEmpty(config.lastExportDate)) {
            binding.tvBackupDate.text = getString(R.string.no_backup_available)
        } else {
            binding.tvBackupDate.text = config.lastExportDate
        }
    }

    private fun setClicks() {
        binding.llRestore.setOnClickListener {
            if (PermissionUtils.isPermissionGranted_R_W(this)) {
                tryToImportMessages()
            } else {
                PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_IMPORT)
            }
        }
        binding.llBackupNow.setOnClickListener {
            if (PermissionUtils.isPermissionGranted_R_W(this)) {
                tryToExportMessages()
            } else {
                PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0) {
            if (requestCode == PERMISSIONS_REQUEST_EXPORT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (readPermission) {
                        tryToExportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                } else {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (readPermission && writePermission) {
                        tryToExportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                }
            }
            if (requestCode == PERMISSIONS_REQUEST_IMPORT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (readPermission) {
                        tryToImportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                } else {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (readPermission && writePermission) {
                        tryToImportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                }
            }
        }
    }

    //export
    private fun tryToExportMessages() {
        ExportDialog(this) { file ->
            getFileOutputStream(file.toFileDirItem(this), true) { outStream ->
                exportMessagesTo(outStream)
            }
        }
    }

    private fun exportMessagesTo(outputStream: OutputStream?) {
        loadDialog(getString(R.string.backing_up_messages))
        ensureBackgroundThread {
            smsExporter.exportMessages(outputStream) {
                when (it) {
                    MessagesExporterUtils.ExportResult.EXPORT_OK -> {
                        toast(getString(R.string.exporting_successful))
                        setLastBackupDate()
                    }

                    else -> {
//                        toast(getString(R.string.exporting_failed))
                    }
                }
                hideDialog()

            }
        }
    }

    private fun clickEnable(enable: Boolean) {
        binding.llRestore.isEnabled = enable
        binding.llBackupNow.isEnabled = enable
    }


    //import Messages

    private fun tryToImportMessages() {
        val arrayList = getAllSavedFontsPath()

        if (arrayList.size > 0) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.app_layout_import_sms_dialog)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setCancelable(true)
            dialog.window!!
                .setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog

            val recyclerView = dialog.findViewById<View>(R.id.recyclerView) as RecyclerView
            recyclerView.layoutManager = LinearLayoutManager(this)

            val mAdapter = ImportSmsAdapter(this, arrayList, object : ImportSmsAdapter.onContainerClickListner {
                override fun onContainerClick(path: String) {
                    SMS_Dialog_Import(path)
                    dialog.dismiss()
                }
            })
            recyclerView.adapter = mAdapter
            dialog.show()
        } else {
            showErrorToast(getString(R.string.no_backup_available))
        }

    }

    private fun getAllSavedFontsPath(): ArrayList<String> {
        val directory: File = ContextWrapper(this).filesDir

        val arrayList = ArrayList<String>()
        val sb = StringBuilder()
        sb.append(directory.path)
        val file = File(sb.toString())
        if (file.isDirectory) {
            for (fileNext in File(sb.toString()).listFiles()!!) {
                try {
                    if (fileNext.isFile) {
                        val mFile = Uri.fromFile(File(fileNext.absolutePath))
                        val fileExt = MimeTypeMap.getFileExtensionFromUrl(mFile.toString())
                        val filename: String = fileNext.name.substring(fileNext.name.lastIndexOf("/") + 1)

                        if (fileExt.equals("json") && filename.contains("Messages")) {
                            arrayList.add(fileNext.absolutePath)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return arrayList
    }

    fun SMS_Dialog_Import(path: String) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_conformation)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvExportCancel = dialog.findViewById<TextView>(R.id.tvExportCancel)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialog.findViewById<TextView>(R.id.tvDescription)

        tvTitle.text = getString(R.string.import_messages)
        tvDescription.text = getString(R.string.are_you_sure_you_want_to_import_sms_)
        val tvExportOk = dialog.findViewById<TextView>(R.id.tvExportOk)
        tvExportCancel.setOnClickListener {
            dialog.dismiss()
        }

        tvExportOk.setOnClickListener {
            dialog.dismiss()
            config.importSms = true
            loadDialog(getString(R.string.importing_messages))
            ensureBackgroundThread {
                MessagesImporterUtilsUtils(this).importMessages(path) {
                    handleParseResult(it)
                    hideDialog()
                    config.appRunCount = 1
                    refreshMessages()
                }
            }
        }
        if (!dialog.isShowing) dialog.show()
    }

    private fun handleParseResult(result: MessagesImporterUtilsUtils.ImportResult) {
        toast(
            when (result) {
                MessagesImporterUtilsUtils.ImportResult.IMPORT_OK -> R.string.importing_successful
                MessagesImporterUtilsUtils.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_entries_failed
                else -> R.string.no_items_found
            }
        )
    }

    //loading
    fun hideDialog() {
        runOnUiThread {
            clickEnable(true)
            binding.llLoadingView.visibility = View.GONE

//            llLoadingView.animate().translationY(-llLoadingView.height.toFloat()).setInterpolator(AccelerateInterpolator()).setDuration(1000)
//                .setListener(object : Animator.AnimatorListener {
//                    override fun onAnimationStart(animator: Animator) {}
//                    override fun onAnimationEnd(animator: Animator) {
//                        llLoadingView.clearAnimation()
//                        layoutHide()
//                    }
//
//                    override fun onAnimationCancel(animator: Animator) {}
//                    override fun onAnimationRepeat(animator: Animator) {}
//                }).start()

        }

    }

    fun loadDialog(strBackupTitle: String) {
        runOnUiThread {
            clickEnable(false)
            binding.tvBackupTitle.text = strBackupTitle
            binding.llLoadingView.visibility = View.VISIBLE
//            llLoadingView.animate().translationY(0f).setInterpolator(DecelerateInterpolator()).setDuration(1000)
//                .setListener(object : Animator.AnimatorListener {
//                    override fun onAnimationStart(animator: Animator) {
//
//                    }
//
//                    override fun onAnimationEnd(animator: Animator) {}
//                    override fun onAnimationCancel(animator: Animator) {}
//                    override fun onAnimationRepeat(animator: Animator) {}
//                }).start()
        }
    }

}
