package com.nomyagenda.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.nomyagenda.app.data.preferences.SettingsRepository

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SettingsRepository(app)

    val themeMode = MutableLiveData(repository.themeMode)
    val language = MutableLiveData(repository.language)
    val notificationsEnabled = MutableLiveData(repository.notificationsEnabled)

    fun setTheme(mode: String) {
        repository.themeMode = mode
        themeMode.value = mode
        repository.applyTheme()
    }

    fun setLanguage(lang: String) {
        repository.language = lang
        language.value = lang
        repository.applyLanguage()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.notificationsEnabled = enabled
        notificationsEnabled.value = enabled
    }
}
