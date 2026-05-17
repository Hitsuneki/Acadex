package com.example.classhub.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    const val PREFS_NAME = "classhub_prefs"
    const val KEY_THEME = "theme_mode"

    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"
    const val MODE_SYSTEM = "system"

    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(nightModeFromPrefs(context))
    }

    fun saveMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode)
            .apply()
        apply(context)
    }

    fun getMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    private fun nightModeFromPrefs(context: Context): Int {
        return when (getMode(context)) {
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
