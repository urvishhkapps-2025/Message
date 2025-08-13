package com.hkapps.messagepro.extensions

import android.graphics.Color
import android.widget.RemoteViews



fun RemoteViews.setTextSize(id: Int, size: Float) {
    setFloat(id, "setTextSize", size)
}

fun RemoteViews.setText(id: Int, text: String) {
    setTextViewText(id, text)
}


fun RemoteViews.applyColorFilter(id: Int, color: Int) {
    setInt(id, "setColorFilter", color)
    setInt(id, "setImageAlpha", Color.alpha(color))
}
