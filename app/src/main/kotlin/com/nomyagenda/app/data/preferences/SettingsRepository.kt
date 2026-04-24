package com.nomyagenda.app.data.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var decorativeTheme: String
        get() = prefs.getString(KEY_DECORATIVE_THEME, DECORATIVE_THEME_DEFAULT) ?: DECORATIVE_THEME_DEFAULT
        set(value) = prefs.edit().putString(KEY_DECORATIVE_THEME, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    fun applyTheme() {
        // Thematic (non-default) themes use fixed light colours, so force light mode.
        // When the default theme is active, honour the stored day/night preference.
        val mode = when {
            decorativeTheme != DECORATIVE_THEME_DEFAULT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> when (themeMode) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applyLanguage() {
        val localeList = when (language) {
            LANGUAGE_ES -> LocaleListCompat.forLanguageTags("es")
            LANGUAGE_EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    companion object {
        const val PREFS_NAME = "nomy_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_DECORATIVE_THEME = "decorative_theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_NOTIFICATIONS = "notifications_enabled"

        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val THEME_SYSTEM = "SYSTEM"

        const val DECORATIVE_THEME_DEFAULT = "DEFAULT"
        const val DECORATIVE_THEME_OCEAN = "OCEAN"
        const val DECORATIVE_THEME_FOREST = "FOREST"
        const val DECORATIVE_THEME_SUNSET = "SUNSET"

        const val LANGUAGE_ES = "es"
        const val LANGUAGE_EN = "en"
        const val LANGUAGE_SYSTEM = "system"

        const val ADVANCE_NOTICE_NONE = 0
        const val ADVANCE_NOTICE_1H = 60
        const val ADVANCE_NOTICE_1D = 1440
        const val ADVANCE_NOTICE_1W = 10080
    }
}
