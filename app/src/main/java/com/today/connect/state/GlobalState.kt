package com.today.connect.state

import android.content.Context
import android.content.SharedPreferences

object GlobalState {
    private lateinit var preferences: SharedPreferences
    private const val PREFERENCES_NAME = "GlobalStatePreferences"

    fun init(context: Context) {
        if (!GlobalState::preferences.isInitialized) {
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }

    var username: String?
        get() = preferences.getString("username", null)
        set(value) = preferences.edit().putString("username", value).apply()

    var displayName: String?
        get() = preferences.getString("displayName", null)
        set(value) = preferences.edit().putString("displayName", value).apply()

    var preferredLang: String?
        get() = preferences.getString("preferredLang", null)
        set(value) = preferences.edit().putString("preferredLang", value).apply()

    var accessToken: String?
        get() = preferences.getString("accessToken", null)
        set(value) = preferences.edit().putString("accessToken", value).apply()

    fun getFileDownloadPath(url: String): String? {
        return preferences.getString(url, null)
    }

    fun setFileDownloadPath(url: String, path: String) {
        preferences.edit().putString(url, path).apply()
    }
}