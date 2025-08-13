package com.hkapps.messagepro.utils

import com.hkapps.messagepro.model.BlockedNumberModel
import java.io.OutputStream
import java.util.ArrayList

class BlockedNumbersExporterUtils {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK
    }

    fun exportBlockedNumbers(
        blockedNumbers: ArrayList<BlockedNumberModel>,
        outputStream: OutputStream?,
        callback: (result: ExportResult) -> Unit,
    ) {
        if (outputStream == null) {
            callback.invoke(ExportResult.EXPORT_FAIL)
            return
        }

        try {
            outputStream.bufferedWriter().use { out ->
                out.write(blockedNumbers.joinToString(BLOCKED_NUMBERS_EXPORT_DELIMITER) {
                    it.number
                })
            }
            callback.invoke(ExportResult.EXPORT_OK)
        } catch (e: Exception) {
            callback.invoke(ExportResult.EXPORT_FAIL)
        }
    }
}
