package com.nomyagenda.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.nomyagenda.app.data.preferences.SettingsRepository

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)

    val themeMode = MutableLiveData(settingsRepo.themeMode)
    val language = MutableLiveData(settingsRepo.language)
    val notificationsEnabled = MutableLiveData(settingsRepo.notificationsEnabled)

    fun setTheme(mode: String) {
        settingsRepo.themeMode = mode
        themeMode.value = mode
        settingsRepo.applyTheme()
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
}
