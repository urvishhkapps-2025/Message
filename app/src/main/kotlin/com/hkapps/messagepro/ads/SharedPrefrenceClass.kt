package com.hkapps.messagepro.ads

import android.content.*
import com.hkapps.messagepro.MainAppClass

class SharedPrefrenceClass private constructor() {
    private val settings: SharedPreferences =
        MainAppClass.instance!!.getSharedPreferences(AdsHelperClass.MY_PREFERANCE, 0)
    private val editor: SharedPreferences.Editor = settings.edit()
    fun getString(key: String?, defValue: String?): String? {
        return settings.getString(key, defValue)
    }

    fun setString(key: String?, value: String?): SharedPrefrenceClass {
        editor.putString(key, value)
        editor.commit()
        return this
    }

    fun setStatus(key: String?, value: Boolean): SharedPrefrenceClass {
        editor.putBoolean(key, value)
        editor.commit()
        return this
    }

    fun getInt(key: String?, defValue: Int): Int {
        return settings.getInt(key, defValue)
    }

    fun setInt(key: String?, value: Int): SharedPrefrenceClass {
        editor.putInt(key, value)
        editor.commit()
        return this
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return settings.getBoolean(key, defValue)
    }

    fun setBoolean(key: String?, value: Boolean): SharedPrefrenceClass {
        editor.putBoolean(key, value)
        editor.commit()
        return this
    }

    fun setLong(key: String?, value: Long): SharedPrefrenceClass {
        editor.putLong(key, value)
        editor.commit()
        return this
    }

    fun getLong(key: String?, defValue: Long): Long {
        return settings.getLong(key, defValue)
    }

    fun clearData() {
        editor.clear()
        editor.commit()
    }

    companion object {
        private var instance: SharedPrefrenceClass? = null
        fun getInstance(): SharedPrefrenceClass? {
            if (instance == null) instance = SharedPrefrenceClass()
            return instance
        }

        fun getInstance(context: Context?): SharedPrefrenceClass? {
            if (instance == null) instance = SharedPrefrenceClass()
            return instance
        }
    }

}
