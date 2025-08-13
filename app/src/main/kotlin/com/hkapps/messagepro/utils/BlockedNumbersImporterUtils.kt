package com.hkapps.messagepro.utils

import android.telephony.PhoneNumberUtils
import com.hkapps.messagepro.activity.BaseActivity
import com.hkapps.messagepro.extensions.addBlockedNumber
import com.hkapps.messagepro.extensions.showErrorToast
import java.io.File

class BlockedNumbersImporterUtils(
    private val mActivity: BaseActivity,
) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK
    }

    fun importBlockedNumbers(path: String): ImportResult {
        return try {
            val inputStream = File(path).inputStream()
            val numbers = inputStream.bufferedReader().use {
                val content = it.readText().split(BLOCKED_NUMBERS_EXPORT_DELIMITER)
                content.filter { text -> PhoneNumberUtils.isGlobalPhoneNumber(text) }
            }
            if (numbers.isNotEmpty()) {
                numbers.forEach { number ->
                    mActivity.addBlockedNumber(number)
                }
                ImportResult.IMPORT_OK
            } else {
                ImportResult.IMPORT_FAIL
            }

        } catch (e: Exception) {
            mActivity.showErrorToast(e)
            ImportResult.IMPORT_FAIL
        }
    }
}
