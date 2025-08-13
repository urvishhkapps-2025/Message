package com.hkapps.messagepro.model

import android.net.Uri

data class SelectedAttachmentModel(
    val uri: Uri,
    val isPending: Boolean,
)
