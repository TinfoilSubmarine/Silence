package me.lucky.silence

import android.content.Context

import androidx.preference.PreferenceManager

class Preferences(context: Context) {
    companion object {
        private const val SERVICE_ENABLED = "service_enabled"
        private const val CALLBACK_CHECKED = "callback_checked"
        private const val TOLL_FREE_CHECKED = "toll_free_checked"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var isServiceEnabled: Boolean
        get() = getBoolean(SERVICE_ENABLED)
        set(value) = setBoolean(SERVICE_ENABLED, value)

    var isCallbackChecked: Boolean
        get() = getBoolean(CALLBACK_CHECKED)
        set(value) = setBoolean(CALLBACK_CHECKED, value)

    var isTollFreeChecked: Boolean
        get() = getBoolean(TOLL_FREE_CHECKED)
        set(value) = setBoolean(TOLL_FREE_CHECKED, value)

    private fun getBoolean(key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

    private fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
