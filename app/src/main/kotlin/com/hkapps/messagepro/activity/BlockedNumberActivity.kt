package com.hkapps.messagepro.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.*
import android.widget.PopupWindow
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hkapps.messagepro.R
import com.hkapps.messagepro.adapter.BlockedNumberAdapter
import com.hkapps.messagepro.databinding.ActivityBlockedNumberBinding
import com.hkapps.messagepro.dialogs.AddToBlockDialog
import com.hkapps.messagepro.dialogs.ExportBlockNumberDialog
import com.hkapps.messagepro.dialogs.FilePickerDialog
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.listners.RefreshingRecyclerListner
import com.hkapps.messagepro.model.BlockedNumberModel
import com.hkapps.messagepro.utils.*
import com.hkapps.messagepro.utils.BlockedNumbersExporterUtils.ExportResult
import java.io.FileOutputStream
import java.io.OutputStream

class BlockedNumberActivity : BaseActivity(), RefreshingRecyclerListner {
    private val PICK_IMPORT_SOURCE_INTENT = 11
    private val PICK_EXPORT_FILE_INTENT = 21
    var appTopToolbar: Toolbar? = null
    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList()
    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    private lateinit var binding: ActivityBlockedNumberBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        appTopToolbar = findViewById(R.id.appTopToolbar)
        setSupportActionBar(appTopToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.title = resources.getString(R.string.block_number)
        appTopToolbar?.setNavigationIcon(R.drawable.icon_back)

        updateBlockedNumbers()
        updatePlaceholderTexts()

        binding.tvNoData2.apply {
            setOnClickListener {
                if (isDefaultDialer()) {
                    addOrEditBlockedNumber()
                } else {
                    defaultDialer()
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    protected fun defaultDialer() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
            }
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName).apply {
                try {
                    startActivityForResult(this, REQUEST_CODE_SET_DEFAULT_DIALER)
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_app_found)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.opt_menu_block_no, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuAdd -> addOrEditBlockedNumber()
            R.id.menuMore -> showMoreDialog()

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun refreshItems() {
        updateBlockedNumbers()
    }

    private fun updatePlaceholderTexts() {
        binding.tvNoData1.text = getString(if (isDefaultDialer()) R.string.not_blocking_anyone else R.string.must_make_default_dialer)
        binding.tvNoData2.text = getString(if (isDefaultDialer()) R.string.add_a_blocked_number else R.string.set_as_default)
    }

    private fun updateBlockedNumbers() {
        ensureBackgroundThread {
            val blockedNumbers = getBlockedNumbers()
            runOnUiThread {
                BlockedNumberAdapter(this, blockedNumbers, this, binding.recyclerViewBlockNumber) {
                    addOrEditBlockedNumber(it as BlockedNumberModel)
                }.apply {
                    binding.recyclerViewBlockNumber.adapter = this
                }

                binding.tvNoData1.beVisibleIf(blockedNumbers.isEmpty())
                binding.tvNoData2.beVisibleIf(blockedNumbers.isEmpty())
                binding.ivThumbNodata.beVisibleIf(blockedNumbers.isEmpty())
            }
        }
    }

    private fun addOrEditBlockedNumber(currentNumber: BlockedNumberModel? = null) {
        AddToBlockDialog(this, currentNumber) {
            updateBlockedNumbers()
        }
    }

    private fun tryImportBlockedNumbers() {
        if (isQPlus()) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    pickFileToImportBlockedNumbers()
                }
            }
        }
    }

    private fun tryImportBlockedNumbersFromFile(uri: Uri) {
        when (uri.scheme) {
            "file" -> importBlockedNumbers(uri.path!!)
            "content" -> {
                val tempFile = getTempFile("blocked", "blocked_numbers.txt")
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    importBlockedNumbers(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }

            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun pickFileToImportBlockedNumbers() {
        FilePickerDialog(this) {
            importBlockedNumbers(it)
        }
    }

    private fun importBlockedNumbers(path: String) {
        ensureBackgroundThread {
            val result = BlockedNumbersImporterUtils(this).importBlockedNumbers(path)
            toast(
                when (result) {
                    BlockedNumbersImporterUtils.ImportResult.IMPORT_OK -> R.string.importing_successful
                    BlockedNumbersImporterUtils.ImportResult.IMPORT_FAIL -> R.string.no_items_found
                }
            )
            updateBlockedNumbers()
        }
    }

    private fun tryExportBlockedNumbers() {
        if (isQPlus()) {
            ExportBlockNumberDialog(this, mPref.lastBlockedNumbersExportPath, true) { file ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(this, PICK_EXPORT_FILE_INTENT)
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
                    ExportBlockNumberDialog(this, mPref.lastBlockedNumbersExportPath, false) { file ->
                        getFileOutputStream(file.toFileDirItem(this), true) { out ->
                            exportBlockedNumbersTo(out)
                        }
                    }
                }
            }
        }
    }

    private fun exportBlockedNumbersTo(outputStream: OutputStream?) {
        ensureBackgroundThread {
            val blockedNumbers = getBlockedNumbers()
            if (blockedNumbers.isEmpty()) {
                toast(R.string.no_entries_for_exporting)
            } else {
                BlockedNumbersExporterUtils().exportBlockedNumbers(blockedNumbers, outputStream) {
                    toast(
                        when (it) {
                            ExportResult.EXPORT_OK -> R.string.exporting_successful
                            ExportResult.EXPORT_FAIL -> R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER && isDefaultDialer()) {
            updatePlaceholderTexts()
            updateBlockedNumbers()
        } else if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryImportBlockedNumbersFromFile(resultData.data!!)
        } else if (requestCode == PICK_EXPORT_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportBlockedNumbersTo(outputStream)
        }
    }

    private fun showMoreDialog() {
        val view: View = layoutInflater.inflate(R.layout.dialog_import_export_number, null)
        val popupMore = PopupWindow(
            view, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT, true
        )
        popupMore.animationStyle = android.R.style.Animation_Dialog
        popupMore.showAtLocation(view, Gravity.TOP or Gravity.END, 0, 180)
        dimBackgroundPopWindow(this, popupMore)

        view.findViewById<View>(R.id.llImportNumber).setOnClickListener { v: View? ->
            dismissPopup(popupMore)
            tryImportBlockedNumbers()
        }
        view.findViewById<View>(R.id.llExportNumber).setOnClickListener { v: View? ->
            dismissPopup(popupMore)
            tryExportBlockedNumbers()
        }
    }
}
