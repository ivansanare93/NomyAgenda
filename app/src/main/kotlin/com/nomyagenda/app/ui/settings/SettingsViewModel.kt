package com.nomyagenda.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.nomyagenda.app.data.preferences.SettingsRepository

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)

    val themeMode = MutableLiveData(settingsRepo.themeMode)
    val decorativeTheme = MutableLiveData(settingsRepo.decorativeTheme)
    val language = MutableLiveData(settingsRepo.language)
    val notificationsEnabled = MutableLiveData(settingsRepo.notificationsEnabled)
    val appBackground = MutableLiveData(settingsRepo.appBackground)

    /** true when the activity must recreate itself to apply a new decorative theme. */
    val recreateEvent = MutableLiveData(false)

    fun consumeRecreateEvent() {
        recreateEvent.value = false
    }

    fun setTheme(mode: String) {
        settingsRepo.themeMode = mode
        themeMode.value = mode
        settingsRepo.applyTheme()
    }

    fun setDecorativeTheme(theme: String) {
        if (settingsRepo.decorativeTheme == theme) return
        settingsRepo.decorativeTheme = theme
        decorativeTheme.value = theme
        settingsRepo.applyTheme()
        recreateEvent.value = true
    }

    fun setLanguage(lang: String) {
        settingsRepo.language = lang
        language.value = lang
        settingsRepo.applyLanguage()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        settingsRepo.notificationsEnabled = enabled
        notificationsEnabled.value = enabled
    }

    fun setAppBackground(bg: String) {
        if (settingsRepo.appBackground == bg) return
        settingsRepo.appBackground = bg
        appBackground.value = bg
        settingsRepo.applyTheme()
        recreateEvent.value = true
    }
}
