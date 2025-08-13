package com.hkapps.messagepro.model

data class SendedSmsModel(val messageID: Long, val delivered: Boolean) : ItemModel()
