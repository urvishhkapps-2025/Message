package com.hkapps.messagepro.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hkapps.messagepro.extensions.mPref
import com.hkapps.messagepro.extensions.checkAppIconColor
import com.hkapps.messagepro.extensions.getSharedTheme
import com.hkapps.messagepro.utils.MyContentProviderUtils

class ThemeService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.mPref.apply {
            val oldColor = appIconColor
            if (intent.action == MyContentProviderUtils.SHARED_THEME_ACTIVATED) {
                if (!wasSharedThemeForced) {
                    wasSharedThemeForced = true
                    isUsingSharedTheme = true
                    wasSharedThemeEverActivated = true

                    context.getSharedTheme {
                        if (it != null) {
                            textColor = it.textColor
                            backgroundColor = it.backgroundColor
                            primaryColor = it.primaryColor
                            accentColor = it.accentColor
                            appIconColor = it.appIconColor
                            navigationBarColor = it.navigationBarColor
                            checkAppIconColorChanged(oldColor, appIconColor, context)
                        }
                    }
                }
            } else if (intent.action == MyContentProviderUtils.SHARED_THEME_UPDATED) {
                if (isUsingSharedTheme) {
                    context.getSharedTheme {
                        if (it != null) {
                            textColor = it.textColor
                            backgroundColor = it.backgroundColor
                            primaryColor = it.primaryColor
                            accentColor = it.accentColor
                            appIconColor = it.appIconColor
                            navigationBarColor = it.navigationBarColor
                            checkAppIconColorChanged(oldColor, appIconColor, context)
                        }
                    }
                }
            }
        }
    }

    private fun checkAppIconColorChanged(oldColor: Int, newColor: Int, context: Context) {
        if (oldColor != newColor) {
            context.checkAppIconColor()
        }
    }
}
