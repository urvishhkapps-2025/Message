package com.hkapps.messagepro.listners

interface CopyAndMoveListner {
    fun copySucceeded(copyOnly: Boolean, copiedAll: Boolean, destinationPath: String, wasCopyingOneFileOnly: Boolean)

    fun copyFailed()
}
